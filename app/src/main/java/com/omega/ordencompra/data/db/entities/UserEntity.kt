package com.omega.ordencompra.data.db.entities

data class UserEntity(
    val id: String = "",
    val username: String = "",
    val password: String = "",
    val rol: String = "usuario",
    val nombreCompleto: String = ""
)
