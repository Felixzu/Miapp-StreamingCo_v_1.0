package com.softidea.StreamingCo

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AgregarClienteActivity : AppCompatActivity() {

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private lateinit var etTelefono: EditText

    lateinit var edtCorreo: EditText
    lateinit var edtContrasena: EditText
    lateinit var tvPantallasDisponibles: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_cliente)

        val edtNombre = findViewById<EditText>(R.id.edtNombre)
        edtCorreo = findViewById(R.id.edtCorreo)
        edtContrasena = findViewById(R.id.edtContrasena)

        val correoRecibido = intent.getStringExtra("correo")
        val contrasenaRecibida = intent.getStringExtra("contrasena")
        val plataformaRecibida = intent.getStringExtra("plataforma")

        if (!correoRecibido.isNullOrEmpty()) {
            edtCorreo.setText(correoRecibido)
            edtCorreo.isEnabled = false
        }

        if (!contrasenaRecibida.isNullOrEmpty()) {
            edtContrasena.setText(contrasenaRecibida)
            edtContrasena.isEnabled = false
        }


        val edtFechaCompra = findViewById<EditText>(R.id.edtFechaCompra)
        val edtPrecio = findViewById<EditText>(R.id.edtPrecio)
        val rbCompleta = findViewById<RadioButton>(R.id.rbCompleta)
        val rbPantalla = findViewById<RadioButton>(R.id.rbPantalla)
        val edtObservaciones = findViewById<EditText>(R.id.edtObservaciones)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val spinnerPlataforma = findViewById<Spinner>(R.id.spinnerPlataforma)
        tvPantallasDisponibles = findViewById(R.id.tvPantallasDisponibles)
        val btnCambiarFecha = findViewById<Button>(R.id.btnCambiarFecha)
        val spinnerMeses = findViewById<Spinner>(R.id.spinnerMeses)
        etTelefono = findViewById(R.id.etTelefono)

        val listaMeses = (1..12).map { "$it mes${if (it > 1) "es" else ""}" }
        val adapterMeses = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaMeses)
        adapterMeses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMeses.adapter = adapterMeses

        val plataformas = listOf(
            PlataformaItem("Netflix", R.drawable.ic_netflix),
            PlataformaItem("HBO", R.drawable.ic_hbo),
            PlataformaItem("Disney+", R.drawable.ic_disney),
            PlataformaItem("Disney+ con ESPN", R.drawable.ic_disneyespn),
            PlataformaItem("Spotify", R.drawable.ic_spotify),
            PlataformaItem("Amazon Prime", R.drawable.ic_prime),
            PlataformaItem("FlujoTV", R.drawable.ic_flujotv),
            PlataformaItem("IptvSmarters", R.drawable.ic_iptvsmarters),
            PlataformaItem("YoutubePremium", R.drawable.ic_yt),
            PlataformaItem("DGO", R.drawable.ic_dgo)
        )

        val adapterSpinner = PlataformaSpinnerAdapter(this, plataformas)
        spinnerPlataforma.adapter = adapterSpinner

        btnCambiarFecha.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                calendar.set(y, m, d)
                edtFechaCompra.setText(dateFormat.format(calendar.time))
            }, year, month, day).show()
        }

        edtFechaCompra.setText(dateFormat.format(calendar.time))

        fun actualizarPantallas() {
            val correo = edtCorreo.text.toString().trim()
            val contrasena = edtContrasena.text.toString().trim()
            val plataforma = (spinnerPlataforma.selectedItem as PlataformaItem).nombre

            if (correo.isNotEmpty() && contrasena.isNotEmpty() && plataforma.isNotEmpty()) {
                val db = FirebaseFirestore.getInstance()

                db.collection("clientes")
                    .whereEqualTo("correo", correo)
                    .whereEqualTo("contrasena", contrasena)
                    .whereEqualTo("plataforma", plataforma)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            tvPantallasDisponibles.text = "Error al consultar"
                            return@addSnapshotListener
                        }

                        if (snapshots != null) {
                            var hayCompleta = false
                            var cantidadPantallas = 0

                            for (doc in snapshots) {
                                val tipo = doc.getString("tipo")
                                if (tipo.equals("Completa", ignoreCase = true)) {
                                    hayCompleta = true
                                } else if (tipo.equals("Pantalla", ignoreCase = true)) {
                                    cantidadPantallas++
                                }
                            }

                            val limite = obtenerLimitePantallas(plataforma)

                            if (hayCompleta) {
                                tvPantallasDisponibles.text = "Cuenta completa registrada"
                            } else {
                                val disponibles = limite - cantidadPantallas
                                tvPantallasDisponibles.text = "Pantallas disponibles: $disponibles de $limite"
                            }
                        }
                    }
            } else {
                tvPantallasDisponibles.text = "Pantallas disponibles: -"
            }
        }

        spinnerPlataforma.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) = actualizarPantallas()
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        edtCorreo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = actualizarPantallas()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        edtContrasena.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = actualizarPantallas()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnGuardar.setOnClickListener {
            val nombre = edtNombre.text.toString().trim()
            val correo = edtCorreo.text.toString().trim().lowercase()
            val contrasena = edtContrasena.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val fechaCompra = edtFechaCompra.text.toString().trim()
            val precio = edtPrecio.text.toString().toDoubleOrNull()
            val plataforma = (spinnerPlataforma.selectedItem as PlataformaItem).nombre.trim()
            val tipoCuenta = if (rbCompleta.isChecked) "Completa" else "Pantalla"
            val observaciones = edtObservaciones.text.toString().trim()

            if (nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty() || precio == null) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val mesesSeleccionados = spinnerMeses.selectedItemPosition + 1
            val vencimiento = Calendar.getInstance().apply {
                time = calendar.time
                add(Calendar.MONTH, mesesSeleccionados)
            }
            val fechaVencimiento = dateFormat.format(vencimiento.time)

            val db = FirebaseFirestore.getInstance()

            // Validar antes de guardar
            db.collection("clientes")
                .whereEqualTo("correo", correo)
                .whereEqualTo("contrasena", contrasena)
                .whereEqualTo("plataforma", plataforma)
                .get()
                .addOnSuccessListener { documentos ->
                    val clientesExistentes = documentos.documents

                    val hayCompleta = clientesExistentes.any { it.getString("tipo") == "Completa" }
                    val limitePantallas = obtenerLimitePantallas(plataforma)
                    val cantidadPantallas = clientesExistentes.count { it.getString("tipo") == "Pantalla" }

                    if (tipoCuenta == "Pantalla" && hayCompleta) {
                        Toast.makeText(this, "❌ Ya existe una cuenta completa con este correo", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    if (tipoCuenta == "Completa" && clientesExistentes.isNotEmpty()) {
                        Toast.makeText(this, "❌ Ya hay pantallas activas, no se puede registrar como cuenta completa", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    if (tipoCuenta == "Pantalla" && cantidadPantallas >= limitePantallas) {
                        Toast.makeText(this, "❌ No hay pantallas disponibles en esta cuenta", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    val clienteMap = mapOf(
                        "nombre" to nombre,
                        "correo" to correo,
                        "contrasena" to contrasena,
                        "fechaCompra" to fechaCompra,
                        "fechaVencimiento" to fechaVencimiento,
                        "precio" to precio,
                        "tipo" to tipoCuenta,
                        "plataforma" to plataforma,
                        "telefono" to telefono,
                        "observacion" to observaciones
                    )

                    val clienteId = "${correo}_${plataforma}_${contrasena}_${nombre.lowercase().replace(" ", "_")}"

                    db.collection("clientes").document(clienteId).set(clienteMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "✅ Cliente registrado exitosamente", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "❌ Error al guardar cliente", Toast.LENGTH_SHORT).show()
                        }
                }
        }
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

    class PlataformaSpinnerAdapter(
        context: Context,
        private val items: List<PlataformaItem>
    ) : ArrayAdapter<PlataformaItem>(context, 0, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return crearVistaPersonalizada(position, convertView, parent)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return crearVistaPersonalizada(position, convertView, parent)
        }

        private fun crearVistaPersonalizada(position: Int, convertView: View?, parent: ViewGroup): View {
            val inflater = android.view.LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.item_spinner_plataforma, parent, false)

            val imgIcono = view.findViewById<ImageView>(R.id.imgIcono)
            val txtNombre = view.findViewById<TextView>(R.id.txtNombre)

            val item = items[position]
            imgIcono.setImageResource(item.iconoResId)
            txtNombre.text = item.nombre

            return view
        }
    }
}
