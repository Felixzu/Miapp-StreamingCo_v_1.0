package com.softidea.StreamingCo

import ClienteAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PagosActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelperInicio

    private lateinit var adapterVencidos: ClienteAdapter
    private lateinit var adapterHoy: ClienteAdapter
    private lateinit var adapterProximos: ClienteAdapter

    private lateinit var listaVencidos: MutableList<Cliente>
    private lateinit var listaHoy: MutableList<Cliente>
    private lateinit var listaProximos: MutableList<Cliente>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pagos)

        val btnHoy = findViewById<Button>(R.id.btnHoy)
        val btnVencidos = findViewById<Button>(R.id.btnVencidos)
        val btnProximos = findViewById<Button>(R.id.btnProximos)





        btnHoy.setOnClickListener {
            startActivity(Intent(this, PagosHoyActivity::class.java))
        }
        btnVencidos.setOnClickListener {
            startActivity(Intent(this, VencidosActivity::class.java))
        }
        btnProximos.setOnClickListener {
            startActivity(Intent(this, ProximosPagosActivity::class.java))
        }


        dbHelper = DatabaseHelperInicio(this)

    }




    private fun configurarRecycler(recyclerId: Int, lista: MutableList<Cliente>, tipo: String) {
        val recycler = findViewById<RecyclerView>(recyclerId)
        recycler.layoutManager = LinearLayoutManager(this)

        val adapter = ClienteAdapter(
            lista = lista,
            onEliminarClick = { cliente ->
                mostrarDialogoEliminacion(cliente, lista, recycler)
            },
            onRenovarClick = { cliente ->
                renovarYReubicarCliente(cliente)
            }
        )

        recycler.adapter = adapter

        when (tipo) {
            "vencidos" -> adapterVencidos = adapter
            "hoy" -> adapterHoy = adapter
            "proximos" -> adapterProximos = adapter
        }
    }

    private fun renovarYReubicarCliente(cliente: Cliente) {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val nuevaCompra = formato.format(calendar.time)

        calendar.add(Calendar.DAY_OF_MONTH, 30)
        val nuevoVencimiento = formato.format(calendar.time)

        cliente.fechaCompra = nuevaCompra
        cliente.fechaVencimiento = nuevoVencimiento

        dbHelper.actualizarFechasCliente(cliente.id, nuevaCompra, nuevoVencimiento)

        // Eliminar de todas las listas
        listaVencidos.remove(cliente)
        listaHoy.remove(cliente)
        listaProximos.remove(cliente)

        // Clasificarlo nuevamente según la nueva fecha
        val fechaVenc = formato.parse(nuevoVencimiento)!!
        val hoy = formato.parse(formato.format(Calendar.getInstance().time))!!
        val tresDias = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }
        val fecha3Dias = formato.parse(formato.format(tresDias.time))!!

        when {
            fechaVenc.before(hoy) -> listaVencidos.add(cliente)
            fechaVenc == hoy -> listaHoy.add(cliente)
            fechaVenc.after(hoy) && !fechaVenc.after(fecha3Dias) -> listaProximos.add(cliente)
            // Si no entra en ninguna, no lo mostramos
        }

        adapterVencidos.notifyDataSetChanged()
        adapterHoy.notifyDataSetChanged()
        adapterProximos.notifyDataSetChanged()

        Toast.makeText(this, "${cliente.nombre} ha sido renovado", Toast.LENGTH_SHORT).show()
    }



    private fun mostrarDialogoEliminacion(cliente: Cliente, lista: MutableList<Cliente>, recycler: RecyclerView) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar cliente?")
            .setMessage("¿Deseas eliminar a ${cliente.nombre}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                val filas = dbHelper.eliminarClientePorId(cliente.id)
                if (filas > 0) {
                    lista.remove(cliente)  // 👈 Aquí eliminamos de la lista visible
                    recycler.adapter?.notifyDataSetChanged() // 👈 Y actualizamos la vista
                    Toast.makeText(this, "${cliente.nombre} eliminado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No se pudo eliminar", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun renovarCliente(cliente: Cliente, lista: MutableList<Cliente>, recycler: RecyclerView) {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val nuevaFechaCompra = formato.format(calendar.time)

        calendar.add(Calendar.DAY_OF_MONTH, 30)
        val nuevaFechaVencimiento = formato.format(calendar.time)

        // Actualizamos las fechas en el objeto
        cliente.fechaCompra = nuevaFechaCompra
        cliente.fechaVencimiento = nuevaFechaVencimiento

        // Actualizamos en la base de datos
        dbHelper.actualizarFechasCliente(cliente.id, nuevaFechaCompra, nuevaFechaVencimiento)

        // 1️⃣ Quitarlo de la lista actual
        lista.remove(cliente)
        recycler.adapter?.notifyDataSetChanged()

        // 2️⃣ Calcular nueva categoría (hoy, próximos o más adelante)
        val nuevaFechaDate = formato.parse(nuevaFechaVencimiento)
        val fechaHoy = formato.parse(formato.format(Calendar.getInstance().time))!!
        val fecha3Dias = formato.parse(
            formato.format(Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 3)
            }.time)
        )!!

        val nuevoEstado = when {
            nuevaFechaDate!!.before(fechaHoy) -> "vencido"
            nuevaFechaDate == fechaHoy -> "hoy"
            nuevaFechaDate.after(fechaHoy) && !nuevaFechaDate.after(fecha3Dias) -> "proximos"
            else -> "otro"
        }

        // 3️⃣ Mostrar mensaje al usuario
        Toast.makeText(this, "${cliente.nombre} renovado. Nuevo vencimiento: $nuevaFechaVencimiento", Toast.LENGTH_SHORT).show()

        // 4️⃣ Opcional: podrías notificar a otra Activity que lo agregue si manejas múltiples secciones
    }





}

