package com.omega.ordencompra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.data.db.entities.ClienteEntity
import com.omega.ordencompra.util.DateUtils
import com.omega.ordencompra.viewmodel.AdminViewModel
import com.omega.ordencompra.viewmodel.OrdenViewModel
import java.text.NumberFormat
import java.util.Locale
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType

data class CustomerDisplayData(
    val cliente: ClienteEntity,
    val ltv: Double,
    val lastOrderDate: String,
    val lastOrderSortValue: String
)

@Composable
fun CustomersScreen(
    adminViewModel: AdminViewModel,
    ordenViewModel: OrdenViewModel,
    isAdmin: Boolean,
    onAddClient: () -> Unit
) {
    val clientesPaging = adminViewModel.clientesPagingFlow.collectAsLazyPagingItems()
    val ordenes by ordenViewModel.ordenes.collectAsState()
    val search by adminViewModel.searchQueryClientes.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { adminViewModel.searchClientes(it.uppercase()) },
                placeholder = { Text("Buscar por nombre...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        
        if (clientesPaging.itemCount == 0 && search.isNotBlank() && clientesPaging.loadState.append.endOfPaginationReached) {
            item {
                Text("No se encontraron clientes", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp))
            }
        } else {
            items(
                count = clientesPaging.itemCount,
                key = clientesPaging.itemKey { it.id },
                contentType = clientesPaging.itemContentType { "ClienteEntity" }
            ) { index ->
                val cliente = clientesPaging[index]
                if (cliente != null) {
                    val clientOrders = ordenes.filter { it.clienteId == cliente.id }
                    val ltv = clientOrders.sumOf { it.total }
                    val lastOrder = clientOrders.maxByOrNull { it.numeroOrden }
                    val lastOrderDate = lastOrder?.fecha?.let { DateUtils.formatDateForDisplay(it) } ?: "Sin pedidos"
                    val lastOrderSortValue = lastOrder?.numeroOrden ?: ""
                    
                    ClienteCard(CustomerDisplayData(cliente, ltv, lastOrderDate, lastOrderSortValue))
                }
            }
        }
        if (isAdmin) {
            item {
                FloatingActionButton(onClick = onAddClient) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar cliente")
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ClienteCard(data: CustomerDisplayData) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(data.cliente.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                EstadoBadge(estado = if (data.ltv > 0) "Activo" else "Nuevo")
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (data.cliente.rif.isNotBlank()) {
                Text("RIF: ${data.cliente.rif}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            if (data.cliente.email.isNotBlank()) {
                Text("Email: ${data.cliente.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (data.cliente.telefono.isNotBlank()) {
                Text("Teléfono: ${data.cliente.telefono}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (data.cliente.direccion.isNotBlank()) {
                Text("Dirección: ${data.cliente.direccion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Valor del Cliente (LTV)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(currencyFormat.format(data.ltv), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Último Pedido", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(data.lastOrderDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

