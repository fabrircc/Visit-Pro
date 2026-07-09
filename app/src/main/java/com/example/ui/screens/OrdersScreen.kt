package com.example.ui.screens

import android.widget.Toast
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.data.model.Order
import com.example.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val orders by viewModel.filteredOrders.collectAsState()
    val clients by viewModel.clients.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()

    var showAddOrderDialog by remember { mutableStateOf(false) }
    var showEditOrderDialog by remember { mutableStateOf<Order?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Order?>(null) }

    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddOrderDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_order_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Pedido")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Stats Indicators
            OrdersPipelineHeader(orders = viewModel.orders.collectAsState().value, formatCurrency = formatCurrency)

            // Search input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Buscar pedido por cliente ou item...") },
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
                        .testTag("order_search_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Category filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    null to "Todos",
                    "Realizado" to "Realizados",
                    "A Faturar" to "A Faturar",
                    "Faturado" to "Faturados",
                    "Entregue" to "Entregues"
                ).forEach { (status, label) ->
                    val isSelected = selectedStatus == status
                    InputChip(
                        selected = isSelected,
                        onClick = { viewModel.setStatusFilter(status) },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.testTag("order_filter_chip_${status ?: "todos"}")
                    )
                }
            }

            // Main orders List
            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum pedido anotado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Ajuste os filtros ou anote um pedido ao concluir uma visita.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("orders_lazy_list")
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        val client = clients.find { it.id == order.clientId }
                        val clientPhone = client?.phone ?: ""
                        OrderItemRow(
                            order = order,
                            clientPhone = clientPhone,
                            formatCurrency = formatCurrency,
                            onStatusChange = { newStatus -> viewModel.updateOrderStatus(order, newStatus) },
                            onEdit = { showEditOrderDialog = order },
                            onDelete = { showDeleteConfirmDialog = order }
                        )
                    }
                }
            }
        }
    }

    // DIALOG: ADD INDEPENDENT ORDER
    if (showAddOrderDialog) {
        var selectedClient by remember { mutableStateOf<Client?>(null) }
        var notes by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }
        var paymentTerm by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Realizado") }

        AlertDialog(
            onDismissRequest = { showAddOrderDialog = false },
            title = { Text("Anote um Pedido Manual") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Client picker Exposed dropdown
                    var clientExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = clientExpanded,
                        onExpandedChange = { clientExpanded = !clientExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedClient?.name ?: "Selecione o Cliente...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cliente") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth().testTag("add_order_client_picker")
                        )
                        ExposedDropdownMenu(
                            expanded = clientExpanded,
                            onDismissRequest = { clientExpanded = false },
                            modifier = Modifier.height(200.dp)
                        ) {
                            clients.forEach { client ->
                                DropdownMenuItem(
                                    text = { Text(client.name, fontSize = 12.sp) },
                                    onClick = {
                                        selectedClient = client
                                        paymentTerm = client.paymentTerm // Autofill payment term
                                        clientExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Bloco de Notas / Produtos vendidos") },
                        placeholder = { Text("Ex: 50 fardos de farinha, 10 un fermento") },
                        modifier = Modifier.fillMaxWidth().testTag("add_order_notes")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text("Valor Total (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("add_order_value")
                        )
                        OutlinedTextField(
                            value = paymentTerm,
                            onValueChange = { paymentTerm = it },
                            label = { Text("Cond. PGTO") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Status Picker
                    var statusExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = !statusExpanded }
                    ) {
                        OutlinedTextField(
                            value = getDisplayStatus(status),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Etapa do Atendimento") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            listOf("Realizado", "A Faturar", "Faturado", "Entregue").forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(getDisplayStatus(item)) },
                                    onClick = {
                                        status = item
                                        statusExpanded = false
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
                        val client = selectedClient
                        if (client == null) {
                            Toast.makeText(context, "Selecione um cliente", Toast.LENGTH_SHORT).show()
                        } else if (notes.isBlank()) {
                            Toast.makeText(context, "Insira os produtos vendidos", Toast.LENGTH_SHORT).show()
                        } else {
                            val valDouble = value.toDoubleOrNull() ?: 0.0
                            viewModel.saveOrder(
                                visitId = null,
                                clientId = client.id,
                                clientName = client.name,
                                notes = notes,
                                value = valDouble,
                                status = status,
                                paymentTerm = paymentTerm
                            )
                            Toast.makeText(context, "Pedido cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                            showAddOrderDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_add_order")
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddOrderDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // DIALOG: EDIT EXISTING ORDER
    showEditOrderDialog?.let { order ->
        var notes by remember { mutableStateOf(order.notes) }
        var value by remember { mutableStateOf(order.totalValue.toString()) }
        var paymentTerm by remember { mutableStateOf(order.paymentTerm) }
        var status by remember { mutableStateOf(order.status) }

        AlertDialog(
            onDismissRequest = { showEditOrderDialog = null },
            title = { Text("Editar Informações do Pedido") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cliente: ${order.clientName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Bloco de Notas / Produtos vendidos") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_order_notes")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text("Valor Total (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("edit_order_value")
                        )
                        OutlinedTextField(
                            value = paymentTerm,
                            onValueChange = { paymentTerm = it },
                            label = { Text("Condições PGTO") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    var statusExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = !statusExpanded }
                    ) {
                        OutlinedTextField(
                            value = getDisplayStatus(status),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Etapa do Atendimento") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            listOf("Realizado", "A Faturar", "Faturado", "Entregue").forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(getDisplayStatus(item)) },
                                    onClick = {
                                        status = item
                                        statusExpanded = false
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
                        val valDouble = value.toDoubleOrNull() ?: 0.0
                        viewModel.saveOrder(
                            id = order.id,
                            visitId = order.visitId,
                            clientId = order.clientId,
                            clientName = order.clientName,
                            notes = notes,
                            value = valDouble,
                            status = status,
                            paymentTerm = paymentTerm
                        )
                        Toast.makeText(context, "Pedido atualizado!", Toast.LENGTH_SHORT).show()
                        showEditOrderDialog = null
                    },
                    modifier = Modifier.testTag("submit_edit_order")
                ) {
                    Text("Atualizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditOrderDialog = null }) { Text("Cancelar") }
            }
        )
    }

    // DIALOG: DELETE ORDER CONFIRM
    showDeleteConfirmDialog?.let { order ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Excluir Pedido?") },
            text = { Text("Tem certeza de que deseja remover permanentemente o pedido de '${order.clientName}' no valor de ${formatCurrency.format(order.totalValue)}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteOrder(order)
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
}

@Composable
fun OrdersPipelineHeader(orders: List<Order>, formatCurrency: NumberFormat) {
    val valRealizados = orders.filter { it.status == "Realizado" }.sumOf { it.totalValue }
    val valAFaturar = orders.filter { it.status == "A Faturar" }.sumOf { it.totalValue }
    val valFaturado = orders.filter { it.status == "Faturado" }.sumOf { it.totalValue }
    val valEntregue = orders.filter { it.status == "Entregue" }.sumOf { it.totalValue }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Consolidação Financeira", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Section 1
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Realizado", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(formatCurrency.format(valRealizados), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3), maxLines = 1)
                }
                // Section 2
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("A Faturar", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(formatCurrency.format(valAFaturar), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800), maxLines = 1)
                }
                // Section 3
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Faturado", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(formatCurrency.format(valFaturado), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9C27B0), maxLines = 1)
                }
                // Section 4
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Entregues", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(formatCurrency.format(valEntregue), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun OrderItemRow(
    order: Order,
    clientPhone: String,
    formatCurrency: NumberFormat,
    onStatusChange: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("order_card_${order.id}"),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Order Status Indicator Badge
                        Box(
                            modifier = Modifier
                                .background(getOrderStatusColor(order.status).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                getDisplayStatus(order.status).uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = getOrderStatusColor(order.status)
                            )
                        }
                        
                        if (order.paymentTerm.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(order.paymentTerm, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(order.clientName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }

                Text(formatCurrency.format(order.totalValue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = getOrderStatusColor(order.status))
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Notebook text block (Produtos)
            Text(
                text = order.notes,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(8.dp).fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cód. Pedido #${order.id}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                Text(sdf.format(Date(order.date)), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }

            // Expanded panels showing lifecycle progression controls
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Spacer(modifier = Modifier.height(10.dp))

                    if (clientPhone.isNotEmpty()) {
                        Text("Contato do Cliente:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val context = LocalContext.current
                            // Dial Phone Button
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                    .clickable { dialNumber(context, clientPhone) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "Ligar",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Ligar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            // WhatsApp Button
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF25D366), RoundedCornerShape(8.dp))
                                    .clickable { openWhatsApp(context, clientPhone) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Chat,
                                    contentDescription = "WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text("Avançar Categoria de Atendimento:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Progression controls
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                "Realizado" to "Ped.",
                                "A Faturar" to "Fat.",
                                "Faturado" to "Bill.",
                                "Entregue" to "Entr."
                            ).forEach { (status, label) ->
                                val active = order.status == status
                                InputChip(
                                    selected = active,
                                    onClick = { onStatusChange(status) },
                                    label = { Text(label, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("order_prog_btn_${order.id}_$status")
                                )
                            }
                        }

                        // Edit / Delete buttons
                        Row {
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

// Map database statuses into local friendly Portuguese strings
fun getDisplayStatus(status: String): String {
    return when (status) {
        "Realizado" -> "Pedido Realizado"
        "A Faturar" -> "Enviado ao Faturamento"
        "Faturado" -> "Aguardando Entrega"
        "Entregue" -> "Entregue ao Cliente"
        else -> status
    }
}

fun getOrderStatusColor(status: String): Color {
    return when (status) {
        "Realizado" -> Color(0xFF2196F3) // Blue
        "A Faturar" -> Color(0xFFFF9800) // Orange
        "Faturado" -> Color(0xFF9C27B0) // Purple
        "Entregue" -> Color(0xFF4CAF50) // Green
        else -> Color.Gray
    }
}

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

private fun openWhatsApp(context: Context, number: String) {
    try {
        val sanitized = number.replace("-", "").replace(" ", "").replace("(", "").replace(")", "").replace("+", "")
        val target = if (sanitized.length <= 11 && !sanitized.startsWith("55")) "55$sanitized" else sanitized
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$target")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Não foi possível abrir o WhatsApp.", Toast.LENGTH_SHORT).show()
    }
}
