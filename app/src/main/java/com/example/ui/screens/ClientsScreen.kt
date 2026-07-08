package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val clients by viewModel.filteredClients.collectAsState()
    val neighborhoods by viewModel.uniqueNeighborhoods.collectAsState()
    val segments by viewModel.uniqueSegments.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBairro by viewModel.selectedBairro.collectAsState()
    val selectedSegment by viewModel.selectedSegment.collectAsState()

    // Dialog triggering states
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Client?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Client?>(null) }
    var showScheduleVisitDialog by remember { mutableStateOf<Client?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_client_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Cliente")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar Component
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Buscar cliente por nome ou endereço...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("client_search_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Interactive Filters Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Neighborhood Filter Chip Exposed Dropdown
                var bairroExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = bairroExpanded,
                        onExpandedChange = { bairroExpanded = !bairroExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBairro ?: "Filtrar Bairro",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bairroExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("bairro_filter_selector"),
                            textStyle = TextStyle(fontSize = 12.sp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = bairroExpanded,
                            onDismissRequest = { bairroExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todos os Bairros", fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setBairroFilter(null)
                                    bairroExpanded = false
                                }
                            )
                            neighborhoods.forEach { neighborhood ->
                                DropdownMenuItem(
                                    text = { Text(neighborhood, fontSize = 12.sp) },
                                    onClick = {
                                        viewModel.setBairroFilter(neighborhood)
                                        bairroExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Segment Filter Chip Exposed Dropdown
                var segmentExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = segmentExpanded,
                        onExpandedChange = { segmentExpanded = !segmentExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSegment ?: "Filtrar Segmento",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = segmentExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("segment_filter_selector"),
                            textStyle = TextStyle(fontSize = 12.sp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = segmentExpanded,
                            onDismissRequest = { segmentExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todos os Segmentos", fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setSegmentFilter(null)
                                    segmentExpanded = false
                                }
                            )
                            segments.forEach { segment ->
                                DropdownMenuItem(
                                    text = { Text(segment, fontSize = 12.sp) },
                                    onClick = {
                                        viewModel.setSegmentFilter(segment)
                                        segmentExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quick Clear active filters indicator
            if (selectedBairro != null || selectedSegment != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    SuggestionChip(
                        onClick = {
                            viewModel.setBairroFilter(null)
                            viewModel.setSegmentFilter(null)
                        },
                        label = { Text("Limpar Filtros", fontSize = 11.sp) },
                        icon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                }
            }

            // Main Clients list
            if (clients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = "No customers found",
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhum cliente correspondente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Ajuste os termos de busca ou filtros de bairro/segmento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("clients_lazy_list")
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(clients, key = { it.id }) { client ->
                        ClientItemRow(
                            client = client,
                            onScheduleVisit = { showScheduleVisitDialog = client },
                            onEdit = { showEditDialog = client },
                            onDelete = { showDeleteConfirmDialog = client },
                            onDialPhone = { dialNumber(context, client.phone) }
                        )
                    }
                }
            }
        }
    }

    // DIALOG: ADD CLIENT
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var neighborhood by remember { mutableStateOf("") }
        var contact by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var paymentTerm by remember { mutableStateOf("") }
        var segment by remember { mutableStateOf("Geral") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Cadastrar Novo Cliente") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome da Empresa/Cliente") }, modifier = Modifier.fillMaxWidth().testTag("add_client_name"))
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Endereço") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = neighborhood, onValueChange = { neighborhood = it }, label = { Text("Bairro") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contato") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefone") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = paymentTerm, onValueChange = { paymentTerm = it }, label = { Text("Condições PGTO (Ex: Boleto 10)") }, modifier = Modifier.fillMaxWidth())
                    
                    // Segment manual override selector
                    var segmentExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = segmentExpanded,
                        onExpandedChange = { segmentExpanded = !segmentExpanded }
                    ) {
                        OutlinedTextField(
                            value = segment,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Segmento") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = segmentExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = segmentExpanded,
                            onDismissRequest = { segmentExpanded = false }
                        ) {
                            val list = listOf("Geral", "Supermercado / Mercearia", "Açougue", "Padaria / Panificadora", "Empório / Boutique", "Alimentação / Lanchonete")
                            list.forEach { seg ->
                                DropdownMenuItem(
                                    text = { Text(seg) },
                                    onClick = {
                                        segment = seg
                                        segmentExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Por favor insira um nome", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addClient(name, address, neighborhood, contact, phone, paymentTerm, segment)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_add_client_btn")
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // DIALOG: EDIT CLIENT
    showEditDialog?.let { client ->
        var name by remember { mutableStateOf(client.name) }
        var address by remember { mutableStateOf(client.address) }
        var neighborhood by remember { mutableStateOf(client.neighborhood) }
        var contact by remember { mutableStateOf(client.contact) }
        var phone by remember { mutableStateOf(client.phone) }
        var paymentTerm by remember { mutableStateOf(client.paymentTerm) }
        var segment by remember { mutableStateOf(client.segment) }

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Editar Informações do Cliente") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome da Empresa/Cliente") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Endereço") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = neighborhood, onValueChange = { neighborhood = it }, label = { Text("Bairro") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contato") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefone") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = paymentTerm, onValueChange = { paymentTerm = it }, label = { Text("Condições PGTO") }, modifier = Modifier.fillMaxWidth())
                    
                    var segmentExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = segmentExpanded,
                        onExpandedChange = { segmentExpanded = !segmentExpanded }
                    ) {
                        OutlinedTextField(
                            value = segment,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Segmento") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = segmentExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = segmentExpanded,
                            onDismissRequest = { segmentExpanded = false }
                        ) {
                            val list = listOf("Geral", "Supermercado / Mercearia", "Açougue", "Padaria / Panificadora", "Empório / Boutique", "Alimentação / Lanchonete")
                            list.forEach { seg ->
                                DropdownMenuItem(
                                    text = { Text(seg) },
                                    onClick = {
                                        segment = seg
                                        segmentExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateClient(
                            client.copy(
                                name = name,
                                address = address,
                                neighborhood = neighborhood,
                                contact = contact,
                                phone = phone,
                                paymentTerm = paymentTerm,
                                segment = segment
                            )
                        )
                        showEditDialog = null
                    }
                ) {
                    Text("Atualizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) { Text("Cancelar") }
            }
        )
    }

    // DIALOG: DELETE CLIENT CONFIRM
    showDeleteConfirmDialog?.let { client ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Excluir Cliente?") },
            text = { Text("Tem certeza de que deseja remover permanentemente o cliente '${client.name}'? Isso também afetará relatórios futuros.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClient(client)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) { Text("Cancelar") }
            }
        )
    }

    // DIALOG: SCHEDULE VISIT FOR CUSTOMER
    showScheduleVisitDialog?.let { client ->
        var selectedPeriod by remember { mutableStateOf("Manhã") }
        var notes by remember { mutableStateOf("") }
        var daysOffset by remember { mutableStateOf(0) } // 0 = Hoje, 1 = Amanhã, 2 = Depois

        AlertDialog(
            onDismissRequest = { showScheduleVisitDialog = null },
            title = { Text("Agendar Visita") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Agendamento para: ${client.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Endereço: ${client.address}, ${client.neighborhood}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date selector representation
                    Text("Data da Visita", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Hoje", "Amanhã", "Depois de amanhã").forEachIndexed { index, label ->
                            val isSelected = daysOffset == index
                            InputChip(
                                selected = isSelected,
                                onClick = { daysOffset = index },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Period Selector
                    Text("Período preferencial", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Manhã", "Tarde", "Noite").forEach { period ->
                            val isSelected = selectedPeriod == period
                            InputChip(
                                selected = isSelected,
                                onClick = { selectedPeriod = period },
                                label = { Text(period, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Anotações de agendamento (Ex: Mix p/ expor)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val oneDayMs = 24 * 60 * 60 * 1000L
                        val scheduledDate = System.currentTimeMillis() + (daysOffset * oneDayMs)
                        viewModel.scheduleVisit(
                            clientId = client.id,
                            clientName = client.name,
                            address = client.address,
                            neighborhood = client.neighborhood,
                            date = scheduledDate,
                            period = selectedPeriod,
                            notes = notes
                        )
                        Toast.makeText(context, "Visita agendada com sucesso!", Toast.LENGTH_SHORT).show()
                        showScheduleVisitDialog = null
                    },
                    modifier = Modifier.testTag("submit_schedule_visit")
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agendar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleVisitDialog = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun ClientItemRow(
    client: Client,
    onScheduleVisit: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDialPhone: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("client_card_${client.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Segment Badge
                    Box(
                        modifier = Modifier
                            .background(getSegmentColor(client.segment).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = client.segment,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = getSegmentColor(client.segment)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (client.seq.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${client.seq}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${client.address} • ${client.neighborhood}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Contact shortcut details
            if (client.contact.isNotEmpty() || client.phone.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${client.contact} (${client.phone})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Expanded panel showing CRUD / scheduling triggers
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Action buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Schedule visit button
                            Button(
                                onClick = onScheduleVisit,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(32.dp).testTag("client_row_schedule_btn_${client.id}")
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Agendar Visita", fontSize = 11.sp)
                            }

                            // Dial Phone button
                            if (client.phone.isNotEmpty()) {
                                IconButton(
                                    onClick = onDialPhone,
                                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Call,
                                        contentDescription = "Ligar",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Edit / Delete buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Utility to return beautiful M3 theme-aligned colors for customer segments
fun getSegmentColor(segment: String): Color {
    return when (segment) {
        "Supermercado / Mercearia" -> Color(0xFF4CAF50) // Green
        "Açougue" -> Color(0xFFF44336) // Red
        "Padaria / Panificadora" -> Color(0xFFFF9800) // Amber/Orange
        "Empório / Boutique" -> Color(0xFF9C27B0) // Purple
        "Alimentação / Lanchonete" -> Color(0xFF00BCD4) // Cyan
        else -> Color(0xFF607D8B) // Slate grey
    }
}

// Dial phone helper
private fun dialNumber(context: Context, number: String) {
    try {
        val sanitized = number.replace("-", "").replace(" ", "").replace("(", "").replace(")", "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Não foi possível abrir o discador.", Toast.LENGTH_SHORT).show()
    }
}

// Style wrapper helper to provide text style in compose
private fun TextStyle(fontSize: androidx.compose.ui.unit.TextUnit) = androidx.compose.ui.text.TextStyle(fontSize = fontSize)
