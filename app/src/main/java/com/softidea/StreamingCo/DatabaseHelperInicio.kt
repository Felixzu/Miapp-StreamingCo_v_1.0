package com.softidea.StreamingCo

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DatabaseHelperInicio(private val context: Context):
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {

        private const val DATABASE_NAME = "streaming_co.db"
        private const val DATABASE_VERSION = 5
        private const val TABLE_NAME = "data"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"

    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = ("CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_USERNAME TEXT, " +
                "$COLUMN_PASSWORD TEXT)")
        db?.execSQL(createTableQuery)

        val createClientes = """
    CREATE TABLE clientes (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        nombre TEXT,
        correo TEXT,
        contrasena TEXT,
        fecha_compra TEXT,
        fecha_vencimiento TEXT,
        precio REAL,
        tipo_cuenta TEXT,
        plataforma TEXT,   
        observaciones TEXT,
        telefono TEXT
    )
""".trimIndent()

        db?.execSQL(createClientes)


    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 5) {
            db?.execSQL("ALTER TABLE clientes ADD COLUMN telefono TEXT")
        }
    }


    fun insertUser(username: String, password: String): Long {
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
        }

        val db = writableDatabase
        return db.insert(TABLE_NAME, null, values)
    }

    fun readUser(username: String, password: String): Boolean {

        val db = readableDatabase
        val selection = "$COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val selectionArgs = arrayOf(username, password)
        val cursor = db.query(
            TABLE_NAME,
            null, // null means all columns
            selection,
            selectionArgs,
            null, // groupBy
            null, // having
            null  // orderBy
        )

        val userExists = cursor.count > 0
        cursor.close()
        return userExists
    }

    fun insertarCliente(cliente: Cliente): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("nombre", cliente.nombre)
            put("correo", cliente.correo)
            put("contrasena", cliente.contrasena)
            put("fecha_compra", cliente.fechaCompra)
            put("fecha_vencimiento", cliente.fechaVencimiento)
            put("precio", cliente.precio)
            put("tipo_cuenta", cliente.tipoCuenta)
            put("observaciones", cliente.observacion)
            put("plataforma", cliente.plataforma)
            put("telefono", cliente.telefono)
        }

        val resultado = db.insert("clientes", null, values)
        db.close()

        if (resultado != -1L) {
            val fireDb = FirebaseFirestore.getInstance()
            val clienteMap = hashMapOf(
                "nombre" to cliente.nombre,
                "correo" to cliente.correo,
                "contrasena" to cliente.contrasena,
                "fecha_compra" to cliente.fechaCompra,
                "fecha_vencimiento" to cliente.fechaVencimiento,
                "precio" to cliente.precio,
                "tipo_cuenta" to cliente.tipoCuenta,
                "observaciones" to cliente.observacion,
                "plataforma" to cliente.plataforma,
                "telefono" to cliente.telefono
            )

            // ✅ Generar un ID único basado en correo, plataforma y contraseña
            val idDocumento = "${cliente.correo}_${cliente.plataforma}_${cliente.contrasena}"
                .replace(".", "_")
                .replace("#", "_")
                .replace("[", "_")
                .replace("]", "_")
                .replace("$", "_")
                .replace("/", "_") // Firestore no acepta estos caracteres en IDs

            fireDb.collection("clientes")
                .document(idDocumento)
                .set(clienteMap)
                .addOnSuccessListener {
                    Log.d("Firebase", "Cliente subido correctamente")
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Error al subir a Firebase", it)
                }

            return true
        }

        return false
    }


    fun obtenerTodosLosClientes(): List<Cliente> {
        val lista = mutableListOf<Cliente>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM clientes", null)

        // Verifica si existe la columna "telefono"
        val columnas = cursor.columnNames
        val tieneTelefono = columnas.contains("telefono")

        if (cursor.moveToFirst()) {
            do {
                val cliente = Cliente(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    correo = cursor.getString(cursor.getColumnIndexOrThrow("correo")),
                    contrasena = cursor.getString(cursor.getColumnIndexOrThrow("contrasena")),
                    telefono = if (tieneTelefono) {
                        val index = cursor.getColumnIndex("telefono")
                        if (index != -1) cursor.getString(index) ?: "" else ""
                    } else "",
                    fechaCompra = cursor.getString(cursor.getColumnIndexOrThrow("fecha_compra")),
                    fechaVencimiento = cursor.getString(cursor.getColumnIndexOrThrow("fecha_vencimiento")),
                    precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio")),
                    tipoCuenta = cursor.getString(cursor.getColumnIndexOrThrow("tipo_cuenta")),
                    plataforma = cursor.getString(cursor.getColumnIndexOrThrow("plataforma")),
                    observacion = cursor.getString(cursor.getColumnIndexOrThrow("observaciones"))
                )
                lista.add(cliente)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }


    fun eliminarClientePorId(id: Int): Int {
        val db = writableDatabase

        // 🔍 Obtener datos del cliente antes de borrarlo para poder eliminar en Firestore
        val cursor = db.rawQuery(
            "SELECT correo, contrasena, plataforma FROM clientes WHERE id = ?",
            arrayOf(id.toString())
        )
        var filasAfectadas = 0

        if (cursor.moveToFirst()) {
            val correo = cursor.getString(0)
            val contrasena = cursor.getString(1)
            val plataforma = cursor.getString(2)
            val idDoc = "${correo}_${plataforma}_${contrasena}".replace(".", "_").replace("/", "_")
                .replace("#", "_")

            FirebaseFirestore.getInstance()
                .collection("clientes")
                .document(idDoc)
                .delete()
                .addOnSuccessListener { Log.d("Firestore", "Cliente eliminado en Firestore") }
                .addOnFailureListener { Log.e("Firestore", "Error al eliminar cliente", it) }

            filasAfectadas = db.delete("clientes", "id = ?", arrayOf(id.toString()))
        }

        cursor.close()
        return filasAfectadas
    }


    fun obtenerPagosPorEstado(): Triple<List<Cliente>, List<Cliente>, List<Cliente>> {
        val vencidos = mutableListOf<Cliente>()
        val hoy = mutableListOf<Cliente>()
        val proximos = mutableListOf<Cliente>()

        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        formato.isLenient = false

        // Obtener fecha de hoy sin horas
        val calendarHoy = Calendar.getInstance()
        calendarHoy.set(Calendar.HOUR_OF_DAY, 0)
        calendarHoy.set(Calendar.MINUTE, 0)
        calendarHoy.set(Calendar.SECOND, 0)
        calendarHoy.set(Calendar.MILLISECOND, 0)
        val fechaHoy = calendarHoy.time

        // Obtener fecha dentro de 3 días sin horas
        val calendar3Dias = Calendar.getInstance()
        calendar3Dias.time = fechaHoy
        calendar3Dias.add(Calendar.DAY_OF_YEAR, 3)
        val fecha3Dias = calendar3Dias.time

        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM clientes", null)

        if (cursor.moveToFirst()) {
            do {
                val fechaVencStr =
                    cursor.getString(cursor.getColumnIndexOrThrow("fecha_vencimiento"))

                try {
                    val fechaVenc = formato.parse(fechaVencStr)

                    val cliente = Cliente(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                        correo = cursor.getString(cursor.getColumnIndexOrThrow("correo")),
                        contrasena = cursor.getString(cursor.getColumnIndexOrThrow("contrasena")),
                        telefono = cursor.getString(cursor.getColumnIndexOrThrow("telefono")),
                        fechaCompra = cursor.getString(cursor.getColumnIndexOrThrow("fecha_compra")),
                        fechaVencimiento = fechaVencStr,
                        precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio")),
                        tipoCuenta = cursor.getString(cursor.getColumnIndexOrThrow("tipo_cuenta")),
                        plataforma = cursor.getString(cursor.getColumnIndexOrThrow("plataforma")),
                        observacion = cursor.getString(cursor.getColumnIndexOrThrow("observaciones"))
                    )

                    // Comparar usando el correo como identificador único
                    when {
                        fechaVenc!!.before(fechaHoy) -> if (vencidos.none { it.correo == cliente.correo }) vencidos.add(
                            cliente
                        )

                        formato.format(fechaVenc) == formato.format(fechaHoy) -> if (hoy.none { it.correo == cliente.correo }) hoy.add(
                            cliente
                        )

                        fechaVenc.after(fechaHoy) && !fechaVenc.after(fecha3Dias) -> if (proximos.none { it.correo == cliente.correo }) proximos.add(
                            cliente
                        )
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } while (cursor.moveToNext())
        }

        cursor.close()
        return Triple(vencidos, hoy, proximos)
    }


    fun actualizarCliente(cliente: Cliente): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("fecha_compra", cliente.fechaCompra)
            put("fecha_vencimiento", cliente.fechaVencimiento)
        }

        // 🔁 Actualizar en Firestore
        val idDoc =
            "${cliente.correo}_${cliente.plataforma}_${cliente.contrasena}".replace(".", "_")
                .replace("/", "_").replace("#", "_")
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("clientes")
            .document(idDoc)
            .update(
                mapOf(
                    "fecha_compra" to cliente.fechaCompra,
                    "fecha_vencimiento" to cliente.fechaVencimiento
                )
            )
            .addOnSuccessListener { Log.d("Firestore", "Fechas actualizadas en Firestore") }
            .addOnSuccessListener { Log.d("Firestore", "Fechas actualizadas en Firestore") }
            .addOnFailureListener { Log.e("Firestore", "Error al actualizar Firestore", it) }

        return db.update("clientes", values, "id=?", arrayOf(cliente.id.toString())) > 0
    }


    fun actualizarFechasCliente(id: Int, nuevaCompra: String, nuevoVencimiento: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("fecha_compra", nuevaCompra)
            put("fecha_vencimiento", nuevoVencimiento)
        }
        db.update("clientes", values, "id = ?", arrayOf(id.toString()))
    }


    fun actualizarCorreoContrasenaObservacion(id: Int, correo: String, contrasena: String, nuevaobservacion: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("correo", correo)
            put("contrasena", contrasena)
            put("observaciones", nuevaobservacion)
        }
        return db.update("clientes", values, "id = ?", arrayOf(id.toString()))
    }



    fun actualizarCuentaCompartida(correoAntiguo: String, contrasenaAntigua: String, nuevoCorreo: String, nuevaContrasena: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("correo", nuevoCorreo)
            put("contrasena", nuevaContrasena)
        }
        db.update("clientes", values, "correo = ? AND contrasena = ?", arrayOf(correoAntiguo, contrasenaAntigua))
    }

    fun eliminarClientesPorCorreoYContrasena(correo: String, contrasena: String) {
        val db = this.writableDatabase
        db.delete("clientes", "correo = ? AND contrasena = ?", arrayOf(correo, contrasena))
    }


    fun obtenerCuentasAgrupadas(): List<CuentaCompartida> {
        val todos = obtenerTodosLosClientes()

        return todos
            .groupBy { Triple(it.correo, it.contrasena, it.plataforma) }
            .map { (key, clientes) ->
                CuentaCompartida(
                    correo = key.first,
                    contrasena = key.second,
                    plataforma = key.third,
                    clientes = clientes.toMutableList() // ✅ conversión correcta
                )
            }
    }

    fun actualizarNombreYObservacion(id: Int, nuevoNombre: String, observacion: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nombre", nuevoNombre)
            put("observaciones", observacion)
        }
        db.update("clientes", values, "id=?", arrayOf(id.toString()))
    }

    fun clienteExiste(correo: String, contrasena: String, plataforma: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM clientes WHERE correo = ? AND contrasena = ? AND plataforma = ?",
            arrayOf(correo, contrasena, plataforma)
        )
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }












}

