package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.data.model.Visit
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun MapRouteScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val visits by viewModel.visits.collectAsState()
    val clients by viewModel.clients.collectAsState()

    // Filter out only active visits "A Realizar"
    val pendingVisits = remember(visits) {
        visits.filter { it.status == "A Realizar" }
    }

    val clientMap = remember(clients) {
        clients.associateBy { it.id }
    }

    // Solve TSP routing
    val optimizedResult = remember(pendingVisits, clients) {
        viewModel.getOptimizedVisits(pendingVisits)
    }

    val orderedRoute = optimizedResult.route
    val totalEstKm = optimizedResult.totalEstimatedDistance

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top stats bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rota Otimizada TSP",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "Vendedor inicia no Centro de Uberlândia",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${orderedRoute.size} Paradas",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "~${String.format(Locale.getDefault(), "%.1f", totalEstKm)} km totais",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        if (orderedRoute.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sem paradas agendadas para roteirizar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Adicione e agende visitas na aba Clientes primeiro, ou use o botão abaixo para simular 5 agendamentos.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            // Schedule 5 random demo visits for the consultant to test with
                            val list = clients.take(5)
                            if (list.isNotEmpty()) {
                                var scheduledCount = 0
                                list.forEachIndexed { idx, client ->
                                    val period = if (idx % 2 == 0) "Manhã" else "Tarde"
                                    viewModel.scheduleVisit(
                                        clientId = client.id,
                                        clientName = client.name,
                                        address = client.address,
                                        neighborhood = client.neighborhood,
                                        date = System.currentTimeMillis(),
                                        period = period,
                                        notes = "Simulação de rota de teste."
                                    )
                                    scheduledCount++
                                }
                                Toast.makeText(context, "$scheduledCount Visitas agendadas para demonstração!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Por favor espere carregar a planilha primeiro.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("sim_route_btn")
                    ) {
                        Text("Gerar Rota Demonstrativa")
                    }
                }
            }
        } else {
            // Interactive visual Canvas map
            var selectedStopIndex by remember { mutableStateOf<Int?>(null) }
            val density = androidx.compose.ui.platform.LocalDensity.current

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            ) {
                val w = with(density) { maxWidth.toPx() }
                val h = with(density) { maxHeight.toPx() }

                // Background Grid Map drawn on Canvas
                val textMeasurer = rememberTextMeasurer()
                val mapPrimaryColor = MaterialTheme.colorScheme.primary

                val baseLat = -18.9186
                val baseLng = -48.2772

                // Map base coordinates into Canvas pixels
                // We will map lat +/- 0.05 and lng +/- 0.05 into w and h
                fun mapToOffset(lat: Double, lng: Double): Offset {
                    val latSpan = 0.12
                    val lngSpan = 0.12
                    val relativeLat = (lat - (baseLat - latSpan / 2)) / latSpan
                    val relativeLng = (lng - (baseLng - lngSpan / 2)) / lngSpan
                    
                    val pxX = (relativeLng * w).toFloat().coerceIn(30f, w - 30f)
                    // Invert Y for screen mapping
                    val pxY = ((1.0 - relativeLat) * h).toFloat().coerceIn(30f, h - 30f)
                    return Offset(pxX, pxY)
                }

                val officeOffset = Offset(w / 2, h / 2)
                
                val coords = remember(orderedRoute, clients) {
                    orderedRoute.map { v ->
                        val cl = clientMap[v.clientId]
                        val lat = cl?.latitude ?: -18.9186
                        val lng = cl?.longitude ?: -48.2772
                        lat to lng
                    }
                }

                val pinOffsets = remember(coords, w, h) {
                    coords.map { pair -> mapToOffset(pair.first, pair.second) }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("route_map_canvas")
                        .pointerInput(pinOffsets) {
                            detectTapGestures { tapOffset ->
                                val threshold = 24.dp.toPx()
                                var clickedIndex: Int? = null
                                var minDistance = Float.MAX_VALUE
                                pinOffsets.forEachIndexed { idx, offset ->
                                    val dx = tapOffset.x - offset.x
                                    val dy = tapOffset.y - offset.y
                                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                    if (dist < threshold && dist < minDistance) {
                                        minDistance = dist
                                        clickedIndex = idx
                                    }
                                }
                                selectedStopIndex = if (selectedStopIndex == clickedIndex) null else clickedIndex
                            }
                        }
                ) {
                    // Draw grid streets for map realism
                    val gridColor = mapPrimaryColor.copy(alpha = 0.08f)
                    for (i in 0..10) {
                        val x = w * (i / 10f)
                        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1.dp.toPx())
                        val y = h * (i / 10f)
                        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1.dp.toPx())
                    }

                    // Start Office base
                    drawCircle(color = Color(0xFF3B82F6), radius = 8.dp.toPx(), center = officeOffset)
                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = officeOffset)
                    
                    // Path lines
                    var lastOffset = officeOffset
                    pinOffsets.forEach { currentOffset ->
                        // Draw path lines
                        drawLine(
                            color = mapPrimaryColor.copy(alpha = 0.5f),
                            start = lastOffset,
                            end = currentOffset,
                            strokeWidth = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                        lastOffset = currentOffset
                    }

                    // Numbers/Circles on Pin nodes
                    pinOffsets.forEachIndexed { idx, pinOffset ->
                        // Outer pulsing glow if selected
                        if (selectedStopIndex == idx) {
                            drawCircle(
                                color = mapPrimaryColor.copy(alpha = 0.25f),
                                radius = 16.dp.toPx(),
                                center = pinOffset
                            )
                            drawCircle(
                                color = mapPrimaryColor.copy(alpha = 0.15f),
                                radius = 24.dp.toPx(),
                                center = pinOffset
                            )
                        }

                        // Pin circle
                        drawCircle(color = mapPrimaryColor, radius = 9.dp.toPx(), center = pinOffset)
                        drawCircle(color = Color.White, radius = 7.5.dp.toPx(), center = pinOffset)
                        drawCircle(color = mapPrimaryColor, radius = 6.dp.toPx(), center = pinOffset)
                        
                        // Step number label
                        val stepStr = "${idx + 1}"
                        val layoutResult = textMeasurer.measure(
                            text = stepStr,
                            style = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        drawText(
                            textLayoutResult = layoutResult,
                            topLeft = Offset(pinOffset.x - layoutResult.size.width / 2, pinOffset.y - layoutResult.size.height / 2)
                        )
                    }
                }

                // Legend Indicators
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF3B82F6), CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Base/Início", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(mapPrimaryColor, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clientes", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Floating detailed card for selected stop
                selectedStopIndex?.let { idx ->
                    if (idx in orderedRoute.indices) {
                        val visit = orderedRoute[idx]
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(8.dp)
                                .fillMaxWidth()
                                .testTag("canvas_selected_visit_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PARADA ${idx + 1} • OTIMIZADA",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = visit.clientName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = visit.address,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            launchGoogleMapsNavigation(context, visit, clientMap[visit.clientId])
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Ir", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { selectedStopIndex = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Fechar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step-by-step stops list with Google Maps routing launcher
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("optimized_route_steps_list")
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text(
                        text = "Ordem Sequencial da Rota",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                itemsIndexed(orderedRoute) { index, visit ->
                    val client = clientMap[visit.clientId]
                    RouteStopRow(
                        index = index,
                        visit = visit,
                        client = client,
                        onNavigate = {
                            launchGoogleMapsNavigation(context, visit, client)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RouteStopRow(
    index: Int,
    visit: Visit,
    client: Client?,
    onNavigate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("route_stop_row_$index"),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sequence Number Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = visit.clientName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${visit.address} • ${visit.neighborhood}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (client != null && client.contact.isNotEmpty()) {
                    Text(
                        text = "Contato: ${client.contact} (${client.phone})",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Navigation trigger deep link button
            Button(
                onClick = onNavigate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("start_nav_btn_$index")
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Navegar", fontSize = 11.sp)
            }
        }
    }
}

// Launches real Google Maps Navigation application via Turn-by-Turn GPS Intent
private fun launchGoogleMapsNavigation(context: Context, visit: Visit, client: Client?) {
    try {
        // Build navigation URI. Uses coordinates if available, fallback to beautiful address search
        val uri = if (client != null && client.latitude != 0.0) {
            // navigation using precise coordinates
            Uri.parse("google.navigation:q=${client.latitude},${client.longitude}&mode=d")
        } else {
            // fallback using address
            val query = Uri.encode("${visit.address}, ${visit.neighborhood}, Uberlandia, MG")
            Uri.parse("google.navigation:q=$query&mode=d")
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps") // Force open in official Google Maps app
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            // If official Google Maps app is missing, fallback to opening maps link in browser
            val webUri = if (client != null && client.latitude != 0.0) {
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${client.latitude},${client.longitude}")
            } else {
                val query = Uri.encode("${visit.address}, ${visit.neighborhood}, Uberlandia, MG")
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$query")
            }
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao abrir navegação: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
