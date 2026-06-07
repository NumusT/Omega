package com.omega.ordencompra.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.omega.ordencompra.data.db.entities.ClienteEntity
import com.omega.ordencompra.util.PagoData
import com.omega.ordencompra.util.PagoService
import com.omega.ordencompra.viewmodel.OrdenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.util.Locale

private val modalidades = listOf(
    "BOFA", "BOFA/EFECTIVO", "BOFA/BOLIVARES",
    "EFECTIVO EN DOLARES", "BOLIVARES", "EFECTIVO/BOLIVARES",
    "MERCANTIL JURY", "BANESCO USA", "BANESCO USA/ EFECTIVO",
    "BANESCO USA/DOLARES"
)

private val vendedores = listOf(
    "DIRECTO", "ZONA CENTRO", "ZONA ZULIA", "ZONA ANDES", "ZONA LARA-PORTUGUESA"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroPagoScreen(
    ordenViewModel: OrdenViewModel,
    vendedorNombre: String,
    vendedorId: String,
    onNavigateBack: () -> Unit
) {
    val clientes by ordenViewModel.clientes.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var notaEntrega by remember { mutableStateOf("") }
    var selectedCliente by remember { mutableStateOf<ClienteEntity?>(null) }
    var monto by remember { mutableStateOf("") }
    var tipoPago by remember { mutableStateOf("") }
    var modalidad by remember { mutableStateOf("") }
    var referencia by remember { mutableStateOf("") }
    var vendedor by remember { mutableStateOf("") }
    var imagenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imagenBase64 by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showClientDialog by remember { mutableStateOf(false) }
    var clientSearch by remember { mutableStateOf("") }
    var showImagePickerDialog by remember { mutableStateOf(false) }

    var modalidadExpanded by remember { mutableStateOf(false) }
    var vendedorExpanded by remember { mutableStateOf(false) }

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(photoUri!!))
            processImage(bitmap) { bmp, b64 ->
                imagenBitmap = bmp
                imagenBase64 = b64
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "temp_pago.jpg")
            photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(photoUri!!)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
            processImage(bitmap) { bmp, b64 ->
                imagenBitmap = bmp
                imagenBase64 = b64
            }
        }
    }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Pago", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
            item {
                OutlinedTextField(
                    value = notaEntrega,
                    onValueChange = { notaEntrega = it.filter { c -> c.isDigit() } },
                    label = { Text("Nota de Entrega *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            item {
                OutlinedTextField(
                    value = selectedCliente?.let { "${it.nombre} (${it.rif})" } ?: "",
                    onValueChange = {},
                    label = { Text("Cliente *") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showClientDialog = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Seleccionar")
                        }
                    }
                )
            }
            item {
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            item {
                Text("Tipo de Pago *", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { tipoPago = "Parcial" }) {
                        RadioButton(selected = tipoPago == "Parcial", onClick = { tipoPago = "Parcial" })
                        Text("Parcial")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { tipoPago = "Total" }) {
                        RadioButton(selected = tipoPago == "Total", onClick = { tipoPago = "Total" })
                        Text("Total")
                    }
                }
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = modalidadExpanded,
                    onExpandedChange = { modalidadExpanded = it }
                ) {
                    OutlinedTextField(
                        value = modalidad,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Modalidad *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modalidadExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = modalidadExpanded,
                        onDismissRequest = { modalidadExpanded = false }
                    ) {
                        modalidades.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = {
                                    modalidad = opcion
                                    modalidadExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = vendedorExpanded,
                    onExpandedChange = { vendedorExpanded = it }
                ) {
                    OutlinedTextField(
                        value = vendedor,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vendedor *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vendedorExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = vendedorExpanded,
                        onDismissRequest = { vendedorExpanded = false }
                    ) {
                        vendedores.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = {
                                    vendedor = opcion
                                    vendedorExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = referencia,
                    onValueChange = { referencia = it },
                    label = { Text("Referencia") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                Text("Cargar Imagen", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                if (imagenBitmap != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                bitmap = imagenBitmap!!.asImageBitmap(),
                                contentDescription = "Comprobante",
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Fit
                            )
                            TextButton(onClick = { imagenBitmap = null; imagenBase64 = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Quitar imagen")
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showImagePickerDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Seleccionar imagen")
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (email.isBlank() || notaEntrega.isBlank() || selectedCliente == null || monto.isBlank() || tipoPago.isBlank() || modalidad.isBlank() || vendedor.isBlank() || imagenBase64.isBlank()) {
                            Toast.makeText(context, "Completa todos los campos obligatorios (*) y adjunta una imagen", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            val pago = PagoData(
                                email = email,
                                notaEntrega = notaEntrega,
                                cliente = selectedCliente!!.nombre,
                                monto = monto,
                                tipoPago = tipoPago,
                                modalidad = modalidad,
                                referencia = referencia,
                                vendedor = vendedor,
                                imagenBase64 = imagenBase64
                            )
                            val result = PagoService.enviarPago(pago)
                            isLoading = false
                            result.onSuccess {
                                Toast.makeText(context, "Pago registrado correctamente", Toast.LENGTH_LONG).show()
                                onNavigateBack()
                            }.onFailure { e ->
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(4.dp))
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Registrar Pago")
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showClientDialog) {
        AlertDialog(
            onDismissRequest = { showClientDialog = false },
            title = {
                OutlinedTextField(
                    value = clientSearch,
                    onValueChange = { clientSearch = it.uppercase() },
                    label = { Text("Buscar cliente") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
            },
            text = {
                LazyColumn {
                    val filtered = if (clientSearch.isBlank()) clientes
                    else clientes.filter { it.nombre.contains(clientSearch, ignoreCase = true) || it.rif.contains(clientSearch, ignoreCase = true) }
                    if (filtered.isEmpty()) {
                        item { Text("Sin resultados") }
                    } else {
                        items(filtered) { cliente ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable {
                                    selectedCliente = cliente
                                    showClientDialog = false
                                    clientSearch = ""
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (cliente.id == selectedCliente?.id)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(cliente.nombre, fontWeight = FontWeight.SemiBold)
                                    Text(cliente.rif, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClientDialog = false; clientSearch = "" }) { Text("Cancelar") }
            }
        )
    }

    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text("Seleccionar origen") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showImagePickerDialog = false
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tomar foto")
                    }
                    TextButton(
                        onClick = {
                            showImagePickerDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Galería")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImagePickerDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

private fun processImage(bitmap: Bitmap?, onResult: (Bitmap, String) -> Unit) {
    if (bitmap == null) return
    val maxSize = 600
    val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height, 1f)
    val resized = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    val stream = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, 50, stream)
    val bytes = stream.toByteArray()
    val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    onResult(resized, b64)
}
