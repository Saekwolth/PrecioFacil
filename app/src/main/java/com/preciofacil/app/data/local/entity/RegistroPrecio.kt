package com.preciofacil.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla: registros_precio
 * Guarda cada vez que se registra el precio de un producto en un supermercado.
 * Es el historial de precios — la base de todas las comparativas y alertas.
 * Las comparativas siempre usan precioSinImpuesto para ser justas
 * entre supermercados de distintos países.
 */
@Entity(
    tableName = "registros_precio",
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
            onDelete = ForeignKey.CASCADE
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
        Index("fecha")
    ]
)
data class RegistroPrecio(

    // Identificador único — Room lo genera automáticamente
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Qué producto es
    val productoId: Long,

    // En qué supermercado se registró
    val supermercadoId: Long,

    // De qué ticket proviene este dato
    val ticketId: Long? = null,

    // Fecha del registro en milisegundos
    val fecha: Long = System.currentTimeMillis(),

    // Precio unitario tal como aparece en el ticket (con impuesto incluido)
    val precioConImpuesto: Double,

    // Precio sin impuesto — usado en TODAS las comparativas entre supermercados
    // Para Andorra: precio ÷ 1.045 (IGI 4,5%)
    // Para España:  precio ÷ 1.04, 1.10 o 1.21 según categoría
    val precioSinImpuesto: Double,

    // Tipo de impuesto que se descontó para calcular precioSinImpuesto
    // "IGI_AD_4.5", "IVA_ES_4", "IVA_ES_10" o "IVA_ES_21"
    val tipoImpuestoAplicado: String = "IGI_AD_4.5",

    // Si el usuario confirmó que el tipo de impuesto es correcto
    val impuestoRevisado: Boolean = false,

    // Precio por kg o litro — calculado automáticamente
    // Permite comparar productos de distinto tamaño y detectar
    // encarecimientos encubiertos por reducción de formato
    val precioPorKgLitro: Double? = null,

    // Si este precio corresponde a una oferta puntual
    // Los precios promocionales no se usan como referencia habitual
    val esPrecioPromocional: Boolean = false,

    // Tipo de promoción: "2x1", "3x2", "descuento_pct", "pack"
    val tipoPromocion: String? = null,

    // Precio sin promoción, si se puede extraer del ticket
    val precioListaOriginal: Double? = null
)
