package com.omega.ordencompra.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.data.db.entities.ClienteEntity
import com.omega.ordencompra.viewmodel.AdminViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionClientesScreen(
    adminViewModel: AdminViewModel,
    isAdmin: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val clientesPaging = adminViewModel.clientesPagingFlow.collectAsLazyPagingItems()
    val search by adminViewModel.searchQueryClientes.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }
    var showCsvDialog by remember { mutableStateOf(false) }
    var editCliente by remember { mutableStateOf<ClienteEntity?>(null) }
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
                    adminViewModel.importClientesFromCsv(
                        lines = lines,
                        onSuccess = { count ->
                            Toast.makeText(context, "Se importaron $count proveedores correctamente", Toast.LENGTH_LONG).show()
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

    LaunchedEffect(Unit) { adminViewModel.loadClientes() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Clientes") },
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
                    editCliente = null
                    showDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar cliente")
                }
            }
        }
    ) { paddingValues ->
        if (clientesPaging.itemCount == 0 && search.isBlank() && clientesPaging.loadState.append.endOfPaginationReached) {
            Text(
                text = "No hay clientes. Agrega uno.",
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
                        onValueChange = { adminViewModel.searchClientes(it.uppercase()) },
                        placeholder = { Text("Buscar por nombre...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                if (clientesPaging.itemCount == 0 && search.isNotBlank() && clientesPaging.loadState.append.endOfPaginationReached) {
                    item {
                        Text(
                            text = "No se encontraron clientes",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(
                        count = clientesPaging.itemCount,
                        key = clientesPaging.itemKey { it.id },
                        contentType = clientesPaging.itemContentType { "ClienteEntity" }
                    ) { index ->
                        val cliente = clientesPaging[index]
                        if (cliente != null) {
                            ClienteCard(
                                cliente = cliente,
                                isAdmin = isAdmin,
                                onEdit = {
                                    editCliente = cliente
                                    showDialog = true
                                },
                                onDelete = { adminViewModel.deleteCliente(cliente) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ClienteDialog(
            cliente = editCliente,
            onDismiss = { showDialog = false },
            onSave = { c ->
                if (editCliente != null) adminViewModel.updateCliente(c)
                else adminViewModel.insertCliente(c)
                showDialog = false
            }
        )
    }

    if (showCsvDialog) {
        AlertDialog(
            onDismissRequest = { showCsvDialog = false },
            title = { Text("Carga Masiva de Proveedores") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Importa proveedores de forma masiva seleccionando un archivo CSV.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "El archivo debe tener el formato:\n" +
                        "nombre, rif, correo, direccion\n\n" +
                        "Ejemplo:\n" +
                        "Inversiones Alfa, J-12345678-9, contacto@alfa.com, Av. Principal local 2\n" +
                        "Corporacion Beta, G-98765432-1, ventas@beta.com, Zona Industrial Parcela 5\n\n" +
                        "Soporta separador por comas (,) o punto y coma (;). Si la primera fila contiene 'nombre', 'rif', 'correo' o 'direccion', se considerará como encabezado y se omitirá automáticamente.",
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
private fun ClienteCard(
    cliente: ClienteEntity,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    text = cliente.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (cliente.rif.isNotBlank()) {
                    Text(
                        text = "RIF: ${cliente.rif}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (cliente.telefono.isNotBlank()) {
                    Text(
                        text = cliente.telefono,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (cliente.email.isNotBlank()) {
                    Text(
                        text = cliente.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
private fun ClienteDialog(
    cliente: ClienteEntity?,
    onDismiss: () -> Unit,
    onSave: (ClienteEntity) -> Unit
) {
    var nombre by remember { mutableStateOf(cliente?.nombre ?: "") }
    var rif by remember { mutableStateOf(cliente?.rif ?: "") }
    var telefono by remember { mutableStateOf(cliente?.telefono ?: "") }
    var email by remember { mutableStateOf(cliente?.email ?: "") }
    var direccion by remember { mutableStateOf(cliente?.direccion ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (cliente != null) "Editar cliente" else "Nuevo cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.uppercase() },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = rif,
                    onValueChange = { rif = it.uppercase() },
                    label = { Text("RIF") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it.uppercase() },
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.uppercase() },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it.uppercase() },
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isNotBlank()) {
                    onSave(ClienteEntity(
                        id = cliente?.id ?: "",
                        nombre = nombre,
                        telefono = telefono,
                        email = email,
                        direccion = direccion,
                        rif = rif
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
