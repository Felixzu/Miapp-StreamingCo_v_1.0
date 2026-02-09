package com.softidea.StreamingCo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileWriter
import java.io.IOException

class FirstMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_main) // ← aquí se enlaza tu XML

        val dbHelper = DatabaseHelperInicio(this)



        val userName = intent.getStringExtra("username") ?: "Usuario"

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        tvWelcome.text = "Hola, $userName 👋"

        val btnClientes = findViewById<Button>(R.id.btnClientes)
        val btnPagos = findViewById<Button>(R.id.btnPagos)
        val btnNuevoCliente = findViewById<Button>(R.id.btnNuevoCliente)
        val btnAgregarCliente = findViewById<Button>(R.id.btnNuevoCliente)
        val btnDashboard = findViewById<Button>(R.id.btnDashboard)
        val btnExportar = findViewById<Button>(R.id.btnExportar)
        val btnCuentasAgrupadas = findViewById<Button>(R.id.btnCuentasAgrupadas)




        btnClientes.setOnClickListener {
            val intent = Intent(this, ListaClientesActivity::class.java)
            startActivity(intent)
        }

        btnDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        btnPagos.setOnClickListener {
            val intent = Intent(this, PagosActivity::class.java)
            startActivity(intent)
        }

        btnNuevoCliente.setOnClickListener {
            Toast.makeText(this, "Abrir registro de cliente", Toast.LENGTH_SHORT).show()
        }

        btnAgregarCliente.setOnClickListener {
            val intent = Intent(this, AgregarClienteActivity::class.java)
            startActivity(intent)
        }

        btnCuentasAgrupadas.setOnClickListener {
            val intent = Intent(this, ListaClientesAgrupadosActivity::class.java)
            startActivity(intent)
        }

        fun exportarClientes(context: Context, clientes: List<Cliente>) {
            val fileName = "clientes_exportados.csv"
            val file = File(context.getExternalFilesDir(null), fileName)

            try {
                val writer = FileWriter(file)
                writer.append("ID,Nombre,Correo,Contraseña,Fecha Compra,Fecha Vencimiento,Precio,Tipo de Cuenta,Observaciones\n")
                for (cliente in clientes) {
                    writer.append("${cliente.id},\"${cliente.nombre}\",\"${cliente.correo}\",\"${cliente.contrasena}\",")
                    writer.append("\"${cliente.fechaCompra}\",\"${cliente.fechaVencimiento}\",")
                    writer.append("${cliente.precio},\"${cliente.tipoCuenta}\",\"${cliente.observacion}\"\n")
                }
                writer.flush()
                writer.close()

                Toast.makeText(context, "Clientes exportados a:\n${file.absolutePath}", Toast.LENGTH_LONG).show()

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Error al exportar clientes", Toast.LENGTH_SHORT).show()
            }
        }

        btnExportar.setOnClickListener {
            val dbHelper = DatabaseHelperInicio(this)
            val clientes = dbHelper.obtenerTodosLosClientes()
            exportarClientes(this, clientes)
        }




    }
}
