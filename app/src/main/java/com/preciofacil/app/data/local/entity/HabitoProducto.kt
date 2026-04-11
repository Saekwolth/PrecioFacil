package com.preciofacil.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla: habitos_producto
 * Guarda los patrones de compra aprendidos para cada producto.
 * La app los calcula automáticamente observando los tickets,
 * y el usuario puede corregirlos manualmente.
 */
@Entity(
    tableName = "habitos_producto",
    foreignKeys = [
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Supermercado::class,
            parentColumns = ["id"],
            childColumns = ["supermercadoHabualId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("productoId"), Index("supermercadoHabualId")]
)
data class HabitoProducto(

    // Identificador único — Room lo genera automáticamente
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // A qué producto corresponde este hábito
    val productoId: Long,

    // Cuántas unidades suele comprar de media
    val cantidadMediaHabitual: Double = 1.0,

    // Cada cuántos días lo compra normalmente
    val frecuenciaMediaDias: Int = 7,

    // Cuándo lo compró por última vez (milisegundos)
    val ultimaVezComprado: Long? = null,

    // Dónde lo compra más frecuentemente
    val supermercadoHabualId: Long? = null,

    // Si el usuario ha corregido el hábito manualmente
    // Si es false, los valores los calculó la app automáticamente
    val ajustadoManualmente: Boolean = false,

    // Si debe avisar cuando el hábito no se cumple
    val avisosHabitoActivados: Boolean = true,

    // Cuántos tickets necesita ver antes de considerar que hay un hábito
    // Por defecto 3 — evita alertas falsas en las primeras semanas
    val minimoTicketsParaCalcular: Int = 3,

    // Cuántos tickets ha procesado hasta ahora para este producto
    // Cuando llega a minimoTicketsParaCalcular, se activan las alertas
    val ticketsProcesados: Int = 0,

    // Cuántas veces seguidas el usuario ha silenciado la alerta de hábito
    // Si llega a 2, la app sugiere marcar el producto como de temporada
    val alertasSilenciadasSeguidas: Int = 0,

    // Desviación típica de la cantidad — si es alta, la app es más
    // permisiva antes de generar una alerta de cantidad inusual
    val desviacionCantidad: Double = 0.0
)
