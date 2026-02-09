import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.softidea.StreamingCo.Cliente

object FirebaseSyncHelper {
    fun subirClienteAFirebase(clientes: List<Cliente>) {
        val fireDb = FirebaseFirestore.getInstance()


        for (cliente in clientes) {
            val clienteMap = hashMapOf(
                "nombre" to cliente.nombre,
                "correo" to cliente.correo,
                "contrasena" to cliente.contrasena,
                "fecha_compra" to cliente.fechaCompra,
                "fecha_vencimiento" to cliente.fechaVencimiento,
                "precio" to cliente.precio,
                "tipo_cuenta" to cliente.tipoCuenta,
                "observaciones" to cliente.observacion,
                "plataforma" to cliente.plataforma,
                "telefono" to cliente.telefono
            )

            // Asegúrate de que este ID sea siempre el mismo para el mismo cliente
            val idDocumento = "${cliente.correo}_${cliente.plataforma}_${cliente.contrasena}".replace(".", "_")
            Log.d("FirebaseSync", "Subiendo cliente con ID: $idDocumento")

            fireDb.collection("clientes")
                .document(idDocumento)
                .set(clienteMap)
                .addOnSuccessListener {
                    Log.d("FirebaseSync", "Cliente actualizado/subido: $idDocumento")
                }
                .addOnFailureListener {
                    Log.e("FirebaseSync", "Error al subir cliente: $idDocumento", it)
                }
        }
    }

    fun eliminarClienteDeCuentaCompartida(
        correo: String,
        contrasena: String,
        plataforma: String,
        nombreCliente: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val idDocumento = "${correo}_${plataforma}_${contrasena}".replace(".", "_")

        val docRef = db.collection("cuentas_compartidas").document(idDocumento)

        docRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val listaClientes = documentSnapshot.get("clientes") as? List<Map<String, Any>>
                if (listaClientes != null) {
                    val nuevaLista = listaClientes.filter {
                        it["nombre"] != nombreCliente
                    }

                    if (nuevaLista.isEmpty()) {
                        // Si ya no quedan clientes, elimina todo el documento
                        docRef.delete().addOnSuccessListener {
                            Log.d("Firestore", "Documento de cuenta compartida eliminado completamente.")
                        }.addOnFailureListener {
                            Log.e("Firestore", "Error al eliminar documento de cuenta compartida", it)
                        }
                    } else {
                        // Si quedan otros clientes, actualiza el array
                        docRef.update("clientes", nuevaLista).addOnSuccessListener {
                            Log.d("Firestore", "Cliente eliminado del array de cuenta compartida.")
                        }.addOnFailureListener {
                            Log.e("Firestore", "Error al actualizar cuenta compartida", it)
                        }
                    }
                }
            }
        }.addOnFailureListener {
            Log.e("Firestore", "Error al obtener documento de cuenta compartida", it)
        }
    }



}

