package com.softidea.StreamingCo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class PlataformaSpinnerAdapter(
    private val context: Context,
    private val plataformas: List<PlataformaItem>
) : BaseAdapter() {

    override fun getCount(): Int = plataformas.size

    override fun getItem(position: Int): Any = plataformas[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_spinner_plataforma, parent, false)
        val icono = itemView.findViewById<ImageView>(R.id.imgIcono)
        val nombre = itemView.findViewById<TextView>(R.id.txtNombre)

        val item = plataformas[position]
        icono.setImageResource(item.iconoResId)
        nombre.text = item.nombre

        return itemView
    }
}
