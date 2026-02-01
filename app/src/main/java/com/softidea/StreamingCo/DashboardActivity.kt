package com.softidea.StreamingCo

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var txtClientesActivos: TextView
    private lateinit var txtIngresos: TextView
    private lateinit var txtPagosVencidos: TextView
    private lateinit var txtPagosPendientes: TextView

    private var snapshotListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        txtClientesActivos = findViewById(R.id.txtClientesActivos)
        txtIngresos = findViewById(R.id.txtIngresos)
        txtPagosVencidos = findViewById(R.id.txtPagosVencidos)
        txtPagosPendientes = findViewById(R.id.txtPagosPendientes)

        db = FirebaseFirestore.getInstance()

        // Cargar las estadísticas iniciales
        cargarEstadisticas()
    }

    private fun cargarEstadisticas() {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val hoy = Calendar.getInstance().time
        val fechaHoy = formato.parse(formato.format(hoy))

        snapshotListener = db.collection("clientes")
            .addSnapshotListener { result, error ->
                if (error != null) {
                    txtClientesActivos.text = "Error al obtener datos"
                    txtIngresos.text = ""
                    txtPagosVencidos.text = ""
                    txtPagosPendientes.text = ""
                    return@addSnapshotListener
                }

                var clientesActivos = 0
                var ingresosTotales = 0
                var pagosVencidos = 0
                var pagosPendientes = 0

                for (doc in result!!) {
                    val fechaVenc = doc.getString("fechaVencimiento")
                    val precio = doc.getLong("precio")?.toInt() ?: 0  // Se toma el precio del cliente

                    if (!fechaVenc.isNullOrEmpty()) {
                        try {
                            val fechaCliente = formato.parse(fechaVenc)
                            // Depuración para ver las fechas
                            Log.d("DashboardActivity", "Fecha de vencimiento del cliente: $fechaCliente")

                            if (fechaCliente != null) {
                                // Si la fecha de vencimiento es después de hoy, es un pago pendiente
                                if (fechaCliente.after(hoy)) {
                                    clientesActivos++
                                    ingresosTotales += precio
                                    pagosPendientes++
                                } else if (fechaCliente.before(hoy)) {
                                    // Si la fecha de vencimiento es antes de hoy, es un pago vencido
                                    pagosVencidos++
                                }
                            } else {
                                Log.d("DashboardActivity", "Fecha de vencimiento nula o incorrecta para el cliente: ${doc.id}")
                            }
                        } catch (e: Exception) {
                            Log.e("DashboardActivity", "Error al parsear la fecha para el cliente ${doc.id}: ${e.message}")
                        }
                    }
                }

                // Mostrar los resultados en los TextViews
                val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                txtClientesActivos.text = "Clientes activos: $clientesActivos"
                txtIngresos.text = "Ingresos estimados: ${formatoMoneda.format(ingresosTotales)}"
                txtPagosVencidos.text = "Pagos vencidos: $pagosVencidos"
                txtPagosPendientes.text = "Pagos pendientes: $pagosPendientes"
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Eliminar el listener cuando la actividad se destruya
        snapshotListener?.remove()
    }
}
