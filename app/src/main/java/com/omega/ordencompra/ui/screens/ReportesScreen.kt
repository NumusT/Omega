package com.omega.ordencompra.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.ui.theme.BlueInProcess
import com.omega.ordencompra.ui.theme.GreenSuccess
import com.omega.ordencompra.ui.theme.OrangeWarning
import com.omega.ordencompra.ui.theme.PrimaryBlue
import com.omega.ordencompra.viewmodel.OrdenViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(
    ordenViewModel: OrdenViewModel,
    isAdmin: Boolean,
    currentUserId: String,
    onNavigateBack: () -> Unit
) {
    val rawOrdenes by ordenViewModel.ordenes.collectAsState()
    val ordenes = remember(rawOrdenes, isAdmin, currentUserId) {
        if (isAdmin) rawOrdenes
        else rawOrdenes.filter { it.usuarioId == currentUserId }
    }
    val catalogo by ordenViewModel.catalogo.collectAsState()
    val clientes by ordenViewModel.clientes.collectAsState()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    // Period filter
    var selectedPeriod by remember { mutableStateOf("Todo") }
    val periods = listOf("7 días", "Mes", "Trimestre", "Año", "Todo")

    val filteredOrdenes = remember(ordenes, selectedPeriod) {
        if (selectedPeriod == "Todo") ordenes
        else {
            val now = System.currentTimeMillis()
            val millis = when (selectedPeriod) {
                "7 días" -> 7L * 24 * 60 * 60 * 1000
                "Mes" -> 30L * 24 * 60 * 60 * 1000
                "Trimestre" -> 90L * 24 * 60 * 60 * 1000
                "Año" -> 365L * 24 * 60 * 60 * 1000
                else -> Long.MAX_VALUE
            }
            val cutoff = now - millis
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale("es", "MX"))
            ordenes.filter {
                try {
                    val date = sdf.parse(it.fecha)
                    date != null && date.time >= cutoff
                } catch (e: Exception) { true }
            }
        }
    }

    // KPI calculations
    val totalGastado = filteredOrdenes.sumOf { it.total }
    val cantidadOrdenes = filteredOrdenes.size
    val promedioOrden = if (cantidadOrdenes > 0) totalGastado / cantidadOrdenes else 0.0
    val proveedoresActivos = filteredOrdenes.distinctBy { it.clienteId }.size

    // Gasto por proveedor (top 10)
    val gastoPorProveedor = filteredOrdenes
        .groupBy { it.clienteId to it.clienteNombre }
        .map { (key, orders) ->
            Triple(key.second, orders.size, orders.sumOf { it.total })
        }
        .sortedByDescending { it.third }
        .take(10)

    // Top productos
    val topProductos by ordenViewModel.topProductos.collectAsState()
    LaunchedEffect(filteredOrdenes) {
        ordenViewModel.loadTopProductos(filteredOrdenes.map { it.id })
    }

    // Stock dialog state
    var showStockDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes y Analíticas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Period filter chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    periods.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // KPI Cards
            item {
                Text("Resumen General", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        icon = Icons.Default.AttachMoney,
                        title = "Total Gastado",
                        value = currencyFormat.format(totalGastado),
                        color = GreenSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        icon = Icons.Default.ShoppingCart,
                        title = "Órdenes",
                        value = cantidadOrdenes.toString(),
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        icon = Icons.Default.Assessment,
                        title = "Promedio/Orden",
                        value = currencyFormat.format(promedioOrden),
                        color = OrangeWarning,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        icon = Icons.Default.Group,
                        title = "Proveedores",
                        value = proveedoresActivos.toString(),
                        color = BlueInProcess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Estado breakdown
            item {
                Spacer(Modifier.height(8.dp))
                Text("Desglose por Estado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OrdenEstados.todos.forEach { estado ->
                            val count = filteredOrdenes.count { it.estado.equals(estado, ignoreCase = true) }
                            val monto = filteredOrdenes.filter { it.estado.equals(estado, ignoreCase = true) }.sumOf { it.total }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    EstadoBadge(estado = estado)
                                    Text("$count", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    currencyFormat.format(monto),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (estado != OrdenEstados.CANCELADA) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }

            // Top 10 proveedores
            item {
                Spacer(Modifier.height(8.dp))
                Text("Top 10 Proveedores", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            if (gastoPorProveedor.isEmpty()) {
                item {
                    Text("Sin datos para el período seleccionado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Proveedor", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f))
                                Text("Órdenes", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("Monto", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                Text("%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                            }
                            HorizontalDivider()
                            gastoPorProveedor.forEach { (nombre, count, monto) ->
                                val pct = if (totalGastado > 0) (monto / totalGastado * 100) else 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(nombre, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("$count", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text(currencyFormat.format(monto), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                    Text("${String.format("%.1f", pct)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }

            // Top 10 productos más pedidos
            if (topProductos.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Top 10 Productos más Pedidos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Producto", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f))
                                Text("Cant. Pedida", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                            HorizontalDivider()
                            topProductos.forEachIndexed { index, (id, cantidad) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(2f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("${index + 1}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(id.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("$cantidad", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }

            // Inventario overview
            item {
                Spacer(Modifier.height(8.dp))
                Text("Estado del Inventario", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                val sinStock = catalogo.filter { it.stock <= 0 }
                val stockBajo = catalogo.filter { it.stock in 1..9 }
                val stockNormal = catalogo.count { it.stock >= 10 }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f).clickable { showStockDialog = "sin" },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${sinStock.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("Sin Stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).clickable { showStockDialog = "bajo" },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${stockBajo.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OrangeWarning)
                            Text("Stock Bajo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$stockNormal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GreenSuccess)
                            Text("Normal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }


            item { Spacer(Modifier.height(80.dp)) }
        }

    } // Scaffold

    if (showStockDialog != null) {
        val productos = when (showStockDialog) {
            "sin" -> catalogo.filter { it.stock <= 0 }
            "bajo" -> catalogo.filter { it.stock in 1..9 }
            else -> emptyList()
        }
        AlertDialog(
            onDismissRequest = { showStockDialog = null },
            title = { Text(if (showStockDialog == "sin") "Sin Stock" else "Stock Bajo") },
            text = {
                Column {
                    if (productos.isEmpty()) {
                        Text("Ninguno")
                    } else {
                        productos.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(p.id, modifier = Modifier.weight(1f))
                                Text("${p.stock}", color = if (showStockDialog == "sin") MaterialTheme.colorScheme.error else OrangeWarning)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStockDialog = null }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun KpiCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
