package com.preciofacil.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla: lineas_ticket
 * Representa cada línea individual de un ticket escaneado.
 * Una línea puede ser un producto, un descuento, un total, etc.
 */
@Entity(
    tableName = "lineas_ticket",
    foreignKeys = [
        ForeignKey(
            entity = Ticket::class,
            parentColumns = ["id"],
            childColumns = ["ticketId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("ticketId"), Index("productoId")]
)
data class LineaTicket(

    // Identificador único — Room lo genera automáticamente
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // A qué ticket pertenece esta línea
    val ticketId: Long,

    // Texto exacto tal como apareció en el ticket (antes de cualquier corrección)
    // Ej: "EROSKI PA DE MOTLLE"
    val textoOriginalOcr: String = "",

    // Producto identificado — puede ser nulo si no se reconoció
    val productoId: Long? = null,

    // Precio total de esta línea (cantidad × precio unitario)
    val precioTotal: Double = 0.0,

    // Precio unitario del producto
    val precioUnitario: Double = 0.0,

    // Precio sin impuesto — usado para comparativas entre supermercados
    val precioSinImpuesto: Double = 0.0,

    // Precio con impuesto tal como aparece en el ticket
    val precioConImpuesto: Double = 0.0,

    // Tipo de impuesto aplicado a este registro
    // "IGI_AD_4.5", "IVA_ES_4", "IVA_ES_10" o "IVA_ES_21"
    val tipoImpuestoAplicado: String = "IGI_AD_4.5",

    // Si el usuario confirmó que el tipo de impuesto es correcto
    val impuestoRevisado: Boolean = false,

    // Cantidad de unidades compradas
    val cantidad: Int = 1,

    // Si el usuario ha confirmado o corregido esta línea
    val revisadoPorUsuario: Boolean = false,

    // Si esta línea es un producto o es otra cosa
    // (descuento, IVA, total, cabecera, etc.)
    val esProducto: Boolean = true,

    // Nivel de confianza con el que se identificó el producto:
    // "ean_exacto"     = identificado por EAN al 100%
    // "nombre_similar" = identificado por nombre, necesita confirmación
    // "nuevo"          = producto no conocido, requiere acción del usuario
    val confianzaIdentificacion: String = "nuevo",

    // Precio por kg o litro — calculado automáticamente si el producto
    // tiene peso/volumen definido. Permite detectar encarecimientos encubiertos
    val precioPorKgLitro: Double? = null,

    // Si este precio corresponde a una oferta puntual (2x1, 3x2, descuento)
    val esPrecioPromocional: Boolean = false,

    // Tipo de promoción: "2x1", "3x2", "descuento_pct", "pack"
    val tipoPromocion: String? = null,

    // Precio sin promoción, si se puede extraer del ticket
    val precioListaOriginal: Double? = null
)
