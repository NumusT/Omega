package com.omega.ordencompra.data.db.entities

data class OrdenEntity(
    val id: String = "",
    val numeroOrden: String = "",
    val clienteId: String = "",
    val clienteNombre: String = "",
    val usuarioId: String = "",
    val fecha: String = "",
    val total: Double = 0.0,
    val estado: String = "Pendiente",
    val observaciones: String = ""
)
