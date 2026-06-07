package com.omega.ordencompra.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.omega.ordencompra.data.db.entities.OrdenEntity
import com.omega.ordencompra.ui.theme.GreenSuccess
import com.omega.ordencompra.ui.theme.OrangeWarning
import com.omega.ordencompra.ui.theme.RedError
import com.omega.ordencompra.util.DateUtils
import com.omega.ordencompra.viewmodel.OrdenViewModel
import java.io.File
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ListaOrdenesScreen(
    viewModel: OrdenViewModel,
    isAdmin: Boolean,
    currentUserId: String,
    onCrear: () -> Unit,
    onOrdenClick: (String) -> Unit
) {
    val rawOrdenes by viewModel.ordenes.collectAsState()
    val ordenes = remember(rawOrdenes, isAdmin, currentUserId) {
        if (isAdmin) rawOrdenes
        else rawOrdenes.filter { it.usuarioId == currentUserId }
    }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var ordenToDelete by remember { mutableStateOf<OrdenEntity?>(null) }

    // Filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedEstado by remember { mutableStateOf("Todos") }
    val estadoFilters = listOf("Todos") + OrdenEstados.todos
    var selectedPeriod by remember { mutableStateOf("Este mes") }
    val periods = listOf("Hoy", "7 días", "Este mes", "Todo")

    val filteredOrdenes = remember(ordenes, searchQuery, selectedEstado, selectedPeriod) {
        val now = System.currentTimeMillis()
        val millis = when (selectedPeriod) {
            "Hoy" -> 1L * 24 * 60 * 60 * 1000
            "7 días" -> 7L * 24 * 60 * 60 * 1000
            "Este mes" -> 30L * 24 * 60 * 60 * 1000
            else -> Long.MAX_VALUE
        }
        val cutoff = now - millis
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("es", "MX"))
        ordenes.filter { orden ->
            val matchesSearch = if (searchQuery.isBlank()) true
            else {
                orden.numeroOrden.contains(searchQuery, ignoreCase = true) ||
                orden.clienteNombre.contains(searchQuery, ignoreCase = true) ||
                "OC-${orden.numeroOrden}".contains(searchQuery, ignoreCase = true)
            }
            val matchesEstado = if (selectedEstado == "Todos") true
            else orden.estado.equals(selectedEstado, ignoreCase = true)
            val matchesDate = if (selectedPeriod == "Todo") true
            else {
                try {
                    val date = sdf.parse(orden.fecha)
                    date != null && date.time >= cutoff
                } catch (e: Exception) { true }
            }

            matchesSearch && matchesEstado && matchesDate
        }
    }

    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (ordenes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))
                Text("No hay pedidos", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Crea tu primer pedido", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it.uppercase() },
                            placeholder = { Text("Buscar por OC o proveedor...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            exportOrdenesToCsv(context, filteredOrdenes)
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Exportar CSV", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Filter chips - Estado
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        estadoFilters.forEach { estado ->
                            FilterChip(
                                selected = selectedEstado == estado,
                                onClick = { selectedEstado = estado },
                                label = {
                                    Text(
                                        estado,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Filter chips - Fecha
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
                                label = {
                                    Text(
                                        period,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                )
                            )
                        }
                    }
                }

                // Results count
                item {
                    Text(
                        "${filteredOrdenes.size} orden${if (filteredOrdenes.size != 1) "es" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (filteredOrdenes.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No se encontraron órdenes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isNotBlank() || selectedEstado != "Todos") {
                                Text(
                                    "Intenta cambiar los filtros",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    items(filteredOrdenes, key = { it.id }) { orden ->
                        OrdenCard(
                            orden = orden,
                            isAdmin = isAdmin,
                            onClick = { onOrdenClick(orden.id) },
                            onDelete = {
                                ordenToDelete = orden
                                showDeleteDialog = true
                            }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = onCrear,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nuevo pedido", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }

    if (showDeleteDialog && ordenToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar pedido") },
            text = { Text("¿Eliminar el pedido ${ordenToDelete!!.numeroOrden}?") },
            confirmButton = {
                TextButton(onClick = {
                    ordenToDelete?.let { viewModel.deleteOrden(it) }
                    showDeleteDialog = false
                    ordenToDelete = null
                }) { Text("Eliminar", color = RedError) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

private fun exportOrdenesToCsv(context: Context, ordenes: List<OrdenEntity>) {
    try {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        val sb = StringBuilder()
        sb.appendLine("Numero,Proveedor,Fecha,Total,Estado")
        ordenes.forEach { orden ->
            sb.appendLine("OC-${orden.numeroOrden},\"${orden.clienteNombre}\",${orden.fecha},\"${currencyFormat.format(orden.total)}\",${orden.estado}")
        }

        val file = File(context.cacheDir, "ordenes_export.csv")
        file.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Exportar órdenes"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error al exportar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun OrdenCard(
    orden: OrdenEntity,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("OC-${orden.numeroOrden}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(orden.clienteNombre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(DateUtils.formatDateForDisplay(orden.fecha), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(6.dp))
                Text(currencyFormat.format(orden.total), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                EstadoBadge(estado = orden.estado)
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = RedError.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}
