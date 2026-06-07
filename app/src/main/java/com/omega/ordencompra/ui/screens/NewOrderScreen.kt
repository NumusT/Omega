package com.omega.ordencompra.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.data.db.entities.CatalogoProductoEntity
import com.omega.ordencompra.data.db.entities.ClienteEntity
import com.omega.ordencompra.data.db.entities.ProductoEntity
import com.omega.ordencompra.ui.theme.GreenSuccess
import com.omega.ordencompra.ui.theme.PrimaryBlue
import com.omega.ordencompra.viewmodel.OrdenViewModel
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import com.omega.ordencompra.R

data class OrderLineItem(
    val producto: CatalogoProductoEntity,
    var cantidad: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOrderScreen(
    ordenViewModel: OrdenViewModel,
    usuarioId: String,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val clientes by ordenViewModel.clientes.collectAsState()
    val catalogo by ordenViewModel.catalogo.collectAsState()
    val ordenes by ordenViewModel.ordenes.collectAsState()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    val maxNumero = remember(ordenes) { ordenes.mapNotNull { it.numeroOrden.toIntOrNull() }.maxOrNull() ?: 0 }
    val nextOrderNumber = remember(maxNumero) { (maxNumero + 1).toString().padStart(4, '0') }

    var step by rememberSaveable { mutableIntStateOf(1) }
    var selectedCliente by remember { mutableStateOf<ClienteEntity?>(null) }
    var clientSearch by remember { mutableStateOf("") }
    var productSearch by remember { mutableStateOf("") }
    var cart by remember { mutableStateOf<List<OrderLineItem>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    val filteredClientes = if (clientSearch.isBlank()) clientes
    else clientes.filter { it.nombre.contains(clientSearch, ignoreCase = true) }
    val filteredProductos = if (productSearch.isBlank()) catalogo
    else catalogo.filter { it.nombre.contains(productSearch, ignoreCase = true) || it.id.contains(productSearch, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Nuevo Pedido", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = {
                    if (step > 1) step-- else onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        StepperIndicator(currentStep = step, totalSteps = 3)

        when (step) {
            1 -> StepSelectClient(
                filteredClientes, clientSearch, { clientSearch = it.uppercase() }, { selectedCliente = it; step = 2 },
                selectedCliente?.nombre
            )
            2 -> StepSelectProducts(
                filteredProductos,                 productSearch, { productSearch = it.uppercase() }, cart,
                onAdd = { p ->
                    val existing = cart.indexOfFirst { it.producto.id == p.id }
                    val currentQty = if (existing >= 0) cart[existing].cantidad else 0
                    if (currentQty + 1 > p.stock) {
                        Toast.makeText(context, "Stock insuficiente para ${p.nombre}. Disponible: ${p.stock}", Toast.LENGTH_SHORT).show()
                    } else {
                        if (existing >= 0) {
                            cart = cart.toMutableList().also { it[existing] = it[existing].copy(cantidad = it[existing].cantidad + 1) }
                        } else {
                            cart = cart + OrderLineItem(p, 1)
                        }
                    }
                },
                onRemove = { p -> cart = cart.filter { it.producto.id != p.id } },
                onUpdateCantidad = { id, c ->
                    cart = cart.map { if (it.producto.id == id) it.copy(cantidad = c.coerceAtLeast(0)) else it }
                },
                currencyFormat,
                onNext = if (cart.isNotEmpty()) ({ step = 3 }) else null
            )
            3 -> StepSummary(
                selectedCliente!!, cart, currencyFormat,
                nextOrderNumber = nextOrderNumber,
                onPlaceOrder = {
                    isSaving = true
                    val productos = cart.map { line ->
                        ProductoEntity(
                            productoCatalogoId = line.producto.id,
                            nombre = line.producto.nombre,
                            cantidad = line.cantidad,
                            precioUnitario = line.producto.precioUnitario,
                            total = line.cantidad * line.producto.precioUnitario
                        )
                    }
                    val total = productos.sumOf { it.total }
                    ordenViewModel.crearOrden(
                        clienteId = selectedCliente!!.id,
                        clienteNombre = selectedCliente!!.nombre,
                        usuarioId = usuarioId,
                        productos = productos,
                        total = total,
                        onSuccess = { ordenId ->
                            isSaving = false
                            onSuccess(ordenId)
                        },
                        onError = { isSaving = false }
                    )
                },
                onReject = { step = 2 },
                isSaving = isSaving
            )
        }
    }
}

@Composable
private fun StepperIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val labels = listOf("Cliente", "Productos", "Resumen")
        labels.forEachIndexed { index, label ->
            val isActive = index + 1 <= currentStep
            val isCurrent = index + 1 == currentStep
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isActive) PrimaryBlue else MaterialTheme.colorScheme.outline),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrent) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun StepSelectClient(
    clientes: List<ClienteEntity>,
    search: String,
    onSearchChange: (String) -> Unit,
    onSelect: (ClienteEntity) -> Unit,
    selectedName: String?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Seleccionar Cliente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search, onValueChange = onSearchChange,
                placeholder = { Text("Buscar cliente...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            if (selectedName != null) {
                Text("Seleccionado: $selectedName", color = GreenSuccess,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
        items(clientes, key = { it.id }) { cliente ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(cliente) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(cliente.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("ID: ${cliente.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    Text("Correo: ${cliente.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StepSelectProducts(
    productos: List<CatalogoProductoEntity>,
    search: String,
    onSearchChange: (String) -> Unit,
    cart: List<OrderLineItem>,
    onAdd: (CatalogoProductoEntity) -> Unit,
    onRemove: (CatalogoProductoEntity) -> Unit,
    onUpdateCantidad: (String, Int) -> Unit,
    currencyFormat: java.text.NumberFormat,
    onNext: (() -> Unit)?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Productos (${cart.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = search, onValueChange = onSearchChange,
                    placeholder = { Text("Buscar productos...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Escanear código",
                            tint = PrimaryBlue,
                            modifier = Modifier.clickable { }
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
            items(productos, key = { it.id }) { producto ->
                val inCart = cart.find { it.producto.id == producto.id }
                val cartCantidad = inCart?.cantidad ?: 0
                val hasStock = producto.stock > 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (inCart != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    producto.id.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (producto.nombre.isNotBlank()) {
                                    Text(
                                        producto.nombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    currencyFormat.format(producto.precioUnitario),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (hasStock) GreenSuccess else MaterialTheme.colorScheme.error)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (hasStock) "Stock: ${producto.stock}" else "Sin stock",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasStock) GreenSuccess else MaterialTheme.colorScheme.error
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (cartCantidad <= 1) onRemove(producto)
                                        else onUpdateCantidad(producto.id, cartCantidad - 1)
                                    },
                                    enabled = cartCantidad > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Quitar",
                                        tint = if (cartCantidad > 0) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    "$cartCantidad",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.width(24.dp),
                                    textAlign = TextAlign.Center
                                )
                                IconButton(
                                    onClick = { onAdd(producto) },
                                    enabled = hasStock && cartCantidad < producto.stock,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Agregar",
                                        tint = if (hasStock && cartCantidad < producto.stock) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }

        if (onNext != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${cart.size} ${if (cart.size == 1) "producto" else "productos"} seleccionados",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Continuar", fontWeight = FontWeight.SemiBold)

                    }
                }
            }
        }
    }
}

@Composable
private fun StepSummary(
    cliente: ClienteEntity,
    cart: List<OrderLineItem>,
    currencyFormat: java.text.NumberFormat,
    nextOrderNumber: String,
    onPlaceOrder: () -> Unit,
    onReject: () -> Unit,
    isSaving: Boolean
) {
    val total = cart.sumOf { it.cantidad * it.producto.precioUnitario }
    val todayStr = remember {
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("es", "MX")).format(java.util.Date())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Logotipo y Cabecera de la Orden
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Logotipo Corporativo
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo Omega",
                    modifier = Modifier.size(120.dp, 60.dp),
                    contentScale = ContentScale.Fit
                )
                
                // Fecha de Emisión
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "FECHA DE EMISIÓN",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        todayStr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Orden de Compra",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                "PO #$nextOrderNumber",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
        }

        // Datos del Emisor y Proveedor
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Emisor
                Column {
                    Text(
                        "EMISOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "REPRESENTACIONES OMEGA CERAMIC, C.A.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text("RIF: J-40101363-5", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                    Text("Telf: +58 412-5626147", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                }

                // Proveedor
                Column {
                    Text(
                        "PROVEEDOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        cliente.nombre,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (cliente.rif.isNotBlank()) {
                        Text("RIF: ${cliente.rif}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                    }
                    if (cliente.direccion.isNotBlank()) {
                        Text(cliente.direccion, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                    } else {
                        Text("Dirección no registrada", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    }
                    if (cliente.telefono.isNotBlank()) {
                        Text("Telf: ${cliente.telefono}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                    }
                    if (cliente.email.isNotBlank()) {
                        Text("Email: ${cliente.email}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
        }

        // Tabla de ítems
        item {
            Text(
                "DETALLE DE ÍTEMS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Cabecera de la tabla
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F4F6))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ÍTEM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(2f))
                Text("CANT.", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("PRECIO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("TOTAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
            }
            
            // Cuerpo de la tabla
            cart.forEach { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(2f)) {
                        Text(
                            line.producto.id.uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        if (line.producto.nombre.isNotBlank()) {
                            Text(
                                line.producto.nombre,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Text(
                        "${line.cantidad}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                    Text(
                        currencyFormat.format(line.producto.precioUnitario),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        color = Color.Black
                    )
                    Text(
                        currencyFormat.format(line.cantidad * line.producto.precioUnitario),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.2f),
                        textAlign = TextAlign.End,
                        color = Color.Black
                    )
                }
                HorizontalDivider(color = Color(0xFFEEEEEE))
            }
        }

        // Bloque de Totales
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(currencyFormat.format(total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color.Black)
                    }
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "MONTO TOTAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Total a Pagar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                        Text(
                            currencyFormat.format(total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }
            }
        }

        // Acciones: Rechazar o Aprobar Orden
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                ) {
                    Text("Rechazar", color = Color.DarkGray, fontWeight = FontWeight.Medium)
                }
                
                Button(
                    onClick = onPlaceOrder,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    if (isSaving) {
                        Text("Aprobando...", color = Color.White)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Aprobar Orden", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
