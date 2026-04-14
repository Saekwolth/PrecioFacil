package com.preciofacil.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.preciofacil.app.data.local.entity.Supermercado

/**
 * SupermercadoAdapter — muestra la lista de supermercados en el Dashboard.
 *
 * Cada tarjeta muestra:
 *  - Un círculo de color con la inicial del supermercado
 *  - El nombre del supermercado
 *  - El país (Andorra o España)
 *  - Una etiqueta "Activo"
 */
class SupermercadoAdapter(
    private var supermercados: List<Supermercado> = emptyList()
) : RecyclerView.Adapter<SupermercadoAdapter.ViewHolder>() {

    class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val iconoLetra: TextView = itemView.findViewById(R.id.iconoLetra)
        val nombre: TextView = itemView.findViewById(R.id.nombreSupermercado)
        val pais: TextView = itemView.findViewById(R.id.paisSupermercado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_supermercado, parent, false)
        return ViewHolder(vista)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val supermercado = supermercados[position]

        // Inicial del nombre en el círculo
        holder.iconoLetra.text = supermercado.nombre.take(1).uppercase()

        // Color del círculo según el supermercado (usa el color guardado en BD)
        try {
            holder.iconoLetra.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor(supermercado.color))
        } catch (e: Exception) {
            // Si el color no es válido, usar verde por defecto
            holder.iconoLetra.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#1E8449"))
        }

        // Nombre
        holder.nombre.text = supermercado.nombre

        // País con bandera
        holder.pais.text = when (supermercado.pais) {
            "AD" -> "🇦🇩 Andorra"
            "ES" -> "🇪🇸 España"
            else -> supermercado.pais
        }
    }

    override fun getItemCount() = supermercados.size

    /**
     * Actualiza la lista cuando llegan nuevos datos de la base de datos.
     */
    fun actualizarLista(nuevaLista: List<Supermercado>) {
        supermercados = nuevaLista
        notifyDataSetChanged()
    }
}
