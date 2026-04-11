package com.preciofacil.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla: alertas
 * Guarda todos los avisos generados por la app.
 * Se generan automáticamente al procesar cada ticket.
 */
@Entity(
    tableName = "alertas",
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
            childColumns = ["supermercadoId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Ticket::class,
            parentColumns = ["id"],
            childColumns = ["ticketId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("productoId"),
        Index("supermercadoId"),
        Index("ticketId"),
        Index("leida"),
        Index("fecha")
    ]
)
data class Alerta(

    // Identificador único — Room lo genera automáticamente
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Categoría del aviso:
    // "subida_precio"            = el precio ha subido
    // "bajada_precio"            = el precio ha bajado
    // "mas_barato_otro_super"    = más barato en otro supermercado
    // "habito_cantidad"          = cantidad inusual comprada
    // "habito_frecuencia"        = producto no comprado en mucho tiempo
    // "habito_supermercado"      = comprado en super diferente al habitual
    // "encarecimiento_encubierto"= precio por kg ha subido aunque el precio
    //                              de etiqueta no haya cambiado
    val tipo: String,

    // Producto al que afecta la alerta
    val productoId: Long,

    // Supermercado relacionado (si aplica)
    val supermercadoId: Long? = null,

    // Ticket que desencadenó la alerta
    val ticketId: Long? = null,

    // Texto del aviso legible para el usuario
    // Ej: "Aceite oliva Borges 1L ha subido +0,40€ (+8%) en Caprabo"
    val mensaje: String,

    // Variación en euros (positivo = subida, negativo = bajada)
    // Guardado para ordenar alertas por impacto económico real
    val variacionEuros: Double? = null,

    // Variación en porcentaje
    val variacionPorcentaje: Double? = null,

    // Cuándo se generó la alerta (milisegundos)
    val fecha: Long = System.currentTimeMillis(),

    // Si el usuario ya la ha visto
    val leida: Boolean = false,

    // Identificador del hogar — para sincronizar con Firebase
    val codigoHogar: String = ""
)
