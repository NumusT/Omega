package com.omega.ordencompra.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.data.db.entities.OrdenEntity
import com.omega.ordencompra.ui.theme.GreenSuccess
import com.omega.ordencompra.ui.theme.LocalAccentColor
import com.omega.ordencompra.ui.theme.LocalDarkMode
import com.omega.ordencompra.ui.theme.OrangeWarning
import com.omega.ordencompra.ui.theme.RedError
import com.omega.ordencompra.util.ConnectivityMonitor
import com.omega.ordencompra.viewmodel.NotificacionViewModel
import kotlinx.coroutines.launch
import com.omega.ordencompra.viewmodel.OrdenViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    ordenViewModel: OrdenViewModel,
    notificacionViewModel: NotificacionViewModel? = null,
    isAdmin: Boolean,
    currentUserId: String,
    onCreateOrder: () -> Unit,
    onGestionUsuarios: () -> Unit,
    onNavigateToClientes: () -> Unit,
    onNavigateToProductos: () -> Unit,
    onNavigateToReportes: () -> Unit,
    onRegistroPago: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToAcceso: () -> Unit = {},
    onNotificaciones: () -> Unit = {}
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
    var showThemePicker by remember { mutableStateOf(false) }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (e: Exception) { "?" }
    }

    val pendingCount by remember { derivedStateOf { ordenes.count { it.estado == "Pendiente" } } }
    val totalSpend by remember { derivedStateOf { ordenes.sumOf { it.total } } }
    val stockCriticoCount by remember { derivedStateOf { catalogo.count { it.stock < 10.0 } } }
    val recentOrders by remember { derivedStateOf { ordenes.take(5) } }
    val pendingOrders by remember { derivedStateOf { ordenes.filter { it.estado == "Pendiente" } } }

    // Dark mode + offline
    val isDarkMode = LocalDarkMode.current
    val accentColor = LocalAccentColor.current
    val connectivityMonitor = remember { ConnectivityMonitor(context) }
    val isOnline by connectivityMonitor.isOnline.collectAsState(initial = true)

    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(currentUserId, notificacionViewModel) {
        notificacionViewModel?.loadNotificaciones(currentUserId)
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                kotlinx.coroutines.delay(500)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Offline banner
        if (!isOnline) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OrangeWarning.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = OrangeWarning, modifier = Modifier.size(20.dp))
                        Text("Sin conexión a Internet", style = MaterialTheme.typography.bodySmall, color = OrangeWarning)
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    "Hola, $userName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Resumen de actividades",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    color = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (isAdmin && pendingOrders.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${pendingCount} ${if (pendingCount == 1) "Orden" else "Órdenes"} por Aprobar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor
                            )
                            Text(
                                "Revisión de ${pendingOrders.first().clienteNombre}",
                                style = MaterialTheme.typography.bodySmall,
                                color = accentColor.copy(alpha = 0.7f)
                            )
                        }
                        Button(
                            onClick = {
                                ordenToApprove = pendingOrders.first()
                                showApproveDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(10.dp)
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminCard(
                            icon = Icons.Default.History,
                            title = "Accesos",
                            subtitle = "Registro de actividad",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAcceso
                        )
                        AdminCard(
                            icon = Icons.Default.Palette,
                            title = "Tema",
                            subtitle = "Personalizar colores",
                            modifier = Modifier.weight(1f),
                            onClick = { showThemePicker = true }
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
    }

    if (showThemePicker) {
        ThemePickerDialog(
            currentDarkMode = com.omega.ordencompra.ui.theme.LocalDarkMode.current,
            onDismiss = { showThemePicker = false },
            onThemeChanged = { /* Activity will recompose via state */ }
        )
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
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
    val accent = LocalAccentColor.current
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RecentOrderRow(orden: OrdenEntity, currencyFormat: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            when (orden.estado.lowercase()) {
                                "aprobada" -> GreenSuccess
                                "cancelada" -> RedError
                                else -> OrangeWarning
                            },
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(Modifier.width(12.dp))
                Column {
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
            }
            Spacer(Modifier.width(8.dp))
            EstadoBadge(estado = orden.estado)
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


