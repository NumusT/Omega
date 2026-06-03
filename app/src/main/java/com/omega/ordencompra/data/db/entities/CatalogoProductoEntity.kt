package com.omega.ordencompra.data.db.entities

data class CatalogoProductoEntity(
    val id: String = "",
    val codigo: String = "",
    val nombre: String = "",
    val stock: Int = 0,
    val precioUnitario: Double = 0.0
)
