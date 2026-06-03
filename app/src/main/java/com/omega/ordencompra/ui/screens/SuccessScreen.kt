package com.omega.ordencompra.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.ui.theme.GreenSuccess
import com.omega.ordencompra.util.PdfGenerator
import com.omega.ordencompra.viewmodel.OrdenViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SuccessScreen(
    viewModel: OrdenViewModel,
    ordenId: String?,
    onGoHome: () -> Unit
) {
    val ordenes by viewModel.ordenes.collectAsState()
    val context = LocalContext.current
    val orden = ordenes.firstOrNull { it.id == ordenId }
    val productos by viewModel.currentProductos.collectAsState()
    val clientes by viewModel.clientes.collectAsState()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    LaunchedEffect(ordenId) {
        ordenId?.let { viewModel.loadProductos(it) }
    }

    val sharePdf: (String?) -> Unit = { packageName ->
        if (orden != null) {
            val cliente = clientes.firstOrNull { it.id == orden.clienteId }
            val uri = PdfGenerator.generateOrdenPdf(context, orden, productos, cliente)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (packageName != null) {
                    setPackage(packageName)
                }
            }
            try {
                context.startActivity(if (packageName == null) Intent.createChooser(intent, "Compartir orden de compra") else intent)
            } catch (e: Exception) {
                if (packageName != null) {
                    // Fallback to standard chooser
                    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(fallbackIntent, "Compartir orden de compra"))
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(80.dp), tint = GreenSuccess)
        Spacer(modifier = Modifier.height(16.dp))
        Text("¡Pedido Creado!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = GreenSuccess)
        Spacer(modifier = Modifier.height(8.dp))
        Text("El pedido se ha registrado exitosamente.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        if (orden != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Orden N.º", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("OC-${orden.numeroOrden}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Monto Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(currencyFormat.format(orden.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    val cliente = clientes.firstOrNull { it.id == orden.clienteId }
                    val uri = PdfGenerator.generateOrdenPdf(context, orden, productos, cliente)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Descargar PDF", fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { sharePdf("com.whatsapp") },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(
                    onClick = { sharePdf(null) },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Correo / Más", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ir al Dashboard", fontWeight = FontWeight.Medium)
        }
    }
}

