package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Client
import com.example.data.model.Order
import com.example.data.model.Visit
import com.example.data.repository.ClientRepository
import com.example.data.repository.ClientSeeder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClientRepository
    
    // Core database flows
    val clients: StateFlow<List<Client>>
    val visits: StateFlow<List<Visit>>
    val orders: StateFlow<List<Order>>
    
    // Google Authentication & Cloud Sync State
    private val sharedPrefs = application.getSharedPreferences("visita_facil_auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(sharedPrefs.getBoolean("is_logged_in", false))
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow(sharedPrefs.getString("user_email", "") ?: "")
    val userEmail = _userEmail.asStateFlow()

    private val _userName = MutableStateFlow(sharedPrefs.getString("user_name", "") ?: "")
    val userName = _userName.asStateFlow()

    private val _userToken = MutableStateFlow(sharedPrefs.getString("user_token", "") ?: "")
    val userToken = _userToken.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(sharedPrefs.getLong("last_sync_time", 0L))
    val lastSyncTime = _lastSyncTime.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    fun loginWithGmail(email: String, name: String, token: String = "") {
        viewModelScope.launch {
            val sanitizedToken = if (token.isEmpty()) "demo_${System.currentTimeMillis()}" else token
            sharedPrefs.edit().apply {
                putBoolean("is_logged_in", true)
                putString("user_email", email)
                putString("user_name", name)
                putString("user_token", sanitizedToken)
                apply()
            }
            _isLoggedIn.value = true
            _userEmail.value = email
            _userName.value = name
            _userToken.value = sanitizedToken

            // If we have data on the cloud, restore them. Otherwise backup our existing database.
            com.example.data.sync.GoogleSyncManager.restoreFromDrive(
                context = getApplication(),
                token = sanitizedToken,
                email = email
            ) { success, msg, jsonContent ->
                if (success && jsonContent != null) {
                    restoreDatabase(jsonContent)
                } else {
                    // Back up initial database to Drive
                    backupToGoogleDrive()
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sharedPrefs.edit().apply {
                putBoolean("is_logged_in", false)
                putString("user_email", "")
                putString("user_name", "")
                putString("user_token", "")
                putLong("last_sync_time", 0L)
                apply()
            }
            _isLoggedIn.value = false
            _userEmail.value = ""
            _userName.value = ""
            _userToken.value = ""
            _lastSyncTime.value = 0L

            // Clear database so it's clean for the next user session, and seed on reload
            repository.deleteAllClients()
            repository.deleteAllVisits()
            repository.deleteAllOrders()
            seedDefaultClients()
        }
    }

    fun backupToGoogleDrive(onComplete: ((Boolean, String) -> Unit)? = null) {
        val email = _userEmail.value
        val token = _userToken.value
        if (email.isEmpty()) return

        _isSyncing.value = true
        viewModelScope.launch {
            val allClients = clients.value
            val allVisits = visits.value
            val allOrders = orders.value

            com.example.data.sync.GoogleSyncManager.backupToDrive(
                context = getApplication(),
                token = token,
                clients = allClients,
                visits = allVisits,
                orders = allOrders,
                email = email
            ) { success, msg ->
                _isSyncing.value = false
                if (success) {
                    val now = System.currentTimeMillis()
                    _lastSyncTime.value = now
                    sharedPrefs.edit().putLong("last_sync_time", now).apply()
                }
                onComplete?.invoke(success, msg)
            }
        }
    }

    fun restoreFromGoogleDrive(onComplete: ((Boolean, String) -> Unit)? = null) {
        val email = _userEmail.value
        val token = _userToken.value
        if (email.isEmpty()) return

        _isSyncing.value = true
        viewModelScope.launch {
            com.example.data.sync.GoogleSyncManager.restoreFromDrive(
                context = getApplication(),
                token = token,
                email = email
            ) { success, msg, jsonContent ->
                if (success && jsonContent != null) {
                    restoreDatabase(jsonContent)
                    onComplete?.invoke(true, msg)
                } else {
                    _isSyncing.value = false
                    onComplete?.invoke(false, msg)
                }
            }
        }
    }

    private fun restoreDatabase(jsonContent: String) {
        viewModelScope.launch {
            try {
                val restoredClients = com.example.data.sync.DatabaseJsonSerializer.deserializeClients(jsonContent)
                val restoredVisits = com.example.data.sync.DatabaseJsonSerializer.deserializeVisits(jsonContent)
                val restoredOrders = com.example.data.sync.DatabaseJsonSerializer.deserializeOrders(jsonContent)

                if (restoredClients.isNotEmpty()) {
                    repository.deleteAllClients()
                    repository.insertClients(restoredClients)
                }
                
                repository.deleteAllVisits()
                for (v in restoredVisits) {
                    repository.insertVisit(v)
                }

                repository.deleteAllOrders()
                for (o in restoredOrders) {
                    repository.insertOrder(o)
                }

                val now = System.currentTimeMillis()
                _lastSyncTime.value = now
                sharedPrefs.edit().putLong("last_sync_time", now).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun triggerAutoSync() {
        if (_isLoggedIn.value) {
            backupToGoogleDrive()
        }
    }

    // Filtering states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedBairro = MutableStateFlow<String?>(null)
    val selectedBairro = _selectedBairro.asStateFlow()

    private val _selectedSegment = MutableStateFlow<String?>(null)
    val selectedSegment = _selectedSegment.asStateFlow()

    private val _selectedPeriod = MutableStateFlow<String?>(null)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus = _selectedStatus.asStateFlow()

    // Alert Notification states (simulating live notifications)
    private val _notifications = MutableStateFlow<List<NotificationAlert>>(emptyList())
    val notifications = _notifications.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ClientRepository(
            clientDao = database.clientDao(),
            visitDao = database.visitDao(),
            orderDao = database.orderDao()
        )

        clients = repository.allClients.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        visits = repository.allVisits.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        orders = repository.allOrders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Preload default customers if DB is empty
        viewModelScope.launch {
            clients.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultClients()
                }
            }
        }

        // Add some initial simulated push alerts to demonstrate alerts functionality
        setupInitialNotifications()
    }

    private suspend fun seedDefaultClients() {
        try {
            val parsed = ClientSeeder.parseClients()
            repository.insertClients(parsed)
            // Insert a few default visits to make dashboard interesting on first load
            if (parsed.isNotEmpty()) {
                val today = System.currentTimeMillis()
                val oneDay = 24 * 60 * 60 * 1000L
                repository.insertVisit(
                    Visit(
                        clientId = parsed[0].id,
                        clientName = parsed[0].name,
                        address = parsed[0].address,
                        neighborhood = parsed[0].neighborhood,
                        date = today,
                        period = "Manhã",
                        status = "Realizada",
                        notes = "Atendimento excelente. Pedido realizado.",
                        kmsDriven = 5.4
                    )
                )
                repository.insertVisit(
                    Visit(
                        clientId = parsed[1].id,
                        clientName = parsed[1].name,
                        address = parsed[1].address,
                        neighborhood = parsed[1].neighborhood,
                        date = today,
                        period = "Tarde",
                        status = "A Realizar",
                        notes = "Apresentar novos produtos do mix.",
                        kmsDriven = 0.0
                    )
                )
                repository.insertVisit(
                    Visit(
                        clientId = parsed[2].id,
                        clientName = parsed[2].name,
                        address = parsed[2].address,
                        neighborhood = parsed[2].neighborhood,
                        date = today + oneDay,
                        period = "Manhã",
                        status = "A Realizar",
                        notes = "Confirmar horário com contato.",
                        kmsDriven = 0.0
                    )
                )

                // Add sample order
                repository.insertOrder(
                    Order(
                        visitId = 1,
                        clientId = parsed[0].id,
                        clientName = parsed[0].name,
                        date = today,
                        paymentTerm = parsed[0].paymentTerm,
                        notes = "10un Caixa de Bombom, 15un Caixa Biscoito Recheado",
                        totalValue = 350.00,
                        status = "Realizado"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupInitialNotifications() {
        _notifications.value = listOf(
            NotificationAlert(
                id = 1,
                title = "Nova rota disponível",
                message = "Seu roteiro para o bairro Laranjeiras foi otimizado e já está disponível para visualização.",
                time = "Há 5 min",
                type = "route"
            ),
            NotificationAlert(
                id = 2,
                title = "Alteração de última hora",
                message = "Cliente 'Adgmar Alves Da Silva' alterou a preferência de atendimento para o período da Tarde.",
                time = "Há 12 min",
                type = "change"
            )
        )
    }

    fun dismissNotification(id: Int) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    fun triggerSimulatedNotification() {
        val alerts = listOf(
            NotificationAlert(
                id = System.currentTimeMillis().toInt(),
                title = "Pedido Faturado!",
                message = "O pedido do cliente 'Mercadinho e Açougue Carajas Ltda' foi faturado e enviado para entrega.",
                time = "Agora",
                type = "billing"
            ),
            NotificationAlert(
                id = System.currentTimeMillis().toInt(),
                title = "Visita Cancelada",
                message = "A visita para 'Classic Supermercado' foi cancelada devido a recesso.",
                time = "Agora",
                type = "change"
            ),
            NotificationAlert(
                id = System.currentTimeMillis().toInt(),
                title = "Aviso de Trânsito",
                message = "Tráfego intenso detectado na Av. João Naves de Ávila. Rota otimizada recalculada.",
                time = "Agora",
                type = "route"
            )
        )
        val newAlert = alerts.random()
        _notifications.value = listOf(newAlert) + _notifications.value
    }

    // Client CRUD Operations
    fun addClient(name: String, address: String, neighborhood: String, contact: String, phone: String, paymentTerm: String, segment: String) {
        viewModelScope.launch {
            // Generate coordinates centered on Uberlândia
            val hash = name.hashCode().toDouble()
            val latOffset = (hash % 100) / 1500.0
            val lngOffset = ((hash / 100).toInt() % 100) / 1500.0
            val lat = -18.9186 + latOffset
            val lng = -48.2772 + lngOffset

            repository.insertClient(
                Client(
                    name = name,
                    address = address,
                    neighborhood = neighborhood,
                    contact = contact,
                    phone = phone,
                    paymentTerm = paymentTerm,
                    segment = segment,
                    latitude = lat,
                    longitude = lng
                )
            )
            triggerAutoSync()
        }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch {
            repository.updateClient(client)
            triggerAutoSync()
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
            triggerAutoSync()
        }
    }

    // Visit Management Operations
    fun scheduleVisit(clientId: Int, clientName: String, address: String, neighborhood: String, date: Long, period: String, notes: String = "") {
        viewModelScope.launch {
            repository.insertVisit(
                Visit(
                    clientId = clientId,
                    clientName = clientName,
                    address = address,
                    neighborhood = neighborhood,
                    date = date,
                    period = period,
                    status = "A Realizar",
                    notes = notes
                )
            )
            triggerAutoSync()
        }
    }

    fun updateVisitStatus(visit: Visit, newStatus: String, kms: Double, notes: String) {
        viewModelScope.launch {
            repository.updateVisit(
                visit.copy(
                    status = newStatus,
                    kmsDriven = if (newStatus == "Realizada") kms else 0.0,
                    notes = notes
                )
            )
            triggerAutoSync()
        }
    }

    fun deleteVisit(visit: Visit) {
        viewModelScope.launch {
            repository.deleteVisit(visit)
            triggerAutoSync()
        }
    }

    // Order Operations
    fun saveOrder(id: Int = 0, visitId: Int?, clientId: Int, clientName: String, notes: String, value: Double, status: String, paymentTerm: String) {
        viewModelScope.launch {
            val order = Order(
                id = id,
                visitId = visitId,
                clientId = clientId,
                clientName = clientName,
                date = System.currentTimeMillis(),
                notes = notes,
                totalValue = value,
                status = status,
                paymentTerm = paymentTerm
            )
            if (id == 0) {
                repository.insertOrder(order)
            } else {
                repository.updateOrder(order)
            }
            triggerAutoSync()
        }
    }

    fun updateOrderStatus(order: Order, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrder(order.copy(status = newStatus))
            triggerAutoSync()
        }
    }

    fun deleteOrder(order: Order) {
        viewModelScope.launch {
            repository.deleteOrder(order)
            triggerAutoSync()
        }
    }

    // Search and Filters setters
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setBairroFilter(bairro: String?) {
        _selectedBairro.value = bairro
    }

    fun setSegmentFilter(segment: String?) {
        _selectedSegment.value = segment
    }

    fun setPeriodFilter(period: String?) {
        _selectedPeriod.value = period
    }

    fun setStatusFilter(status: String?) {
        _selectedStatus.value = status
    }

    // Combined filtered flows for screen display
    val filteredClients = combine(clients, searchQuery, selectedBairro, selectedSegment) { list, query, bairro, segment ->
        list.filter { client ->
            val matchesQuery = query.isEmpty() || client.name.contains(query, ignoreCase = true) || client.address.contains(query, ignoreCase = true)
            val matchesBairro = bairro == null || client.neighborhood.equals(bairro, ignoreCase = true)
            val matchesSegment = segment == null || client.segment.equals(segment, ignoreCase = true)
            matchesQuery && matchesBairro && matchesSegment
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredVisits = combine(visits, searchQuery, selectedBairro, selectedPeriod, selectedStatus) { list, query, bairro, period, status ->
        list.filter { visit ->
            val matchesQuery = query.isEmpty() || visit.clientName.contains(query, ignoreCase = true) || visit.notes.contains(query, ignoreCase = true)
            val matchesBairro = bairro == null || visit.neighborhood.equals(bairro, ignoreCase = true)
            val matchesPeriod = period == null || visit.period.equals(period, ignoreCase = true)
            val matchesStatus = status == null || visit.status.equals(status, ignoreCase = true)
            matchesQuery && matchesBairro && matchesPeriod && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredOrders = combine(orders, searchQuery, selectedStatus) { list, query, status ->
        list.filter { order ->
            val matchesQuery = query.isEmpty() || order.clientName.contains(query, ignoreCase = true) || order.notes.contains(query, ignoreCase = true)
            val matchesStatus = status == null || order.status.equals(status, ignoreCase = true)
            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unique neighborhoods and segments lists for filter selectors
    val uniqueNeighborhoods = clients.combine(emptyFlow = false) { list ->
        list.map { it.neighborhood.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    val uniqueSegments = clients.combine(emptyFlow = false) { list ->
        list.map { it.segment.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    // Custom extension to combine state flows easily
    private fun <T> StateFlow<T>.combine(emptyFlow: Boolean, transform: (T) -> List<String>): StateFlow<List<String>> {
        val flow = MutableStateFlow<List<String>>(emptyList())
        viewModelScope.launch {
            this@combine.collect {
                flow.value = transform(it)
            }
        }
        return flow
    }

    // ROUTE OPTIMIZATION: Nearest Neighbor TSP Solver starting from central Uberlândia location
    fun getOptimizedVisits(visitsToOptimize: List<Visit>): OptimizedRouteResult {
        if (visitsToOptimize.isEmpty()) return OptimizedRouteResult(emptyList(), 0.0)

        // Resolve coordinates for each visit client
        val clientMap = clients.value.associateBy { it.id }
        
        val startLat = -18.9186
        val startLng = -48.2772

        val unvisited = visitsToOptimize.toMutableList()
        val ordered = mutableListOf<Visit>()
        var currentLat = startLat
        var currentLng = startLng
        var totalDistance = 0.0

        while (unvisited.isNotEmpty()) {
            var nearestVisit: Visit? = null
            var minDistance = Double.MAX_VALUE
            var nearestLat = currentLat
            var nearestLng = currentLng

            for (visit in unvisited) {
                val client = clientMap[visit.clientId]
                val clientLat = client?.latitude ?: -18.9186
                val clientLng = client?.longitude ?: -48.2772
                
                val dist = calculateDistance(currentLat, currentLng, clientLat, clientLng)
                if (dist < minDistance) {
                    minDistance = dist
                    nearestVisit = visit
                    nearestLat = clientLat
                    nearestLng = clientLng
                }
            }

            if (nearestVisit != null) {
                ordered.add(nearestVisit)
                unvisited.remove(nearestVisit)
                totalDistance += minDistance
                currentLat = nearestLat
                currentLng = nearestLng
            } else {
                break
            }
        }

        return OptimizedRouteResult(ordered, totalDistance)
    }

    // Distance calculation in km using Haversine formula
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // EXPORT EXCEL (CSV formatting)
    fun exportToExcel(context: Context) {
        viewModelScope.launch {
            try {
                val clientList = clients.value
                val visitList = visits.value
                val orderList = orders.value

                val csvContent = StringBuilder()
                
                // Section 1: Dashboard KPI indicators
                csvContent.append("RELATÓRIO CONSOLIDADO - VISITAFÁCIL\n")
                csvContent.append("Data de Geração:;${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")
                
                csvContent.append("INDICADORES DE PRODUTIVIDADE\n")
                csvContent.append("Total de Clientes:;${clientList.size}\n")
                csvContent.append("Visitas Agendadas:;${visitList.size}\n")
                csvContent.append("Visitas Realizadas:;${visitList.count { it.status == "Realizada" }}\n")
                csvContent.append("Total de Kms Rodados:;${String.format(Locale.US, "%.2f", visitList.sumOf { it.kmsDriven })} km\n")
                csvContent.append("Total Faturado/Vendido:;R$ ${String.format(Locale.US, "%.2f", orderList.sumOf { it.totalValue })}\n\n")

                // Section 2: Clients
                csvContent.append("CLIENTES\n")
                csvContent.append("Código;Sequência;Nome;Endereço;Bairro;Contato;Telefone;Segmento;Forma Pagamento\n")
                for (c in clientList) {
                    csvContent.append("${c.id};${c.seq};${c.name};${c.address};${c.neighborhood};${c.contact};${c.phone};${c.segment};${c.paymentTerm}\n")
                }
                csvContent.append("\n")

                // Section 3: Visits
                csvContent.append("HISTÓRICO DE VISITAS\n")
                csvContent.append("ID;Cliente;Bairro;Data;Período;Status;Kms Rodados;Anotações\n")
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                for (v in visitList) {
                    csvContent.append("${v.id};${v.clientName};${v.neighborhood};${sdf.format(Date(v.date))};${v.period};${v.status};${v.kmsDriven};${v.notes.replace("\n", " ")}\n")
                }
                csvContent.append("\n")

                // Section 4: Orders
                csvContent.append("CADERNO DE PEDIDOS\n")
                csvContent.append("ID;Cliente;Data;Valor Total;Forma Pgto;Status;Anotações/Produtos\n")
                for (o in orderList) {
                    csvContent.append("${o.id};${o.clientName};${sdf.format(Date(o.date))};R$ ${String.format(Locale.US, "%.2f", o.totalValue)};${o.paymentTerm};${o.status};${o.notes.replace("\n", " ")}\n")
                }

                // Write file to external storage or cache for sharing
                val directory = File(context.cacheDir, "exports")
                if (!directory.exists()) directory.mkdirs()
                val file = File(directory, "Relatorio_Visitas_VisitaFacil.csv")
                
                FileOutputStream(file).use { out ->
                    out.write(csvContent.toString().toByteArray(charset("ISO-8859-1"))) // standard Excel encoding in Brazil
                }

                shareFile(context, file, "text/csv", "Compartilhar Planilha Excel")
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao exportar planilha: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // EXPORT PDF (using native Android PdfDocument paint canvas)
    fun exportToPdf(context: Context) {
        viewModelScope.launch {
            try {
                val clientList = clients.value
                val visitList = visits.value
                val orderList = orders.value

                val pdfDocument = PdfDocument()
                val paint = Paint()
                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 18f
                    isFakeBoldText = true
                }
                val subtitlePaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 12f
                    isFakeBoldText = true
                }
                val textPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                }
                val borderPaint = Paint().apply {
                    color = Color.LTGRAY
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }

                // PAGE 1: Dashboard and Summary of KPI
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size (595x842 pt)
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawText("VISITAFÁCIL - RELATÓRIO EXECUTIVO DE ATENDIMENTO", 40f, 50f, titlePaint)
                canvas.drawText("Gerado em: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}", 40f, 75f, textPaint)

                // Divider line
                canvas.drawLine(40f, 85f, 555f, 85f, borderPaint)

                canvas.drawText("INDICADORES DE PRODUTIVIDADE", 40f, 110f, subtitlePaint)
                
                var y = 140f
                val metrics = listOf(
                    "Total de Clientes Cadastrados: ${clientList.size}",
                    "Total de Visitas Agendadas: ${visitList.size}",
                    "Visitas Realizadas: ${visitList.count { it.status == "Realizada" }}",
                    "Visitas Pendentes (A realizar): ${visitList.count { it.status == "A Realizar" }}",
                    "Visitas Canceladas: ${visitList.count { it.status == "Cancelada" }}",
                    "Total Quilômetros Rodados: ${String.format(Locale.getDefault(), "%.2f", visitList.sumOf { it.kmsDriven })} km",
                    "Total Valor de Pedidos: R$ ${String.format(Locale.getDefault(), "%.2f", orderList.sumOf { it.totalValue })}"
                )
                
                for (metric in metrics) {
                    canvas.drawText(metric, 50f, y, textPaint)
                    y += 20f
                }

                // Orders Status summary
                y += 15f
                canvas.drawText("RESUMO FINANCEIRO POR STATUS DE PEDIDO", 40f, y, subtitlePaint)
                y += 25f

                val orderStatuses = listOf("Realizado", "A Faturar", "Faturado", "Entregue")
                for (status in orderStatuses) {
                    val count = orderList.count { it.status == status }
                    val total = orderList.filter { it.status == status }.sumOf { it.totalValue }
                    canvas.drawText("Status '$status': $count pedido(s) - Total R$ ${String.format(Locale.getDefault(), "%.2f", total)}", 50f, y, textPaint)
                    y += 20f
                }

                // Recent Visits
                y += 20f
                canvas.drawText("ÚLTIMAS VISITAS AGENDADAS (Até 10)", 40f, y, subtitlePaint)
                y += 25f

                val recentVisits = visitList.take(10)
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                canvas.drawText("Cliente", 50f, y, subtitlePaint)
                canvas.drawText("Bairro", 220f, y, subtitlePaint)
                canvas.drawText("Data", 380f, y, subtitlePaint)
                canvas.drawText("Status", 460f, y, subtitlePaint)
                
                y += 5f
                canvas.drawLine(40f, y, 555f, y, borderPaint)
                y += 18f

                for (v in recentVisits) {
                    val clientNameAbbr = if (v.clientName.length > 25) v.clientName.substring(0, 22) + "..." else v.clientName
                    canvas.drawText(clientNameAbbr, 50f, y, textPaint)
                    canvas.drawText(v.neighborhood, 220f, y, textPaint)
                    canvas.drawText(sdf.format(Date(v.date)), 380f, y, textPaint)
                    canvas.drawText(v.status, 460f, y, textPaint)
                    y += 18f
                }

                // Footer
                canvas.drawText("Fim do Relatório Executivo - VisitaFácil", 200f, 800f, textPaint)

                pdfDocument.finishPage(page)

                // Save to cache and share
                val directory = File(context.cacheDir, "exports")
                if (!directory.exists()) directory.mkdirs()
                val file = File(directory, "Relatorio_Mensal_VisitaFacil.pdf")
                
                FileOutputStream(file).use { out ->
                    pdfDocument.writeTo(out)
                }
                pdfDocument.close()

                shareFile(context, file, "application/pdf", "Compartilhar Relatório PDF")
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao exportar PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        try {
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório de Atividades - VisitaFácil")
                putExtra(Intent.EXTRA_TEXT, "Prezado,\n\nSegue em anexo o relatório gerado pelo aplicativo VisitaFácil.\n\nAtenciosamente,\nConsultor de Vendas.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao compartilhar arquivo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}

// Notification structure for simulated alerts
data class NotificationAlert(
    val id: Int,
    val title: String,
    val message: String,
    val time: String,
    val type: String // "route", "change", "billing"
)

// Wrapper for Optimized TSP calculations
data class OptimizedRouteResult(
    val route: List<Visit>,
    val totalEstimatedDistance: Double
)
