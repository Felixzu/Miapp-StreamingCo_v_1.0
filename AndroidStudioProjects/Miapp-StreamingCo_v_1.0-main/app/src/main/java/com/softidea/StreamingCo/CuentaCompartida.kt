package com.softidea.StreamingCo

data class CuentaCompartida(
    val correo: String = "",
    val contrasena: String = "",
    val plataforma: String = "",
    val tipo: String = "",
    val clientes: List<Cliente> = listOf(),
    var documentId: String = "" // <--- Este ID debe ser el mismo que el de Firestore
)
