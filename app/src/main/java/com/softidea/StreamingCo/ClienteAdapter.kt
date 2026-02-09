import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.softidea.StreamingCo.Cliente
import com.softidea.StreamingCo.R
import java.util.Locale

class ClienteAdapter(
    private val lista: MutableList<Cliente>,
    private val onEliminarClick: (Cliente) -> Unit,
    private val onRenovarClick: ((Cliente) -> Unit)? = null,
    private val onEditarClick: ((Cliente) -> Unit)? = null
) : RecyclerView.Adapter<ClienteAdapter.ClienteViewHolder>() {

    private val listaCompleta = mutableListOf<Cliente>().apply { addAll(lista) }

    fun actualizarLista(nuevaLista: List<Cliente>) {
        lista.clear()
        lista.addAll(nuevaLista)
        listaCompleta.clear()
        listaCompleta.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    fun eliminarCliente(cliente: Cliente) {
        lista.remove(cliente)
        listaCompleta.remove(cliente)
        notifyDataSetChanged()
    }

    fun filtrar(texto: String) {
        val textoFiltrado = texto.lowercase(Locale.getDefault())
        val listaFiltrada = listaCompleta.filter {
            it.nombre.lowercase(Locale.getDefault()).contains(textoFiltrado) ||
                    it.correo.lowercase(Locale.getDefault()).contains(textoFiltrado)
        }
        lista.clear()
        lista.addAll(listaFiltrada)
        notifyDataSetChanged()
    }

    inner class ClienteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvCorreo: TextView = view.findViewById(R.id.tvCorreo)
        val tvContrasena: TextView = view.findViewById(R.id.tvContrasena)
        val tvFechaCompra: TextView = view.findViewById(R.id.tvFechaCompra)
        val tvFechaVencimiento: TextView = view.findViewById(R.id.tvFechaVencimiento)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecio)
        val tvTipoCuenta: TextView = view.findViewById(R.id.tvTipoCuenta)
        val tvObservaciones: TextView = view.findViewById(R.id.tvObservaciones)
        val tvPlataforma: TextView = itemView.findViewById(R.id.tvPlataforma)
        val tvTelefono: TextView = itemView.findViewById(R.id.tvTelefono)
        val imgPlataforma: ImageView = itemView.findViewById(R.id.imgPlataforma) // Nuevo ImageView
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminar)
        val btnRenovar: ImageButton = view.findViewById(R.id.btnRenovar)
        val btnEditar: ImageButton = view.findViewById(R.id.btnEditar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cliente, parent, false)
        return ClienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        val cliente = lista[position]

        holder.tvNombre.text = "👤 ${cliente.nombre}"
        holder.tvCorreo.text = "📧 ${cliente.correo}"
        holder.tvContrasena.text = "🔒 ${cliente.contrasena}"
        holder.tvFechaCompra.text = "🛒 Compra: ${cliente.fechaCompra}"
        holder.tvFechaVencimiento.text = "📅 Vence: ${cliente.fechaVencimiento}"
        holder.tvPrecio.text = "💵 \$${cliente.precio}"
        holder.tvTipoCuenta.text = "📦 Tipo: ${cliente.tipoCuenta}"
        holder.tvObservaciones.text = "📝 ${cliente.observacion}"
        holder.tvPlataforma.text = "🎬 Plataforma: ${cliente.plataforma}"
        holder.tvTelefono.text = "📱 Teléfono: ${cliente.telefono}"

        // Asignar icono de la plataforma
        holder.imgPlataforma.setImageResource(obtenerIconoPlataforma(cliente.plataforma))

        holder.btnEliminar.setOnClickListener { onEliminarClick(cliente) }
        holder.btnRenovar.visibility = if (onRenovarClick != null) View.VISIBLE else View.GONE
        holder.btnRenovar.setOnClickListener { onRenovarClick?.invoke(cliente) }
        holder.btnEditar.setOnClickListener { onEditarClick?.invoke(cliente) }
    }

    override fun getItemCount(): Int = lista.size

    // Función para obtener icono según plataforma
    private fun obtenerIconoPlataforma(nombre: String): Int {
        return when (nombre.trim().lowercase()) {
            "netflix" -> R.drawable.ic_netflix
            "hbo" -> R.drawable.ic_hbo
            "disney+" -> R.drawable.ic_disney
            "disney+ con espn" -> R.drawable.ic_disneyespn
            "spotify" -> R.drawable.ic_spotify
            "amazon prime" -> R.drawable.ic_prime
            "flujotv" -> R.drawable.ic_flujotv
            "iptvsmarters" -> R.drawable.ic_iptvsmarters
            "youtubepremium" -> R.drawable.ic_yt
            "dgo" -> R.drawable.ic_dgo
            else -> R.drawable.ic_plataforma_generico // Icono por defecto
        }
    }
}
