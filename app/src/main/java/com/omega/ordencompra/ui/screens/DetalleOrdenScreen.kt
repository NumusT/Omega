package com.omega.ordencompra.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import com.omega.ordencompra.data.db.entities.CatalogoProductoEntity
import com.omega.ordencompra.data.db.entities.HistorialEntity
import com.omega.ordencompra.data.db.entities.OrdenEntity
import com.omega.ordencompra.data.db.entities.ProductoEntity
import com.omega.ordencompra.ui.theme.PrimaryBlue
import com.omega.ordencompra.ui.theme.RedError
import com.omega.ordencompra.util.DateUtils
import com.omega.ordencompra.util.PdfGenerator
import com.omega.ordencompra.viewmodel.OrdenViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleOrdenScreen(
    ordenId: String,
    viewModel: OrdenViewModel,
    isAdmin: Boolean,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onDuplicateSuccess: (String) -> Unit = {}
) {
    var orden by remember { mutableStateOf<OrdenEntity?>(null) }
    val productos by viewModel.currentProductos.collectAsState()
    val catalogo by viewModel.catalogo.collectAsState()
    val clientes by viewModel.clientes.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var editEstado by remember { mutableStateOf("") }
    var editObservaciones by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var productsModified by remember { mutableStateOf(false) }
    var isDuplicating by remember { mutableStateOf(false) }
    var isSavingEdit by remember { mutableStateOf(false) }
    val editProductos = remember { mutableStateListOf<ProductoEntity>() }
    var historialItems by remember { mutableStateOf<List<HistorialEntity>>(emptyList()) }
    var historialExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(ordenId) {
        viewModel.loadOrden(ordenId)
        viewModel.loadProductos(ordenId)
    }

    LaunchedEffect(ordenId) {
        viewModel.getHistorialByOrdenId(ordenId).collect { historialItems = it }
    }

    val currentOrden by viewModel.currentOrden.collectAsState()
    LaunchedEffect(currentOrden) {
        if (currentOrden != null && orden == null) {
            if (!isAdmin && currentOrden!!.usuarioId != currentUserId) {
                Toast.makeText(context, "No tienes permisos para ver este pedido", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            } else {
                orden = currentOrden
                editEstado = currentOrden!!.estado
                editObservaciones = currentOrden!!.observaciones
            }
        }
    }

    LaunchedEffect(productos) {
        if (editProductos.isEmpty() && productos.isNotEmpty() && !productsModified) {
            editProductos.clear()
            editProductos.addAll(productos)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(orden?.let { "OC-${it.numeroOrden}" } ?: "Detalle")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = {
                            if (isEditing) {
                                editProductos.clear()
                                editProductos.addAll(productos)
                                productsModified = false
                            }
                            isEditing = !isEditing
                            if (isEditing && orden != null) {
                                editEstado = orden!!.estado
                                editObservaciones = orden!!.observaciones
                                editProductos.clear()
                                editProductos.addAll(productos)
                            }
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        if (!isEditing) {
                            IconButton(
                                onClick = {
                                    isDuplicating = true
                                    val currentUser = com.omega.ordencompra.data.db.entities.UserEntity()
                                    viewModel.duplicarOrden(orden!!, productos, currentUserId) { newId ->
                                        isDuplicating = false
                                        onDuplicateSuccess(newId)
                                    }
                                },
                                enabled = !isDuplicating
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar")
                            }
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = RedError)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        orden?.let { ord ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Cliente: ${ord.clienteNombre}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Fecha: ${DateUtils.formatDateForDisplay(ord.fecha)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            EstadoBadge(estado = ord.estado)
                        }
                    }
                }
                if (isEditing) {
                    item {
                        var estadoExpanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = estadoExpanded,
                            onExpandedChange = { estadoExpanded = !estadoExpanded }
                        ) {
                            OutlinedTextField(
                                value = editEstado,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Estado") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = estadoExpanded)
                                },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = estadoExpanded,
                                onDismissRequest = { estadoExpanded = false }
                            ) {
                                for (estado in OrdenEstados.todos) {
                                    DropdownMenuItem(
                                        text = { 
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                EstadoBadge(estado = estado)
                                            }
                                        },
                                        onClick = {
                                            editEstado = estado
                                            estadoExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = editObservaciones,
                            onValueChange = { editObservaciones = it.uppercase() },
                            label = { Text("Observaciones") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2, maxLines = 4
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = {
                                isSavingEdit = true
                                val updatedOrden = ord.copy(estado = editEstado, observaciones = editObservaciones)
                                if (productsModified) {
                                    viewModel.updateOrdenConProductos(
                                        updatedOrden,
                                        editProductos.toList(),
                                        onComplete = {
                                            isSavingEdit = false
                                            orden = updatedOrden.copy(total = editProductos.sumOf { it.total })
                                            isEditing = false
                                            productsModified = false
                                        }
                                    )
                                } else {
                                    viewModel.updateOrden(updatedOrden)
                                    orden = updatedOrden
                                    isEditing = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSavingEdit
                        ) {
                            Text(if (isSavingEdit) "Guardando..." else "Guardar cambios")
                        }
                    }
                } else {
                    if (ord.observaciones.isNotBlank()) {
                        item {
                            Text("Observaciones:", style = MaterialTheme.typography.titleMedium)
                            Text(ord.observaciones, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Productos", style = MaterialTheme.typography.titleLarge)
                        val displayProductos = if (isEditing) editProductos else productos
                        Text("${displayProductos.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val displayProductos = if (isEditing) editProductos else productos
                items(displayProductos, key = { it.id.ifEmpty { it.productoCatalogoId } }) { producto ->
                    if (isEditing) {
                        ProductoEditableCard(
                            producto = producto,
                            onIncrement = {
                                val idx = editProductos.indexOfFirst { it.productoCatalogoId == producto.productoCatalogoId }
                                if (idx >= 0) {
                                    val p = editProductos[idx]
                                    val newCant = p.cantidad + 1
                                    editProductos[idx] = p.copy(cantidad = newCant, total = newCant * p.precioUnitario)
                                    productsModified = true
                                }
                            },
                            onDecrement = {
                                val idx = editProductos.indexOfFirst { it.productoCatalogoId == producto.productoCatalogoId }
                                if (idx >= 0) {
                                    val p = editProductos[idx]
                                    if (p.cantidad <= 1) {
                                        editProductos.removeAt(idx)
                                    } else {
                                        val newCant = p.cantidad - 1
                                        editProductos[idx] = p.copy(cantidad = newCant, total = newCant * p.precioUnitario)
                                    }
                                    productsModified = true
                                }
                            },
                            onRemove = {
                                editProductos.removeAll { it.productoCatalogoId == producto.productoCatalogoId }
                                productsModified = true
                            }
                        )
                    } else {
                        ProductoDetalleCard(producto)
                    }
                }
                if (isEditing) {
                    item {
                        OutlinedButton(
                            onClick = { showAddProductDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Añadir producto del catálogo")
                        }
                    }
                }
                item {
                    val currencyFormat = remember {
                        NumberFormat.getCurrencyInstance(Locale("es", "MX"))
                    }
                    val displayProductos = if (isEditing) editProductos else productos
                    val displayTotal = if (isEditing && productsModified) displayProductos.sumOf { it.total } else ord.total
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currencyFormat.format(displayTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                item {
                    Button(
                        onClick = {
                            val cliente = clientes.firstOrNull { it.id == ord.clienteId }
                            val uri = PdfGenerator.generateOrdenPdf(context, ord, productos, cliente)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartir orden de compra"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartir PDF")
                    }
                }

                // Historial de Cambios
                if (historialItems.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.dp),
                            onClick = { historialExpanded = !historialExpanded }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Historial de Cambios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (historialExpanded) "▲" else "▼",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (historialExpanded) {
                        items(historialItems, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.width(4.dp).height(40.dp).padding(top = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier.size(8.dp)
                                                .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(item.accion, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(item.fecha.take(10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (item.detalle.isNotBlank()) {
                                            Text(item.detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (item.usuarioNombre.isNotBlank()) {
                                            Text(item.usuarioNombre, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } ?: run {
            Text(
                text = "Cargando...",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            )
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            catalogo = catalogo,
            existingIds = editProductos.map { it.productoCatalogoId }.toSet(),
            onDismiss = { showAddProductDialog = false },
            onAdd = { catalogProduct, cantidad ->
                val existing = editProductos.indexOfFirst { it.productoCatalogoId == catalogProduct.id }
                if (existing >= 0) {
                    val p = editProductos[existing]
                    val newCant = p.cantidad + cantidad
                    editProductos[existing] = p.copy(cantidad = newCant, total = newCant * p.precioUnitario)
                } else {
                    editProductos.add(
                        ProductoEntity(
                            productoCatalogoId = catalogProduct.id,
                            nombre = catalogProduct.nombre,
                            cantidad = cantidad,
                            precioUnitario = catalogProduct.precioUnitario,
                            total = cantidad * catalogProduct.precioUnitario
                        )
                    )
                }
                productsModified = true
                showAddProductDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar orden") },
            text = { Text("¿Eliminar esta orden de compra?") },
            confirmButton = {
                TextButton(onClick = {
                    orden?.let { viewModel.deleteOrden(it) }
                    onNavigateBack()
                    showDeleteDialog = false
                }) { Text("Eliminar", color = RedError) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun ProductoDetalleCard(producto: ProductoEntity) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = producto.productoCatalogoId.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (producto.nombre.isNotBlank()) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${producto.cantidad} x ${currencyFormat.format(producto.precioUnitario)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currencyFormat.format(producto.total),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ProductoEditableCard(
    producto: ProductoEntity,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto.productoCatalogoId.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (producto.nombre.isNotBlank()) {
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = currencyFormat.format(producto.total),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement) {
                    Text("-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "${producto.cantidad}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = onIncrement) {
                    Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = RedError)
                }
            }
        }
    }
}

@Composable
private fun AddProductDialog(
    catalogo: List<CatalogoProductoEntity>,
    existingIds: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (CatalogoProductoEntity, Int) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(catalogo, search) {
        if (search.isBlank()) catalogo
        else catalogo.filter { it.nombre.contains(search, ignoreCase = true) || it.id.contains(search, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir producto") },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it.uppercase() },
                    placeholder = { Text("Buscar producto...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filtered, key = { it.id }) { producto ->
                        val alreadyInCart = existingIds.contains(producto.id)
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !alreadyInCart) { onAdd(producto, 1) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (alreadyInCart) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(if (alreadyInCart) 0.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(producto.id.uppercase(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(producto.nombre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (alreadyInCart) {
                                    Text("Ya agregado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar", tint = PrimaryBlue)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
