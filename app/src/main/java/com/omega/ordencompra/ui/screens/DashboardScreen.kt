package com.omega.ordencompra.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.data.db.entities.OrdenEntity
import com.omega.ordencompra.ui.theme.GreenSuccess
import com.omega.ordencompra.ui.theme.OrangeWarning
import com.omega.ordencompra.ui.theme.PrimaryBlue
import com.omega.ordencompra.ui.theme.RedError
import com.omega.ordencompra.util.UpdateChecker
import com.omega.ordencompra.util.UpdateInfo
import com.omega.ordencompra.viewmodel.OrdenViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    ordenViewModel: OrdenViewModel,
    isAdmin: Boolean,
    currentUserId: String,
    onCreateOrder: () -> Unit,
    onGestionUsuarios: () -> Unit,
    onNavigateToClientes: () -> Unit,
    onNavigateToProductos: () -> Unit,
    onNavigateToReportes: () -> Unit,
    onLogout: () -> Unit
) {
    val rawOrdenes by ordenViewModel.ordenes.collectAsState()
    val ordenes = remember(rawOrdenes, isAdmin, currentUserId) {
        if (isAdmin) rawOrdenes
        else rawOrdenes.filter { it.usuarioId == currentUserId }
    }
    val catalogo by ordenViewModel.catalogo.collectAsState()
    val context = LocalContext.current
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

    var showApproveDialog by remember { mutableStateOf(false) }
    var ordenToApprove by remember { mutableStateOf<com.omega.ordencompra.data.db.entities.OrdenEntity?>(null) }

    val updateChecker = remember { UpdateChecker(context) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        updateInfo = updateChecker.checkForUpdate()
    }

    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("Nueva versión disponible") },
            text = {
                Column {
                    Text("Versión ${updateInfo!!.versionName} disponible para descargar.")
                    if (updateInfo!!.changelog.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(updateInfo!!.changelog, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isDownloading) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text("Descargando...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !isDownloading, onClick = {
                    scope.launch {
                        isDownloading = true
                        val uri = updateChecker.downloadApk(updateInfo!!.apkUrl)
                        isDownloading = false
                        if (uri != null) {
                            updateInfo = null
                            updateChecker.installApk(uri)
                        } else {
                            Toast.makeText(context, "Error al descargar la actualización", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Actualizar") }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) { Text("Más tarde") }
            }
        )
    }

    val pendingCount = ordenes.count { it.estado == "Pendiente" }
    val totalSpend = ordenes.sumOf { it.total }
    val stockCriticoCount = catalogo.count { it.stock < 10 }
    val recentOrders = ordenes.take(5)
    val pendingOrders = ordenes.filter { it.estado == "Pendiente" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Resumen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión", tint = RedError)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    icon = Icons.Default.Description,
                    title = "Pendientes",
                    value = pendingCount.toString(),
                    color = OrangeWarning,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    icon = Icons.Default.Warning,
                    title = "Stock Crítico",
                    value = stockCriticoCount.toString(),
                    color = RedError,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    icon = Icons.Default.AttachMoney,
                    title = "Gasto Total",
                    value = currencyFormat.format(totalSpend),
                    color = GreenSuccess,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    icon = Icons.Default.Group,
                    title = "Proveedores",
                    value = "${ordenes.distinctBy { it.clienteId }.size}",
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Tareas Urgentes (Action Required)
        if (isAdmin && pendingOrders.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Acciones Requeridas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(pendingOrders, key = { "pending_" + it.id }) { orden ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aprobar Orden OC-${orden.numeroOrden}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Cliente: ${orden.clienteNombre}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Total: ${currencyFormat.format(orden.total)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = {
                                ordenToApprove = orden
                                showApproveDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Aprobar")
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("Pedidos Recientes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (recentOrders.isEmpty()) {
            item {
                Text("No hay pedidos aún", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp))
            }
        } else {
            items(recentOrders, key = { it.id }) { orden ->
                RecentOrderCard(orden, currencyFormat)
            }
        }

        // Enlaces Rápidos
        item {
            Spacer(Modifier.height(8.dp))
            Text("Enlaces Rápidos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickLinkCard(
                        icon = Icons.Default.Add,
                        title = "Crear Pedido",
                        subtitle = "Nueva orden de compra",
                        modifier = Modifier.weight(1f),
                        onClick = onCreateOrder
                    )
                    QuickLinkCard(
                        icon = Icons.Default.Group,
                        title = "Proveedores",
                        subtitle = "Gestionar catálogo",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToClientes
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickLinkCard(
                        icon = Icons.Default.Assessment,
                        title = "Reportes",
                        subtitle = "Analíticas de compra",
                        modifier = if (isAdmin) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                        onClick = onNavigateToReportes
                    )
                    if (isAdmin) {
                        QuickLinkCard(
                            icon = Icons.Default.Settings,
                            title = "Configuración",
                            subtitle = "Catálogo productos",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToProductos
                        )
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Administración", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                QuickLinkCard(
                    icon = Icons.Default.Group,
                    title = "Usuarios",
                    subtitle = "Gestionar accesos y roles",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onGestionUsuarios
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    // Approve confirmation dialog
    if (showApproveDialog && ordenToApprove != null) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Confirmar Aprobación") },
            text = {
                Column {
                    Text("¿Deseas aprobar la siguiente orden?")
                    Spacer(Modifier.height(8.dp))
                    Text("Orden: OC-${ordenToApprove!!.numeroOrden}", fontWeight = FontWeight.Bold)
                    Text("Proveedor: ${ordenToApprove!!.clienteNombre}")
                    Text("Monto: ${currencyFormat.format(ordenToApprove!!.total)}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    ordenToApprove?.let {
                        ordenViewModel.updateOrden(it.copy(estado = "Aprobada"))
                        Toast.makeText(context, "Orden OC-${it.numeroOrden} aprobada", Toast.LENGTH_SHORT).show()
                    }
                    showApproveDialog = false
                    ordenToApprove = null
                }) { Text("Aprobar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showApproveDialog = false
                    ordenToApprove = null
                }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SummaryCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentOrderCard(orden: OrdenEntity, currencyFormat: java.text.NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("OC-${orden.numeroOrden}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(orden.clienteNombre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(currencyFormat.format(orden.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                EstadoBadge(estado = orden.estado)
            }
        }
    }
}

@Composable
private fun QuickLinkCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

