package com.softidea.StreamingCo

import ClienteAdapter
import android.content.Intent
import android.content.pm.PackageManager
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

class PagosHoyActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var listaHoy: MutableList<Cliente>
    private lateinit var adapter: ClienteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pagos_hoy)

        val btnWhatsApp = findViewById<Button>(R.id.btnEnviarWhatsApp)
        db = FirebaseFirestore.getInstance()
        listaHoy = mutableListOf()

        val recycler = findViewById<RecyclerView>(R.id.recyclerHoy)
        recycler.layoutManager = LinearLayoutManager(this)

        adapter = ClienteAdapter(
            lista = listaHoy,
            onEliminarClick = { cliente -> mostrarDialogoEliminacion(cliente) },
            onRenovarClick = { cliente -> renovarClienteFirebase(cliente) }
        )
        recycler.adapter = adapter

        btnWhatsApp.setOnClickListener {
            enviarMensajesMasivos()
        }

        cargarClientesDeHoy()
    }

    private fun cargarClientesDeHoy() {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaHoy = formato.format(Calendar.getInstance().time)

        db.collection("clientes")
            .whereEqualTo("fechaVencimiento", fechaHoy)
            .get()
            .addOnSuccessListener { result ->
                listaHoy.clear()
                for (doc in result) {
                    val cliente = doc.toObject(Cliente::class.java)
                    cliente.documentId = doc.id
                    listaHoy.add(cliente)
                }
                adapter.notifyDataSetChanged()

                if (listaHoy.isEmpty()) {
                    Toast.makeText(this, "No hay clientes que venzan hoy", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoEliminacion(cliente: Cliente) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar cliente?")
            .setMessage("¿Estás seguro de eliminar a ${cliente.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("clientes").document(cliente.documentId)
                    .delete()
                    .addOnSuccessListener {
                        listaHoy.remove(cliente)
                        adapter.notifyDataSetChanged()
                        Toast.makeText(this, "Cliente eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun renovarClienteFirebase(cliente: Cliente) {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val nuevaFechaCompra = formato.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 30)
        val nuevaFechaVencimiento = formato.format(calendar.time)

        val actualizaciones = mapOf(
            "fechaCompra" to nuevaFechaCompra,
            "fechaVencimiento" to nuevaFechaVencimiento
        )

        db.collection("clientes").document(cliente.documentId)
            .update(actualizaciones)
            .addOnSuccessListener {
                listaHoy.remove(cliente)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "${cliente.nombre} renovado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al renovar", Toast.LENGTH_SHORT).show()
            }
    }

    fun enviarMensajesMasivos() {
        enviarMensajeACliente(0, listaHoy.toMutableList())
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
