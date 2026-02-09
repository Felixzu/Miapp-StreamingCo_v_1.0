package com.softidea.StreamingCo

data class Cliente(
    var id: Int = 0,
    var nombre: String = "",
    var correo: String = "",
    var contrasena: String = "",
    var fechaCompra: String = "",
    var fechaVencimiento: String = "",
    var precio: Double = 0.0,
    var tipoCuenta: String = "",
    var observacion: String = "",
    var plataforma: String = "",
    var telefono: String = "",
    var documentId: String = "" // 🔥 ID de Firestore
) {
    // Constructor vacío requerido por Firestore
    constructor() : this(0, "", "", "", "", "", 0.0, "", "", "", "", "")
}





