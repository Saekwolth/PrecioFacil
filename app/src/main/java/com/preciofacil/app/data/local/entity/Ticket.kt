package com.preciofacil.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla: tickets
 * Representa cada ticket de compra escaneado por el usuario.
 * Cada ticket pertenece a un supermercado y contiene varias líneas de producto.
 */
@Entity(
    tableName = "tickets",
    foreignKeys = [
        ForeignKey(
            entity = Supermercado::class,
            parentColumns = ["id"],
            childColumns = ["supermercadoId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("supermercadoId")]
)
data class Ticket(

    // Identificador único — Room lo genera automáticamente
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // A qué supermercado corresponde este ticket
    val supermercadoId: Long,

    // Fecha de la compra en milisegundos (formato estándar de Android)
    val fecha: Long = System.currentTimeMillis(),

    // Importe total del ticket
    val total: Double = 0.0,

    // Ruta local donde se guarda la foto del ticket en el móvil
    // Las fotos NO se suben a la nube — solo se guardan en el móvil que escaneó
    val rutaFoto: String = "",

    // Estado del ticket en el flujo de revisión:
    // "pendiente_revision" = escaneado pero no revisado por el usuario
    // "revisado"           = el usuario ha confirmado las líneas
    // "procesado"          = guardado definitivamente en la base de datos
    val estado: String = "pendiente_revision",

    // Texto completo tal como lo leyó el OCR, sin procesar
    // Se guarda para referencia y para mejorar el parser en el futuro
    val textoOcrRaw: String = "",

    // Identificador del hogar al que pertenece este ticket
    // Usado para sincronizar con Firebase entre los móviles del hogar
    val codigoHogar: String = ""
)
