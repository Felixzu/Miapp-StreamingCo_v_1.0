package com.softidea.StreamingCo

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore



class CuentaCompartidaAdapter(
    private val context: Context,
    private var cuentas: List<CuentaCompartida>,
    private val dbHelper: DatabaseHelperInicio,
    private val onEditarCuenta: (CuentaCompartida) -> Unit,
    private val onEliminarCuenta: (CuentaCompartida) -> Unit,
    private val onEliminarCliente: (Cliente) -> Unit,
    private val onRenovarCliente: (Cliente) -> Unit,
    private val onEditarCliente: (Cliente) -> Unit,
    private var cuentasFiltradas: List<CuentaCompartida> = cuentas,




) : RecyclerView.Adapter<CuentaCompartidaAdapter.ViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    private val iconosPorPlataforma = mapOf(
        "netflix" to R.drawable.ic_netflix,
        "hbo" to R.drawable.ic_hbo,
        "disney+" to R.drawable.ic_disney,
        "disney+ con espn" to R.drawable.ic_disneyespn,
        "spotify" to R.drawable.ic_spotify,
        "amazon prime" to R.drawable.ic_prime,
        "flujotv" to R.drawable.ic_flujotv,
        "iptvsmarters" to R.drawable.ic_iptvsmarters,
        "youtubepremium" to R.drawable.ic_yt,
        "dgo" to R.drawable.ic_dgo
    )

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnDisponibilidadPantallas: Button = view.findViewById(R.id.btnDisponibilidadPantallas)

        val btnAgregarCliente: ImageButton = view.findViewById(R.id.btnAgregarCliente)
        val tvCuenta: TextView = view.findViewById(R.id.tvCuenta)
        val contenedorClientes: LinearLayout = view.findViewById(R.id.contenedorClientes)
        val btnEditar: ImageButton = view.findViewById(R.id.btnEditarCuenta)
        val btnRenovar: ImageButton = view.findViewById(R.id.btnRenovarCuenta)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminarCuenta)
        val btnVerPantallas: ImageButton = view.findViewById(R.id.btnVerPantallas)
        val iconoPlataforma: ImageView = view.findViewById(R.id.iconoPlataforma)
        val btnCopiarCorreo = view.findViewById<ImageButton>(R.id.btnCopiarCorreo)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(R.layout.item_cuenta_compartida, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = cuentasFiltradas.size


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cuenta = cuentasFiltradas[position]

        holder.btnAgregarCliente.setOnClickListener {

            val esCuentaCompleta =
                cuenta.clientes.firstOrNull()?.tipoCuenta.equals("Completa", true)

            if (esCuentaCompleta) {
                Toast.makeText(
                    context,
                    "❌ Esta cuenta es COMPLETA, no se pueden agregar pantallas",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val intent = android.content.Intent(context, AgregarClienteActivity::class.java)
            intent.putExtra("correo", cuenta.correo)
            intent.putExtra("contrasena", cuenta.contrasena)
            intent.putExtra("plataforma", cuenta.plataforma)

            context.startActivity(intent)
        }




        val plataforma = cuenta.plataforma
        val correo = cuenta.correo
        val contrasena = cuenta.contrasena


        // Asignar ícono usando el mapa
        val iconoResId = iconosPorPlataforma[cuenta.plataforma.trim().lowercase()]
            ?: R.drawable.ic_plataforma_generico
        holder.iconoPlataforma.setImageResource(iconoResId)




        // Mostrar correo y contraseña
        holder.tvCuenta.text = "📩 ${cuenta.correo} | 🔒 ${cuenta.contrasena}"

        // Mostrar clientes
        holder.contenedorClientes.removeAllViews()
        cuenta.clientes.forEachIndexed { index, cliente ->
            val tv = TextView(context)
            val numeroPerfil = index + 1
            tv.text = "- ${cliente.nombre} (Perfil $numeroPerfil)"
            tv.setTextColor(context.getColor(android.R.color.darker_gray))
            tv.textSize = 14f
            holder.contenedorClientes.addView(tv)
        }

        holder.btnCopiarCorreo.setOnClickListener {
            copiarAlPortapapeles(context, cuenta.correo, cuenta.contrasena,cuenta.plataforma,"Datos copiados")
        }




        holder.btnEliminar.setOnClickListener {
            mostrarDialogoEliminarCuenta(cuenta)
        }

        holder.btnRenovar.setOnClickListener {
            renovarCuenta(cuenta)
            guardarCuentaCompartidaEnFirestore(cuenta)

        }

        holder.btnEditar.setOnClickListener {
            mostrarDialogoEditarCuenta(cuenta)
        }

        holder.btnVerPantallas.setOnClickListener {
            if (holder.contenedorClientes.visibility == View.VISIBLE) {
                holder.contenedorClientes.visibility = View.GONE
            } else {
                holder.contenedorClientes.removeAllViews()
                for ((index, cliente) in cuenta.clientes.withIndex()) {
                    val itemView = LayoutInflater.from(context).inflate(
                        R.layout.item_pantalla_individual,
                        holder.contenedorClientes,
                        false
                    )

                    val tvNombrePantalla = itemView.findViewById<TextView>(R.id.tvNombrePantalla)
                    val btnEditarPantalla = itemView.findViewById<ImageButton>(R.id.btnEditarPantalla)
                    val btnEliminarPantalla = itemView.findViewById<ImageButton>(R.id.btnEliminarPantalla)
                    val btnRenovarPantalla = itemView.findViewById<ImageButton>(R.id.btnRenovarPantalla)

                    tvNombrePantalla.text = "${cliente.nombre} (Perfil ${index + 1})"

                    btnEditarPantalla.setOnClickListener {
                        val dialogView = LayoutInflater.from(context).inflate(
                            R.layout.dialog_editar_nombre,
                            null
                        )
                        val edtNuevoNombre = dialogView.findViewById<EditText>(R.id.edtNuevoNombre)
                        val edtNuevaObservacion = dialogView.findViewById<EditText>(R.id.edtNuevaObservacion)

                        edtNuevoNombre.setText(cliente.nombre)
                        edtNuevaObservacion.setText(cliente.observacion)

                        AlertDialog.Builder(context)
                            .setTitle("Editar cliente")
                            .setView(dialogView)
                            .setPositiveButton("Guardar") { _, _ ->
                                val nuevoNombre = edtNuevoNombre.text.toString().trim()
                                val nuevaObs = edtNuevaObservacion.text.toString().trim()

                                if (nuevoNombre.isNotEmpty()) {
                                    cliente.nombre = nuevoNombre
                                    cliente.observacion = nuevaObs
                                    dbHelper.actualizarNombreYObservacion(cliente.id, nuevoNombre, nuevaObs)
                                    actualizarClienteEnFirestore(cliente)

                                    actualizarLista(dbHelper.obtenerCuentasAgrupadas())

                                    for (cuenta in cuentas) {
                                        for (cliente in cuenta.clientes) {
                                            if (cliente.documentId.isBlank()) {
                                                cliente.documentId = generarDocumentIdUnico(cliente)
                                            }
                                        }
                                    }


                                    Toast.makeText(context, "Cliente actualizado", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }

                    btnEliminarPantalla.setOnClickListener {
                        AlertDialog.Builder(context)
                            .setTitle("¿Eliminar pantalla?")
                            .setMessage("¿Deseas eliminar a ${cliente.nombre}?")
                            .setPositiveButton("Eliminar") { _, _ ->
                                dbHelper.eliminarClientePorId(cliente.id)

                                if (cliente.documentId.isBlank()) {
                                    cliente.documentId = generarDocumentIdUnico(cliente)
                                }

                                eliminarClienteDeFirestore(cliente.documentId)

                                // 🔥 Aquí llamamos la función para quitarlo del array de cuentas compartidas en Firestore
                                eliminarClienteDeCuentaCompartida(
                                    cliente.correo,
                                    cliente.contrasena,
                                    cliente.plataforma,
                                    cliente.nombre
                                )

                                actualizarLista(dbHelper.obtenerCuentasAgrupadas())
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }


                    btnRenovarPantalla.setOnClickListener { onRenovarCliente(cliente) }

                    holder.contenedorClientes.addView(itemView)
                }

                holder.contenedorClientes.visibility = View.VISIBLE
            }
        }

        val clientesActuales = cuenta.clientes.size
        val limitePantallas = obtenerLimitePantallas(cuenta.plataforma)

        holder.btnDisponibilidadPantallas.apply {
            if (clientesActuales < limitePantallas) {
                text = "Disponible (${clientesActuales}/${limitePantallas})"
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            } else {
                text = "Sin espacio (${clientesActuales}/${limitePantallas})"
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            }
        }



    }
    fun copiarAlPortapapeles(context: Context, correo: String, contrasena: String, plataforma:String, mensaje: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val textoCopiado = "Correo: $correo\nContraseña: $contrasena\nPlataforma: $plataforma"
        val clip = android.content.ClipData.newPlainText("datos_cuenta", textoCopiado)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
    }





    private fun mostrarDialogoEliminarCuenta(cuenta: CuentaCompartida) {
        val cuentaId = generarDocumentIdUnicoDesdeCuenta(cuenta)
        eliminarCuentaCompartidaDeFirestore(cuentaId)

        AlertDialog.Builder(context)
            .setTitle("¿Eliminar cuenta?")
            .setMessage("¿Deseas eliminar la cuenta de ${cuenta.correo}? Se eliminarán todos sus clientes.")
            .setPositiveButton("Eliminar") { _, _ ->
                cuenta.clientes.forEach { cliente ->
                    dbHelper.eliminarClientePorId(cliente.id)

                    if (cliente.documentId.isBlank()) {
                        cliente.documentId = generarDocumentIdUnico(cliente)
                    }
                    eliminarClienteDeFirestore(cliente.documentId)
                }

                // 🔥 Eliminar también la cuenta compartida en Firestore
                Log.d("EliminarCuentaDebug", "ID que se intenta eliminar: $cuentaId")

                eliminarCuentaCompartidaDeFirestore(cuentaId)

                actualizarLista(dbHelper.obtenerCuentasAgrupadas())
                Toast.makeText(context, "Cuenta eliminada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    fun generarDocumentIdUnicoDesdeCuenta(cuenta: CuentaCompartida): String {
        return "${cuenta.correo.trim().lowercase()}_${cuenta.contrasena.trim()}_${cuenta.plataforma.trim().lowercase()}"
            .replace(".", "_")
            .replace("/", "_")
            .replace("#", "_")
            .replace(" ", "_")
    }





    private fun renovarCuenta(cuenta: CuentaCompartida) {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val nuevaCompra = formato.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 30)
        val nuevoVencimiento = formato.format(calendar.time)

        cuenta.clientes.forEach {
            it.fechaCompra = nuevaCompra
            it.fechaVencimiento = nuevoVencimiento
            dbHelper.actualizarFechasCliente(it.id, nuevaCompra, nuevoVencimiento)
            actualizarClienteEnFirestore(it)
        }

        actualizarLista(dbHelper.obtenerCuentasAgrupadas())
        Toast.makeText(context, "Cuenta renovada", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarDialogoEditarCuenta(cuenta: CuentaCompartida) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_editar_cliente, null)
        val edtCorreo = view.findViewById<EditText>(R.id.edtNuevoCorreo)
        val edtContrasena = view.findViewById<EditText>(R.id.edtNuevaContrasena)
        val edtObservacion = view.findViewById<EditText>(R.id.edtObservaciones)

        edtCorreo.setText(cuenta.correo)
        edtContrasena.setText(cuenta.contrasena)
        edtObservacion.setText(cuenta.clientes.firstOrNull()?.observacion ?: "")

        AlertDialog.Builder(context)
            .setTitle("Editar cuenta")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoCorreo = edtCorreo.text.toString()
                val nuevaContrasena = edtContrasena.text.toString()
                val nuevaObs = edtObservacion.text.toString()

                if (nuevoCorreo.isNotBlank() && nuevaContrasena.isNotBlank()) {
                    cuenta.clientes.forEach {
                        // Actualizar los valores del cliente en memoria
                        it.correo = nuevoCorreo
                        it.contrasena = nuevaContrasena
                        it.observacion = nuevaObs

                        // Actualizar en SQLite
                        dbHelper.actualizarCorreoContrasenaObservacion(
                            it.id, nuevoCorreo, nuevaContrasena, nuevaObs
                        )

                        // Actualizar en Firebase
                        actualizarClienteEnFirestore(it)
                    }

                    actualizarLista(dbHelper.obtenerCuentasAgrupadas())


                    Toast.makeText(context, "Cuenta actualizada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Campos vacíos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarCuentaCompartidaEnFirestore(cliente: Cliente) {
        val db = FirebaseFirestore.getInstance()

        if (cliente.documentId.isNullOrBlank()) {
            Log.e("FirestoreEditar", "No se puede actualizar: documentId vacío para cliente ${cliente.nombre}")
            return
        }

        db.collection("clientes").document(cliente.documentId)
            .update(
                "correo", cliente.correo,
                "contrasena", cliente.contrasena,
                "observacion", cliente.observacion
            )
            .addOnSuccessListener {
                Log.d("FirestoreEditar", "Cliente actualizado correctamente en Firestore: ${cliente.documentId}")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreEditar", "Error al actualizar el cliente ${cliente.documentId}", e)
            }
    }



    fun actualizarLista(nuevaLista: List<CuentaCompartida>) {
        cuentas = nuevaLista
        cuentasFiltradas = nuevaLista
        notifyDataSetChanged()
    }

    fun filtrar(texto: String) {
        val textoLower = texto.lowercase()

        cuentasFiltradas = if (textoLower.isBlank()) {
            cuentas
        } else {
            cuentas.filter { cuenta ->
                val coincideCorreo = cuenta.correo.lowercase().contains(textoLower)
                val coincidePlataforma = cuenta.plataforma.lowercase().contains(textoLower)
                val coincideEnClientes = cuenta.clientes.any {
                    it.nombre.lowercase().contains(textoLower)
                }

                coincideCorreo || coincidePlataforma || coincideEnClientes
            }
        }

        notifyDataSetChanged()
    }

    private fun obtenerLimitePantallas(plataforma: String): Int {
        return when (plataforma.uppercase(Locale.getDefault())) {
            "NETFLIX" -> 5
            "HBO" -> 5
            "DISNEY+" -> 7
            "DISNEY+" -> 7
            "DISNEY+ CON ESPN" -> 7
            "SPOTIFY" -> 1
            "AMAZON PRIME" -> 6
            "FLUJO TV" -> 4
            "IPTVSMARTERS" -> 4
            "YOUTUBEPREMIUM" -> 1
            "DGO" -> 1
            else -> 4
        }
    }


    fun actualizarClienteEnFirestore(cliente: Cliente) {
        val db = FirebaseFirestore.getInstance()
        val documentRef = db.collection("clientes").document(cliente.documentId)

        val datosActualizados = mapOf(
            "nombre" to cliente.nombre,
            "correo" to cliente.correo,
            "contrasena" to cliente.contrasena,
            "fechaCompra" to cliente.fechaCompra,
            "fechaVencimiento" to cliente.fechaVencimiento,
            "tipo" to cliente.tipoCuenta,
            "observacion" to cliente.observacion,
            "plataforma" to cliente.plataforma
        )

        documentRef.update(datosActualizados)
            .addOnSuccessListener {
                Log.d("Firestore", "Cliente actualizado correctamente.")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al actualizar cliente", e)
            }
    }




    private fun eliminarClienteDeFirestore(documentId: String) {
        Log.d("FirestoreDelete", "Intentando eliminar documento con ID: $documentId")

        if (documentId.isEmpty()) {
            Toast.makeText(context, "Error: documentId vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("clientes").document(documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Eliminado de Firestore", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generarDocumentIdUnico(cliente: Cliente): String {
        return "${cliente.correo}_${cliente.plataforma}_${cliente.id}"
            .replace(".", "_")
            .replace("/", "_")
            .replace("#", "_")
            .replace(" ", "_")
    }

    fun eliminarClienteDeCuentaCompartida(
        correo: String,
        contrasena: String,
        plataforma: String,
        nombreCliente: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val cuentasRef = db.collection("cuentas_compartidas")

        cuentasRef
            .whereEqualTo("correo", correo.trim())
            .whereEqualTo("contrasena", contrasena.trim())
            .whereEqualTo("plataforma", plataforma.trim())
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.w("Firestore", "❌ No se encontró ningún documento para esa cuenta")
                    return@addOnSuccessListener
                }

                for (doc in result) {
                    val clientesArray = doc.get("clientes") as? List<Map<String, Any>> ?: emptyList()

                    val nuevosClientes = clientesArray.filterNot { cliente ->
                        val nombre = cliente["nombre"]?.toString()?.trim()
                        nombre == nombreCliente.trim()
                    }

                    if (nuevosClientes.size < clientesArray.size) {
                        if (nuevosClientes.isEmpty()) {
                            // Si no queda ningún cliente, elimina toda la cuenta
                            doc.reference.delete()
                                .addOnSuccessListener {
                                    Log.d("Firestore", "✅ Cuenta compartida eliminada")
                                }
                                .addOnFailureListener {
                                    Log.e("Firestore", "❌ Error al eliminar cuenta", it)
                                }
                        } else {
                            // Si aún quedan clientes, actualiza la lista
                            doc.reference.update("clientes", nuevosClientes)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "✅ Cliente eliminado del array")
                                }
                                .addOnFailureListener {
                                    Log.e("Firestore", "❌ Error al actualizar array", it)
                                }
                        }
                    } else {
                        Log.w("Firestore", "⚠️ Cliente no encontrado en el array")
                    }
                }
            }
            .addOnFailureListener {
                Log.e("Firestore", "❌ Error al buscar cuenta compartida", it)
            }
    }




    private fun eliminarCuentaCompartidaDeFirestore(documentId: String) {
        val db = FirebaseFirestore.getInstance()

        Log.d("FirestoreEliminar", "Intentando eliminar documento con ID: $documentId")

        db.collection("cuentas_compartidas").document(documentId)
            .delete()
            .addOnSuccessListener {
                Log.d("FirestoreEliminar", "Cuenta eliminada correctamente en Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreEliminar", "Error al eliminar la cuenta: $documentId", e)
            }
    }




    private fun guardarCuentaCompartidaEnFirestore(cuenta: CuentaCompartida) {
        val firestore = FirebaseFirestore.getInstance()

        // Creamos un ID único para el documento
        val cuentaId = generarDocumentIdUnicoDesdeCuenta(cuenta)

        // Convertimos la lista de clientes a un formato Firestore completo
        val clientesFirestore = cuenta.clientes.map { cliente ->
            mapOf(
                "nombre" to cliente.nombre,
                "correo" to cliente.correo,
                "contrasena" to cliente.contrasena,
                "fecha_compra" to cliente.fechaCompra,
                "fecha_vencimiento" to cliente.fechaVencimiento,
                "observaciones" to cliente.observacion,
                "plataforma" to cliente.plataforma,
                "tipo_cuenta" to cliente.tipoCuenta,
                "precio" to cliente.precio,
                "telefono" to cliente.telefono
            )
        }

        val data = hashMapOf(
            "correo" to cuenta.correo,
            "contrasena" to cuenta.contrasena,
            "plataforma" to cuenta.plataforma,
            "clientes" to clientesFirestore
        )

        firestore.collection("cuentas_compartidas").document(cuentaId)
            .set(data)
            .addOnSuccessListener {
                Log.d("Firestore", "Cuenta compartida guardada/actualizada exitosamente")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al guardar cuenta compartida", e)
            }
    }



    private fun editarCuentaCompartidaEnFirestore(
        cuentaViejaId: String,
        cuentaNueva: CuentaCompartida
    ) {
        val db = FirebaseFirestore.getInstance()

        // 1. Eliminar la cuenta antigua (si el ID cambió por correo o plataforma)
        db.collection("cuentas_compartidas").document(cuentaViejaId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Cuenta vieja eliminada correctamente")

                // 2. Guardar la nueva cuenta
                guardarCuentaCompartidaEnFirestore(cuentaNueva)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al eliminar la cuenta vieja", e)
            }
    }

    private fun generarIdCliente(correo: String, contrasena: String, plataforma: String): String {
        return "${correo}_${contrasena}_${plataforma}"
    }









}
