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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.data.db.entities.CatalogoProductoEntity
import com.omega.ordencompra.ui.theme.GreenSuccess
import com.omega.ordencompra.ui.theme.OrangeWarning
import com.omega.ordencompra.ui.theme.RedError
import com.omega.ordencompra.viewmodel.AdminViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InventoryScreen(
    adminViewModel: AdminViewModel,
    isAdmin: Boolean,
    onAddProduct: () -> Unit
) {
    val productos by adminViewModel.productos.collectAsState()
    val search by adminViewModel.searchQueryCatalogo.collectAsState()

    val filteredProductos = if (search.isBlank()) productos
    else productos.filter {
        it.id.contains(search, ignoreCase = true) || it.nombre.contains(search, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { adminViewModel.searchCatalogo(it.uppercase()) },
                placeholder = { Text("Buscar por código o nombre...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        if (filteredProductos.isEmpty() && search.isNotBlank()) {
            item {
                Text("No se encontraron productos", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp))
            }
        } else {
            items(filteredProductos, key = { it.id }) { producto ->
                ProductoInventarioCard(producto)
            }
        }
        if (isAdmin) {
            item {
                FloatingActionButton(
                    onClick = onAddProduct,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar producto")
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ProductoInventarioCard(producto: CatalogoProductoEntity) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    val stockColor = when {
        producto.stock <= 0 -> RedError
        producto.stock < 10 -> OrangeWarning
        else -> GreenSuccess
    }
    val stockLabel = when {
        producto.stock <= 0 -> "Sin stock"
        producto.stock < 10 -> "Stock bajo"
        else -> "En stock"
    }

    val categoria = when {
        producto.nombre.lowercase().contains("papel") || producto.nombre.lowercase().contains("pluma") || producto.nombre.lowercase().contains("oficina") || producto.nombre.lowercase().contains("carpeta") -> "Oficina"
        producto.nombre.lowercase().contains("computadora") || producto.nombre.lowercase().contains("teclado") || producto.nombre.lowercase().contains("mouse") || producto.nombre.lowercase().contains("impresora") -> "Tecnología"
        producto.nombre.lowercase().contains("servicio") || producto.nombre.lowercase().contains("mantenimiento") || producto.nombre.lowercase().contains("limpieza") -> "Servicios"
        else -> "Materiales"
    }

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
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.id.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (producto.nombre.isNotBlank()) {
                    Text(producto.nombre, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Categoría: $categoria", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(currencyFormat.format(producto.precioUnitario), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${producto.stock}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = stockColor)
                EstadoBadge(estado = stockLabel)
            }
        }
    }
}
