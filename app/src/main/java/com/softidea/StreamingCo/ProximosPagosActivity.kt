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
import java.util.Calendar
import java.util.Locale

class ProximosPagosActivity : AppCompatActivity() {

    private lateinit var listaProximos: MutableList<Cliente>
    private lateinit var adapter: ClienteAdapter
    private val db = FirebaseFirestore.getInstance()
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proximos_pagos)

        val btnWhatsApp = findViewById<Button>(R.id.btnEnviarWhatsApp)
        val recycler = findViewById<RecyclerView>(R.id.recyclerProximos)
        recycler.layoutManager = LinearLayoutManager(this)

        listaProximos = mutableListOf()

        adapter = ClienteAdapter(
            lista = listaProximos,
            onEliminarClick = { cliente ->
                mostrarDialogoEliminacion(cliente)
            },
            onRenovarClick = { cliente ->
                renovarCliente(cliente)
            }
        )

        recycler.adapter = adapter

        btnWhatsApp.setOnClickListener {
            enviarMensajesMasivos(listaProximos)
        }

        cargarClientesProximos()
    }

    private fun cargarClientesProximos() {
        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                listaProximos.clear()
                val hoy = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                for (doc in result) {
                    val cliente = doc.toObject(Cliente::class.java)
                    cliente.documentId = doc.id

                    try {
                        val fechaVenc = formatoFecha.parse(cliente.fechaVencimiento)
                        if (fechaVenc != null) {
                            val fechaVencCal = Calendar.getInstance().apply {
                                time = fechaVenc
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val diffMillis = fechaVencCal.timeInMillis - hoy.timeInMillis
                            val diasRestantes = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                            if (diasRestantes in 1..2) {
                                listaProximos.add(cliente)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
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


    private fun mostrarDialogoEliminacion(cliente: Cliente) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar cliente?")
            .setMessage("¿Deseas eliminar a ${cliente.nombre}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("clientes")
                    .document(cliente.documentId)
                    .delete()
                    .addOnSuccessListener {
                        listaProximos.remove(cliente)
                        adapter.notifyDataSetChanged()
                        Toast.makeText(this, "Eliminado", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun renovarCliente(cliente: Cliente) {
        val calendar = Calendar.getInstance()
        val nuevaFechaCompra = formatoFecha.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 30)
        val nuevaFechaVencimiento = formatoFecha.format(calendar.time)

        cliente.fechaCompra = nuevaFechaCompra
        cliente.fechaVencimiento = nuevaFechaVencimiento

        db.collection("clientes")
            .document(cliente.documentId)
            .update(
                mapOf(
                    "fechaCompra" to nuevaFechaCompra,
                    "fechaVencimiento" to nuevaFechaVencimiento
                )
            )
            .addOnSuccessListener {
                listaProximos.remove(cliente)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "${cliente.nombre} renovado", Toast.LENGTH_SHORT).show()
            }
    }
}
