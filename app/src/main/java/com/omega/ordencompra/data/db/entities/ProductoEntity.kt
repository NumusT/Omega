package com.omega.ordencompra.data.db.entities

data class ProductoEntity(
    val id: String = "",
    val ordenId: String = "",
    val productoCatalogoId: String = "",
    val nombre: String = "",
    val cantidad: Int = 0,
    val precioUnitario: Double = 0.0,
    val total: Double = 0.0
)
