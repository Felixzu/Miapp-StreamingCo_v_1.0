package com.softidea.StreamingCo

import ClienteAdapter
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.appcompat.widget.SearchView

import android.content.Intent
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder
import java.util.*


class ListaClientesActivity : AppCompatActivity() {


    private lateinit var dbHelper: DatabaseHelperInicio
    private lateinit var adapter: ClienteAdapter
    private lateinit var listaClientes: MutableList<Cliente>
    private lateinit var listaFiltrada: MutableList<Cliente>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_clientes)

        dbHelper = DatabaseHelperInicio(this)

        listaClientes = mutableListOf()
        listaFiltrada = mutableListOf() // 🔹 Inicializamos la lista filtrada

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerClientes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ClienteAdapter(
            listaFiltrada, // 🔹 El adapter usa la lista filtrada
            onEliminarClick = { cliente -> mostrarDialogoEliminacion(cliente) },
            onRenovarClick = { cliente -> renovarCliente(cliente) },
            onEditarClick = { cliente -> mostrarDialogoEditar(cliente) }
        )

        recyclerView.adapter = adapter

        escucharCambiosEnFirestore()

        val searchView = findViewById<SearchView>(R.id.searchViewClientes)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarClientes(newText ?: "")
                return true
            }
        })
    }



    private fun filtrarClientes(texto: String) {
        val query = texto.lowercase(Locale.getDefault())
        listaFiltrada.clear()

        if (query.isEmpty()) {
            listaFiltrada.addAll(listaClientes)
        } else {
            listaFiltrada.addAll(
                listaClientes.filter {
                    it.nombre.lowercase(Locale.getDefault()).contains(query) ||
                            it.correo.lowercase(Locale.getDefault()).contains(query)
                }
            )
        }
        adapter.notifyDataSetChanged()
    }



    private fun mostrarDialogoEliminacion(cliente: Cliente) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar cliente?")
            .setMessage("¿Deseas eliminar a ${cliente.nombre}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                dbHelper.eliminarClientePorId(cliente.id)
                if (cliente.documentId.isNotEmpty()) {
                    eliminarClienteDeFirestore(cliente.documentId)
                }

                listaClientes.remove(cliente)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarClienteDeFirestore(documentId: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("clientes").document(documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Eliminado también en Firestore", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error eliminando en Firestore: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    private fun renovarCliente(cliente: Cliente) {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val nuevaFechaCompra = formato.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 30)
        val nuevaFechaVencimiento = formato.format(calendar.time)

        cliente.fechaCompra = nuevaFechaCompra
        cliente.fechaVencimiento = nuevaFechaVencimiento

        dbHelper.actualizarFechasCliente(cliente.id, nuevaFechaCompra, nuevaFechaVencimiento)

        listaClientes.remove(cliente) // Lo quitas de la lista visible
        adapter.notifyDataSetChanged()

        Toast.makeText(this, "${cliente.nombre} renovado", Toast.LENGTH_SHORT).show()
        actualizarFechasEnFirestore(cliente)

    }

    private fun actualizarFechasEnFirestore(cliente: Cliente) {
        if (cliente.documentId.isEmpty()) return

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("clientes")
            .document(cliente.documentId)
            .update(
                mapOf(
                    "fecha_compra" to cliente.fechaCompra,
                    "fecha_vencimiento" to cliente.fechaVencimiento
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Fechas actualizadas en Firestore", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al actualizar Firestore: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    private fun escucharCambiosEnFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("clientes")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error al sincronizar", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    listaClientes.clear()
                    for (doc in snapshots) {
                        val cliente = Cliente(
                            id = 0,
                            nombre = doc.getString("nombre") ?: "",
                            correo = doc.getString("correo") ?: "",
                            contrasena = doc.getString("contrasena") ?: "",
                            fechaCompra = doc.getString("fechaCompra") ?: "",
                            fechaVencimiento = doc.getString("fechaVencimiento") ?: "",
                            precio = doc.getDouble("precio") ?: 0.0,
                            tipoCuenta = doc.getString("tipo") ?: "",
                            observacion = doc.getString("observaciones") ?: "",
                            plataforma = doc.getString("plataforma") ?: "",
                            telefono = doc.getString("telefono") ?: "",
                            documentId = doc.id
                        )
                        listaClientes.add(cliente)
                    }
                    // 🔹 Llenar lista filtrada con todos los clientes
                    listaFiltrada.clear()
                    listaFiltrada.addAll(listaClientes)
                    adapter.notifyDataSetChanged()
                }
            }
    }


    private fun mostrarDialogoEditar(cliente: Cliente) {
        val view = layoutInflater.inflate(R.layout.dialog_editar_cliente, null)

        val edtCorreo = view.findViewById<EditText>(R.id.edtNuevoCorreo)
        val edtContrasena = view.findViewById<EditText>(R.id.edtNuevaContrasena)
        val edtObservacion = view.findViewById<EditText>(R.id.edtObservaciones)
        val edtFechaCompra = view.findViewById<EditText>(R.id.edtFechaCompra)

        // Rellenamos con los valores actuales
        edtCorreo.setText(cliente.correo)
        edtContrasena.setText(cliente.contrasena)
        edtObservacion.setText(cliente.observacion)
        edtFechaCompra.setText(cliente.fechaCompra)

        // --- Configuramos DatePicker para fecha de compra ---
        edtFechaCompra.isFocusable = false
        edtFechaCompra.isClickable = true

        edtFechaCompra.setOnClickListener {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance()

            // Intentamos parsear la fecha actual
            val fechaParseada: Date = formato.parse(edtFechaCompra.text.toString()) ?: Date()
            calendar.time = fechaParseada

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val nuevaFecha = Calendar.getInstance()
                    nuevaFecha.set(selectedYear, selectedMonth, selectedDay)
                    edtFechaCompra.setText(formato.format(nuevaFecha.time))
                },
                year, month, day
            )
            datePicker.show()
        }

        // --- AlertDialog para editar ---
        AlertDialog.Builder(this)
            .setTitle("Editar cliente")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoCorreo = edtCorreo.text.toString()
                val nuevaContrasena = edtContrasena.text.toString()
                val nuevaObservacion = edtObservacion.text.toString()
                val nuevaFechaCompra = edtFechaCompra.text.toString()

                if (nuevoCorreo.isNotBlank() &&
                    nuevaContrasena.isNotBlank() &&
                    nuevaObservacion.isNotBlank() &&
                    nuevaFechaCompra.isNotBlank()
                ) {
                    // Actualizamos en memoria
                    cliente.correo = nuevoCorreo
                    cliente.contrasena = nuevaContrasena
                    cliente.observacion = nuevaObservacion
                    cliente.fechaCompra = nuevaFechaCompra

                    // --- Calculamos fecha de vencimiento automáticamente (+30 días) ---
                    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fechaCompraParseada: Date = formato.parse(nuevaFechaCompra) ?: Date()
                    val calendar = Calendar.getInstance()
                    calendar.time = fechaCompraParseada
                    calendar.add(Calendar.DAY_OF_MONTH, 30)
                    val nuevaFechaVencimiento = formato.format(calendar.time)
                    cliente.fechaVencimiento = nuevaFechaVencimiento

                    // --- Actualizamos en Firestore ---
                    actualizarClienteEnFirestore(cliente)

                    // --- Actualizamos la lista visible ---
                    adapter.actualizarLista(dbHelper.obtenerTodosLosClientes())

                    Toast.makeText(this, "Cliente actualizado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Campos vacíos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }



    private fun actualizarClienteEnFirestore(cliente: Cliente) {
        if (cliente.documentId.isEmpty()) return

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("clientes")
            .document(cliente.documentId)
            .update(
                mapOf(
                    "correo" to cliente.correo,
                    "contrasena" to cliente.contrasena,
                    "fechaCompra" to cliente.fechaCompra,
                    "fechaVencimiento" to cliente.fechaVencimiento,
                    "observaciones" to cliente.observacion
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Actualizado también en Firestore", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al actualizar Firestore: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


}
