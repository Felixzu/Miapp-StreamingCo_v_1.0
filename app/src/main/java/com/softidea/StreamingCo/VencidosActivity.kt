package com.softidea.StreamingCo

import ClienteAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class VencidosActivity : AppCompatActivity() {

    private lateinit var listaVencidos: MutableList<Cliente>
    private lateinit var adapter: ClienteAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vencidos)

        val btnWhatsApp = findViewById<Button>(R.id.btnEnviarWhatsApp)

        listaVencidos = mutableListOf()

        val recycler = findViewById<RecyclerView>(R.id.recyclerVencidos)
        recycler.layoutManager = LinearLayoutManager(this)

        adapter = ClienteAdapter(
            lista = listaVencidos,
            onEliminarClick = { cliente -> eliminarCliente(cliente) },
            onRenovarClick = { cliente -> renovarCliente(cliente) }
        )

        recycler.adapter = adapter

        btnWhatsApp.setOnClickListener {
            enviarMensajesMasivos(listaVencidos)
        }

        cargarClientesVencidos()
    }

    private fun cargarClientesVencidos() {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val hoy = formato.parse(formato.format(Date()))

        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                listaVencidos.clear()

                for (doc in result) {
                    val cliente = doc.toObject(Cliente::class.java)
                    cliente.documentId = doc.id

                    val fechaVencStr = cliente.fechaVencimiento

                    if (!fechaVencStr.isNullOrBlank()) {
                        try {
                            val fechaVenc = formato.parse(fechaVencStr)
                            if (fechaVenc != null && fechaVenc.before(hoy)) {
                                listaVencidos.add(cliente)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Puedes decidir ignorarlo o reportar el error
                        }
                    }

                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar clientes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun eliminarCliente(cliente: Cliente) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar cliente?")
            .setMessage("¿Deseas eliminar a ${cliente.nombre}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("clientes").document(cliente.documentId)
                    .delete()
                    .addOnSuccessListener {
                        listaVencidos.remove(cliente)
                        adapter.notifyDataSetChanged()
                        Toast.makeText(this, "Eliminado", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun renovarCliente(cliente: Cliente) {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        try {
            // --- FECHA DE COMPRA ---
            // Obtenemos el día original de la fecha de compra del cliente
            val fechaCompraOriginal = formato.parse(cliente.fechaCompra)
            val calendarCompra = Calendar.getInstance()

            if (fechaCompraOriginal != null) {
                // Tomamos el día del cliente
                val diaCompra = Calendar.getInstance().apply {
                    time = fechaCompraOriginal
                }.get(Calendar.DAY_OF_MONTH)

                // Establecemos ese mismo día, pero en el mes y año actuales
                calendarCompra.set(Calendar.DAY_OF_MONTH, diaCompra)
                // (si el mes actual tiene menos días, se ajusta automáticamente al último día válido)
                val nuevaFechaCompra = formato.format(calendarCompra.time)

                // --- FECHA DE VENCIMIENTO ---
                // Partimos de la nueva fecha de compra
                calendarCompra.add(Calendar.MONTH, 1)
                val nuevaFechaVencimiento = formato.format(calendarCompra.time)

                // --- ACTUALIZAMOS EN FIRESTORE ---
                val actualizacion = mapOf(
                    "fechaCompra" to nuevaFechaCompra,
                    "fechaVencimiento" to nuevaFechaVencimiento
                )

                db.collection("clientes").document(cliente.documentId)
                    .update(actualizacion)
                    .addOnSuccessListener {
                        listaVencidos.remove(cliente)
                        adapter.notifyDataSetChanged()
                        Toast.makeText(this, "${cliente.nombre} renovado", Toast.LENGTH_SHORT).show()
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al renovar cliente", Toast.LENGTH_SHORT).show()
        }
    }



    private fun enviarMensajesMasivos(lista: List<Cliente>) {
        if (lista.isEmpty()) {
            Toast.makeText(this, "No hay clientes para enviar mensajes.", Toast.LENGTH_SHORT).show()
            return
        }
        enviarMensajeACliente(0, lista)
    }

    private fun enviarMensajeACliente(indice: Int, clientes: List<Cliente>) {
        if (indice >= clientes.size) {
            Toast.makeText(this, "Todos los mensajes fueron enviados", Toast.LENGTH_SHORT).show()
            return
        }

        val cliente = clientes[indice]
        val numero = cliente.telefono.replace("\\s".toRegex(), "")
        val mensaje = "Buen día 😊 ${cliente.nombre}, recuerda que tu pago vence pronto. ¿Deseas continuar con el servicio?"
        val mensajeCodificado = URLEncoder.encode(mensaje, "UTF-8")

        val uri = Uri.parse("https://api.whatsapp.com/send?phone=57$numero&text=$mensajeCodificado")
        val intent = Intent(Intent.ACTION_VIEW, uri)

        val whatsappBusiness = "com.whatsapp.w4b"
        val whatsappNormal = "com.whatsapp"

        when {
            packageManager.getLaunchIntentForPackage(whatsappBusiness) != null -> {
                intent.setPackage(whatsappBusiness)
            }
            packageManager.getLaunchIntentForPackage(whatsappNormal) != null -> {
                intent.setPackage(whatsappNormal)
            }
            else -> {
                Toast.makeText(this, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
                return
            }
        }

        startActivity(intent)

        AlertDialog.Builder(this)
            .setTitle("Mensaje enviado")
            .setMessage("¿Deseas enviar mensaje al siguiente cliente?")
            .setPositiveButton("Sí") { _, _ -> enviarMensajeACliente(indice + 1, clientes) }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
