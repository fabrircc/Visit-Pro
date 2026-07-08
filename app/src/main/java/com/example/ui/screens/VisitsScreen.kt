package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.Visit
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitsScreen(viewModel: MainViewModel, onNavigateToMap: () -> Unit) {
    val context = LocalContext.current
    val visits by viewModel.filteredVisits.collectAsState()
    val neighborhoods by viewModel.uniqueNeighborhoods.collectAsState()

    val selectedBairro by viewModel.selectedBairro.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()

    // Dialog triggering states
    var showCompleteDialog by remember { mutableStateOf<Visit?>(null) }
    var showCancelDialog by remember { mutableStateOf<Visit?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Visit?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Report Generation Actions Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.exportToExcel(context) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("export_excel_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B5E20), // Dark Green
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar Excel", fontSize = 12.sp)
            }

            Button(
                onClick = { viewModel.exportToPdf(context) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("export_pdf_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB71C1C), // Dark Red
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar PDF", fontSize = 12.sp)
            }
        }

        // Filters Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Filter Period dropdown
            var periodExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                ExposedDropdownMenuBox(
                    expanded = periodExpanded,
                    onExpandedChange = { periodExpanded = !periodExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPeriod ?: "Período",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("visit_period_selector"),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = periodExpanded,
                        onDismissRequest = { periodExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos", fontSize = 12.sp) },
                            onClick = {
                                viewModel.setPeriodFilter(null)
                                periodExpanded = false
                            }
                        )
                        listOf("Manhã", "Tarde", "Noite").forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setPeriodFilter(period)
                                    periodExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Filter Status dropdown
            var statusExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedStatus ?: "Status",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("visit_status_selector"),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos", fontSize = 12.sp) },
                            onClick = {
                                viewModel.setStatusFilter(null)
                                statusExpanded = false
                            }
                        )
                        listOf("A Realizar", "Realizada", "Cancelada").forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setStatusFilter(status)
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Filter Bairro dropdown
            var bairroExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                ExposedDropdownMenuBox(
                    expanded = bairroExpanded,
                    onExpandedChange = { bairroExpanded = !bairroExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBairro ?: "Bairro",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bairroExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("visit_bairro_selector"),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = bairroExpanded,
                        onDismissRequest = { bairroExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos", fontSize = 12.sp) },
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
        }

        // Active clear chip
        if (selectedBairro != null || selectedPeriod != null || selectedStatus != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                SuggestionChip(
                    onClick = {
                        viewModel.setBairroFilter(null)
                        viewModel.setPeriodFilter(null)
                        viewModel.setStatusFilter(null)
                    },
                    label = { Text("Limpar Filtros", fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp)) }
                )
            }
        }

        // Route optimization overview trigger
        val pendingOnScreen = visits.filter { it.status == "A Realizar" }
        if (pendingOnScreen.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = { onNavigateToMap() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("optimize_on_screen_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Roteirizar Atendimentos (${pendingOnScreen.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Visits list
        if (visits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(
                        Icons.Default.EventNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma visita encontrada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Você pode agendar visitas tocando em qualquer cliente na aba Clientes.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("visits_lazy_list")
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visits, key = { it.id }) { visit ->
                    VisitItemRow(
                        visit = visit,
                        onComplete = { showCompleteDialog = visit },
                        onCancel = { showCancelDialog = visit },
                        onDelete = { showDeleteConfirm = visit }
                    )
                }
            }
        }
    }

    // DIALOG: COMPLETE VISIT (With integrated Order taking block)
    showCompleteDialog?.let { visit ->
        var kms by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var includesOrder by remember { mutableStateOf(false) }
        
        // Order details states
        var orderNotes by remember { mutableStateOf("") }
        var orderValue by remember { mutableStateOf("") }
        var paymentTerm by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCompleteDialog = null },
            title = { Text("Registrar Conclusão de Visita") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text("Cliente: ${visit.clientName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Bairro: ${visit.neighborhood}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = kms,
                            onValueChange = { kms = it },
                            label = { Text("Quilometragem Rodada (km)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("complete_visit_kms")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Anotações da Visita / Atendimento") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Integrated Order taking section!
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (includesOrder) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Bloco de Notas de Pedido?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    IconButton(
                                        onClick = { includesOrder = !includesOrder },
                                        modifier = Modifier.size(36.dp).testTag("toggle_order_btn")
                                    ) {
                                        Icon(
                                            if (includesOrder) Icons.Default.CheckCircle else Icons.Default.EventNote,
                                            contentDescription = null,
                                            tint = if (includesOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                
                                if (includesOrder) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = orderNotes,
                                        onValueChange = { orderNotes = it },
                                        placeholder = { Text("Ex: 10un Produto A, 5un Produto B") },
                                        label = { Text("Produtos / Pedidos realizados") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = orderValue,
                                            onValueChange = { orderValue = it },
                                            label = { Text("Valor Total (R$)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f).testTag("order_total_value")
                                        )
                                        OutlinedTextField(
                                            value = paymentTerm,
                                            onValueChange = { paymentTerm = it },
                                            label = { Text("Forma PGTO") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val kmsDouble = kms.toDoubleOrNull() ?: 0.0
                        viewModel.updateVisitStatus(visit, "Realizada", kmsDouble, notes)

                        if (includesOrder) {
                            val valueDouble = orderValue.toDoubleOrNull() ?: 0.0
                            viewModel.saveOrder(
                                visitId = visit.id,
                                clientId = visit.clientId,
                                clientName = visit.clientName,
                                notes = orderNotes,
                                value = valueDouble,
                                status = "Realizado", // Initial phase of order lifecycle
                                paymentTerm = paymentTerm
                            )
                            Toast.makeText(context, "Visita concluída e pedido salvo no Bloco!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Visita concluída com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                        showCompleteDialog = null
                    },
                    modifier = Modifier.testTag("submit_complete_visit")
                ) {
                    Text("Concluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = null }) { Text("Cancelar") }
            }
        )
    }

    // DIALOG: CANCEL VISIT
    showCancelDialog?.let { visit ->
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCancelDialog = null },
            title = { Text("Cancelar Visita?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Cliente: ${visit.clientName}")
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Motivo do Cancelamento") },
                        modifier = Modifier.fillMaxWidth().testTag("cancel_visit_reason")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateVisitStatus(visit, "Cancelada", 0.0, notes)
                        Toast.makeText(context, "Visita marcada como cancelada", Toast.LENGTH_SHORT).show()
                        showCancelDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmar Cancelamento", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = null }) { Text("Voltar") }
            }
        )
    }

    // DIALOG: DELETE VISIT HISTORY ENTRY
    showDeleteConfirm?.let { visit ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Excluir registro?") },
            text = { Text("Excluir esta visita para '${visit.clientName}' do seu histórico permanente?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVisit(visit)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun VisitItemRow(
    visit: Visit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("visit_card_${visit.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = when (visit.status) {
                "Realizada" -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                "Cancelada" -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
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
                        // Period Badge
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(visit.period, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Status Badge
                        Box(
                            modifier = Modifier
                                .background(getStatusColor(visit.status).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                visit.status.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = getStatusColor(visit.status)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(visit.clientName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (visit.status == "Cancelada") MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                }

                // Date display
                Text(sdf.format(Date(visit.date)), fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("${visit.address} • ${visit.neighborhood}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Completed visits KMs Driven indicator
            if (visit.status == "Realizada") {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quilômetros Rodados: ${visit.kmsDriven} km", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }

            if (visit.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(visit.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            // Expanded action panel for pending visits
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (visit.status == "A Realizar") {
                                Button(
                                    onClick = onComplete,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(32.dp).testTag("concluir_visita_btn")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Concluir Visita", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = onCancel,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(32.dp).testTag("cancelar_visita_btn")
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancelar", fontSize = 11.sp)
                                }
                            } else {
                                // Already completed/cancelled visits can be re-scheduled or deleted from history
                                Text("Visita Finalizada", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
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

fun getStatusColor(status: String): Color {
    return when (status) {
        "Realizada" -> Color(0xFF4CAF50) // Green
        "Cancelada" -> Color(0xFFF44336) // Red
        else -> Color(0xFF2196F3) // Blue (Pending)
    }
}
