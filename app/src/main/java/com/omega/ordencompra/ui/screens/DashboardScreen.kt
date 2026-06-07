package com.omega.ordencompra.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.data.db.entities.OrdenEntity
import com.omega.ordencompra.ui.theme.BlueInProcess
import com.omega.ordencompra.ui.theme.GreenDelivered
import com.omega.ordencompra.ui.theme.GreenSuccess
import com.omega.ordencompra.ui.theme.OrangeWarning
import com.omega.ordencompra.ui.theme.PrimaryBlue
import com.omega.ordencompra.ui.theme.RedError
import com.omega.ordencompra.util.UpdateChecker
import com.omega.ordencompra.util.UpdateInfo
import com.omega.ordencompra.viewmodel.OrdenViewModel
import java.text.NumberFormat
import java.util.Locale

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
    onRegistroPago: () -> Unit,
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
    val userName = ordenViewModel.currentUserName.ifBlank { "Usuario" }

    var showApproveDialog by remember { mutableStateOf(false) }
    var ordenToApprove by remember { mutableStateOf<com.omega.ordencompra.data.db.entities.OrdenEntity?>(null) }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (e: Exception) { "?" }
    }

    val updateChecker = remember { UpdateChecker(context) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

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
                    Spacer(Modifier.height(12.dp))
                    Text("Presiona Actualizar para descargar. Luego abre la notificación e instala.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val apkUrl = updateInfo!!.apkUrl
                    updateInfo = null
                    updateChecker.openDownloadInBrowser(apkUrl)
                    Toast.makeText(context, "Descargando... abre la notificacion para instalar", Toast.LENGTH_LONG).show()
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Hola, $userName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Panel de Control",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = RedError,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp).clickable { }
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    icon = Icons.Default.ShoppingCart,
                    title = "Pendientes",
                    value = pendingCount.toString(),
                    color = OrangeWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = Icons.Default.Inventory,
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
                MetricCard(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Ventas Totales",
                    value = currencyFormat.format(totalSpend),
                    color = GreenSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = Icons.Default.Description,
                    title = "Órdenes",
                    value = "${ordenes.size}",
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (isAdmin && pendingOrders.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${pendingCount} ${if (pendingCount == 1) "Orden" else "Órdenes"} por Aprobar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                            Text(
                                "Se requiere revisión de ${pendingOrders.first().clienteNombre}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryBlue.copy(alpha = 0.7f)
                            )
                        }
                        Button(
                            onClick = {
                                ordenToApprove = pendingOrders.first()
                                showApproveDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Aprobar", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Pedidos Recientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (recentOrders.isEmpty()) {
            item {
                Text("No hay pedidos aún", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp))
            }
        } else {
            items(recentOrders, key = { it.id }) { orden ->
                RecentOrderRow(orden, currencyFormat)
            }
        }

        if (isAdmin) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Administración",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminCard(
                            icon = Icons.Default.Person,
                            title = "Usuarios",
                            subtitle = "Gestionar accesos",
                            modifier = Modifier.weight(1f),
                            onClick = onGestionUsuarios
                        )
                        AdminCard(
                            icon = Icons.Default.Inventory,
                            title = "Productos",
                            subtitle = "Catálogo",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToProductos
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminCard(
                            icon = Icons.Default.Description,
                            title = "Clientes",
                            subtitle = "Gestionar clientes",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToClientes
                        )
                        AdminCard(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            title = "Reportes",
                            subtitle = "Analíticas",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToReportes
                        )
                    }
                }
            }
        }

        item {
            Text(
                "v$versionName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

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
private fun MetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
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
private fun AdminCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(1.dp)
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

@Composable
private fun RecentOrderRow(orden: OrdenEntity, currencyFormat: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "OC-${orden.numeroOrden}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    orden.clienteNombre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            OrdenStatusBadge(estado = orden.estado)
            Spacer(Modifier.width(8.dp))
            Text(
                currencyFormat.format(orden.total),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun OrdenStatusBadge(estado: String) {
    val (bg, fg) = when (estado.lowercase()) {
        "pendiente" -> Color(0xFFFFF3E0) to OrangeWarning
        "aprobada" -> Color(0xFFE8F5E9) to GreenDelivered
        "en proceso" -> Color(0xFFE3F2FD) to BlueInProcess
        "entregada" -> Color(0xFFE8F5E9) to GreenSuccess
        "cancelada" -> Color(0xFFEEEEEE) to Color(0xFF757575)
        "rechazada", "sin stock" -> Color(0xFFFFEBEE) to RedError
        "en stock" -> Color(0xFFE8F5E9) to GreenSuccess
        "stock bajo" -> Color(0xFFFFF3E0) to OrangeWarning
        else -> Color(0xFFF3E5F5) to Color(0xFF7B1FA2)
    }
    Text(
        text = estado,
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
