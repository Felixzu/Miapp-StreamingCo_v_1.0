package com.softidea.StreamingCo


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ListaClientesAgrupadosActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelperInicio
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CuentaCompartidaAdapter
    lateinit var cuentasListener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_cuentas)

        dbHelper = DatabaseHelperInicio(this)
        recyclerView = findViewById(R.id.recyclerCuentas)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CuentaCompartidaAdapter(
            context = this,
            cuentas = emptyList(), // Inicialmente vacío
            dbHelper,
            onEditarCuenta = { cuenta -> mostrarDialogoEditarCuenta(cuenta) },
            onEliminarCuenta = { cuenta -> eliminarCuenta(cuenta) },
            onEliminarCliente = { cliente -> eliminarPantalla(cliente) },
            onRenovarCliente = { cliente -> renovarCliente(cliente) },
            onEditarCliente = { cliente -> editarCliente(cliente) }
        )

        recyclerView.adapter = adapter

// 🔥 Carga desde Firestore


        val etBuscar = findViewById<EditText>(R.id.etBuscarCuenta)
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.filtrar(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })




    }

    private fun mostrarDialogoEditarCuenta(cuenta: CuentaCompartida) {
        // Aquí puedes mostrar un diálogo para editar correo/contraseña de la cuenta
        // y actualizar en todos los clientes relacionados
    }

    fun eliminarCuenta(cuenta: CuentaCompartida) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        cuenta.clientes.forEach { cliente ->
            val ref = db.collection("clientes").document(cliente.documentId)
            batch.delete(ref)
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()
                escucharCuentasAgrupadasDesdeFirestore { cuentas ->
                    adapter.actualizarLista(cuentas)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar cuenta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    fun eliminarPantalla(cliente: Cliente) {
        val db = FirebaseFirestore.getInstance()
        db.collection("clientes").document(cliente.documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Pantalla eliminada correctamente", Toast.LENGTH_SHORT).show()
                escucharCuentasAgrupadasDesdeFirestore { cuentas ->
                    adapter.actualizarLista(cuentas)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar pantalla: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun renovarCliente(cliente: Cliente) {

        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val nuevaFechaCompra = formato.format(Calendar.getInstance().time)

        calendar.add(Calendar.DAY_OF_MONTH, 30)
        val nuevaFechaVencimiento = formato.format(calendar.time)

        cliente.fechaCompra = nuevaFechaCompra
        cliente.fechaVencimiento = nuevaFechaVencimiento

        dbHelper.actualizarFechasCliente(cliente.id, nuevaFechaCompra, nuevaFechaVencimiento)
        actualizarLista()
        Toast.makeText(this, "Renovado ${cliente.nombre}", Toast.LENGTH_SHORT).show()

    }

    private fun editarCliente(cliente: Cliente) {
        val view = layoutInflater.inflate(R.layout.dialog_editar_cliente, null)
        val edtCorreo = view.findViewById<EditText>(R.id.edtNuevoCorreo)
        val edtContrasena = view.findViewById<EditText>(R.id.edtNuevaContrasena)
        val edtObservaciones = view.findViewById<EditText>(R.id.edtObservaciones)

        edtCorreo.setText(cliente.correo)
        edtContrasena.setText(cliente.contrasena)
        edtObservaciones.setText(cliente.observacion)

        AlertDialog.Builder(this)
            .setTitle("Editar cliente")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoCorreo = edtCorreo.text.toString()
                val nuevaContrasena = edtContrasena.text.toString()
                val nuevaObs = edtObservaciones.text.toString()

                if (nuevoCorreo.isNotBlank() && nuevaContrasena.isNotBlank()) {
                    cliente.correo = nuevoCorreo
                    cliente.contrasena = nuevaContrasena
                    cliente.observacion = nuevaObs

                    dbHelper.actualizarCorreoContrasenaObservacion(
                        cliente.id,
                        nuevoCorreo,
                        nuevaContrasena,
                        nuevaObs
                    )

                    actualizarLista()
                    Toast.makeText(this, "Cliente actualizado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Campos vacíos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun agruparPorCuenta(listaClientes: List<Cliente>): List<CuentaCompartida> {
        return listaClientes
            .filter { it.tipoCuenta.equals("Pantalla", ignoreCase = true) } // <- solo agrupar pantallas
            .groupBy { Triple(it.correo, it.contrasena, it.plataforma) }
            .map { (key, clientes) ->
                CuentaCompartida(
                    correo = key.first,
                    contrasena = key.second,
                    plataforma = key.third,
                    clientes = clientes
                )
            }
    }



    fun escucharCuentasAgrupadasDesdeFirestore(callback: (List<CuentaCompartida>) -> Unit): ListenerRegistration {
        val db = FirebaseFirestore.getInstance()
        return db.collection("clientes")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("Firestore", "Error en tiempo real al escuchar los clientes", error)
                    callback(emptyList())
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    callback(emptyList())
                    return@addSnapshotListener
                }

                val clientes = snapshots.mapNotNull { doc ->
                    try {
                        Cliente(
                            id = 0,
                            nombre = doc.getString("nombre") ?: "",
                            correo = doc.getString("correo") ?: "",
                            contrasena = doc.getString("contrasena") ?: "",
                            fechaCompra = doc.getString("fechaCompra") ?: "",
                            fechaVencimiento = doc.getString("fechaVencimiento") ?: "",
                            precio = doc.getDouble("precio") ?: 0.0,
                            tipoCuenta = doc.getString("tipoCuenta") ?: "",
                            observacion = doc.getString("observacion") ?: "",
                            plataforma = doc.getString("plataforma") ?: "",
                            telefono = doc.getString("telefono") ?: "",
                            documentId = doc.id
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                val cuentasAgrupadas = clientes
                    .groupBy { Triple(it.correo, it.contrasena, it.plataforma) }
                    .map { (clave, listaClientes) ->
                        val cuentaId = "${clave.first}_${clave.third}".lowercase(Locale.getDefault())
                        CuentaCompartida(
                            correo = clave.first,
                            contrasena = clave.second,
                            plataforma = clave.third,
                            clientes = listaClientes.toMutableList(),
                            documentId = cuentaId
                        )
                    }

                callback(cuentasAgrupadas)
            }
    }



    override fun onStart() {
        super.onStart()
        cuentasListener = escucharCuentasAgrupadasDesdeFirestore { cuentasCompartidas ->
            adapter.actualizarLista(cuentasCompartidas)
        }
    }

    override fun onStop() {
        super.onStop()
        cuentasListener.remove() // Detiene la escucha en tiempo real
    }




    private fun actualizarLista() {
        val cuentasActualizadas = agruparPorCuenta(dbHelper.obtenerTodosLosClientes())
        adapter.actualizarLista(cuentasActualizadas)
    }
}


