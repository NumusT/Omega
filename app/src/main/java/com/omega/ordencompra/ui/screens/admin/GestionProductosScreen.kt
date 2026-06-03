package com.omega.ordencompra.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.data.db.entities.CatalogoProductoEntity
import com.omega.ordencompra.viewmodel.AdminViewModel
import java.text.NumberFormat
import java.util.Locale
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionProductosScreen(
    adminViewModel: AdminViewModel,
    isAdmin: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val productosPaging = adminViewModel.productosPagingFlow.collectAsLazyPagingItems()
    val search by adminViewModel.searchQueryCatalogo.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }
    var showCsvDialog by remember { mutableStateOf(false) }
    var editProducto by remember { mutableStateOf<CatalogoProductoEntity?>(null) }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = inputStream?.bufferedReader()
                val lines = reader?.readLines() ?: emptyList()
                if (lines.isNotEmpty()) {
                    adminViewModel.importProductosFromCsv(
                        lines = lines,
                        onSuccess = { count ->
                            Toast.makeText(context, "Se importaron $count productos correctamente", Toast.LENGTH_LONG).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Error al importar: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "El archivo seleccionado está vacío", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al abrir el archivo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) { adminViewModel.loadProductos() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Productos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showCsvDialog = true }) {
                            Icon(Icons.Default.Upload, contentDescription = "Carga masiva (CSV)")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = {
                    editProducto = null
                    showDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar producto")
                }
            }
        }
    ) { paddingValues ->
        if (productosPaging.itemCount == 0 && search.isBlank() && productosPaging.loadState.append.endOfPaginationReached) {
            Text(
                text = "No hay productos. Agrega uno.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { adminViewModel.searchCatalogo(it) },
                        placeholder = { Text("Buscar por código...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                if (productosPaging.itemCount == 0 && search.isNotBlank() && productosPaging.loadState.append.endOfPaginationReached) {
                    item {
                        Text(
                            text = "No se encontraron productos",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(
                        count = productosPaging.itemCount,
                        key = productosPaging.itemKey { it.id },
                        contentType = productosPaging.itemContentType { "CatalogoProductoEntity" }
                    ) { index ->
                        val producto = productosPaging[index]
                        if (producto != null) {
                            ProductoCatalogoCard(
                                producto = producto,
                                isAdmin = isAdmin,
                                onEdit = {
                                    editProducto = producto
                                    showDialog = true
                                },
                                onDelete = { adminViewModel.deleteProducto(producto) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ProductoDialog(
            producto = editProducto,
            onDismiss = { showDialog = false },
            onSave = { p ->
                if (editProducto != null) adminViewModel.updateProducto(p)
                else adminViewModel.insertProducto(p)
                showDialog = false
            }
        )
    }

    if (showCsvDialog) {
        AlertDialog(
            onDismissRequest = { showCsvDialog = false },
            title = { Text("Carga Masiva de Productos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Importa productos de forma masiva seleccionando un archivo CSV.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "El archivo debe tener el formato:\n" +
                        "codigo, descripcion, stock, precio\n\n" +
                        "Ejemplo:\n" +
                        "PRD-001, Pluma Azul, 150, 12.50\n" +
                        "PRD-002, Cuaderno Raya, 80, 45.00\n\n" +
                        "Soporta separador por comas (,) o punto y coma (;). Si la primera fila contiene 'codigo', 'descripcion', 'stock' o 'precio', se considerará como encabezado y se omitirá automáticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCsvDialog = false
                    filePickerLauncher.launch("*/*")
                }) {
                    Text("Seleccionar archivo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ProductoCatalogoCard(
    producto: CatalogoProductoEntity,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto.id.uppercase(),
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
                    text = "Stock: ${producto.stock} | Precio: ${currencyFormat.format(producto.precioUnitario)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (isAdmin) {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductoDialog(
    producto: CatalogoProductoEntity?,
    onDismiss: () -> Unit,
    onSave: (CatalogoProductoEntity) -> Unit
) {
    var id by remember { mutableStateOf(producto?.id ?: "") }
    var nombre by remember { mutableStateOf(producto?.nombre ?: "") }
    var stock by remember { mutableIntStateOf(producto?.stock ?: 0) }
    var precio by remember { mutableDoubleStateOf(producto?.precioUnitario ?: 0.0) }
    var stockText by remember { mutableStateOf((producto?.stock ?: 0).toString()) }
    var precioText by remember { mutableStateOf(if (producto != null) producto.precioUnitario.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (producto != null) "Editar producto" else "Nuevo producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it.uppercase() },
                    label = { Text("Código") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = (producto == null)
                )
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it; stock = it.toIntOrNull() ?: 0 },
                    label = { Text("Stock") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = precioText,
                    onValueChange = { precioText = it; precio = it.toDoubleOrNull() ?: 0.0 },
                    label = { Text("Precio") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (id.isNotBlank() && nombre.isNotBlank() && precioText.isNotBlank()) {
                    onSave(CatalogoProductoEntity(
                        id = id,
                        codigo = id, // Set codigo = id to facilitate search
                        nombre = nombre,
                        stock = stock,
                        precioUnitario = precio
                    ))
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}


