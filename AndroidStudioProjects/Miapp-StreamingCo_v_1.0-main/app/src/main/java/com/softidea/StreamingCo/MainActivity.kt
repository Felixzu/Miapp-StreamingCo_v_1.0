package com.softidea.StreamingCo

import android.annotation.SuppressLint
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.Locale
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseStorage:FirebaseStorage
    private lateinit var firestore:FirebaseFirestore

    private lateinit var dbHelper: DatabaseHelperInicio

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_activity)


        insertarClientesDePrueba()

        FirebaseApp.initializeApp(this)

        val database = com.google.firebase.database.FirebaseDatabase.getInstance()
        val reference = database.getReference("prueba_conexion")

        reference.setValue("Hola Firebase desde Android")
            .addOnSuccessListener {
                Toast.makeText(this, "Conectado a Firebase", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Fallo la conexión a Firebase", Toast.LENGTH_SHORT).show()
            }

        dbHelper = DatabaseHelperInicio(this)

        if (dbHelper.obtenerTodosLosClientes().isEmpty()) {
            sincronizarDesdeFirebase(dbHelper)
        }


        val togglePassword = findViewById<ImageView>(R.id.togglePassword)


        var isPasswordVisible = true // Variable para controlar la visibilidad de la contraseña

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)


        // Toggle para mostrar/ocultar contraseña
        togglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                edtPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePassword.setImageResource(R.drawable.eyeopen)
            } else {
                edtPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePassword.setImageResource(R.drawable.eyebrow)
            }
            edtPassword.setSelection(edtPassword.text.length) // mantiene el cursor al final
        }


        // Botón de LOGIN
        btnLogin.setOnClickListener {
            val username = edtEmail.text.toString()
            val password = edtPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userExists = dbHelper.readUser(username, password)

            if (userExists) {
                Toast.makeText(this, "Logueado correctamente", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, FirstMainActivity::class.java)
                startActivity(intent)
                finish() // Esto evita que pueda volver con el botón 'Atrás'



            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón de REGISTRAR
        btnRegister.setOnClickListener {
            val username = edtEmail.text.toString()
            val password = edtPassword.text.toString()
            val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)


        }

        val dbHelper = DatabaseHelperInicio(this)



    }
    private fun insertarClientesDePrueba() {

        val dbHelper = DatabaseHelperInicio(this)
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun obtenerFecha(dias: Int): String {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, dias)
            return formato.format(calendar.time)
        }
    }

    private fun sincronizarDesdeFirebase(dbHelper: DatabaseHelperInicio) {
        val db = FirebaseFirestore.getInstance()

        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val cliente = Cliente(
                        id = 0,
                        nombre = document.getString("nombre") ?: "",
                        correo = document.getString("correo") ?: "",
                        contrasena = document.getString("contrasena") ?: "",
                        fechaCompra = document.getString("fecha_compra") ?: "",
                        fechaVencimiento = document.getString("fecha_vencimiento") ?: "",
                        precio = document.getDouble("precio") ?: 0.0,
                        tipoCuenta = document.getString("tipo_cuenta") ?: "",
                        observacion = document.getString("observaciones") ?: "",
                        plataforma = document.getString("plataforma") ?: "",
                        telefono = document.getString("telefono") ?: ""
                    )
                    dbHelper.insertarCliente(cliente)
                }
                Toast.makeText(this, "Clientes sincronizados desde Firebase", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al sincronizar desde Firebase", Toast.LENGTH_SHORT).show()
                Log.e("FirebaseSync", "Error", it)
            }
    }



}