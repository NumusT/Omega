package com.omega.ordencompra.data.db.entities

data class HistorialEntity(
    val id: String = "",
    val ordenId: String = "",
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    val accion: String = "",
    val detalle: String = "",
    val fecha: String = ""
)
