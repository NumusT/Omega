package com.omega.ordencompra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.ui.theme.BlueInProcess
import com.omega.ordencompra.ui.theme.GrayCanceled
import com.omega.ordencompra.ui.theme.GreenDelivered
import com.omega.ordencompra.ui.theme.GreenSuccess
import com.omega.ordencompra.ui.theme.OrangeWarning
import com.omega.ordencompra.ui.theme.RedError

// Centralized list of valid order statuses
object OrdenEstados {
    const val PENDIENTE = "Pendiente"
    const val APROBADA = "Aprobada"
    const val EN_PROCESO = "En Proceso"
    const val ENTREGADA = "Entregada"
    const val CANCELADA = "Cancelada"

    val todos = listOf(PENDIENTE, APROBADA, EN_PROCESO, ENTREGADA, CANCELADA)
}

@Composable
fun EstadoBadge(estado: String) {
    val (bg, fg) = when (estado.lowercase()) {
        "pendiente" -> OrangeWarning to Color.White
        "aprobada" -> GreenSuccess to Color.White
        "en proceso" -> BlueInProcess to Color.White
        "entregada" -> GreenDelivered to Color.White
        "cancelada" -> GrayCanceled to Color.White
        "rechazada", "sin stock" -> RedError to Color.White
        "en stock" -> GreenSuccess to Color.White
        "stock bajo" -> OrangeWarning to Color.White
        "activo" -> GreenSuccess to Color.White
        "nuevo" -> BlueInProcess to Color.White
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Text(
        text = estado,
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

