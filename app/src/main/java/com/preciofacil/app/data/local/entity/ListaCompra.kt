package com.preciofacil.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla: lista_compra
 * Guarda los productos de la lista de la compra activa.
 * Se genera automáticamente a partir de los hábitos,
 * y el usuario puede añadir o eliminar productos manualmente.
 */
@Entity(
    tableName = "lista_compra",
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
            childColumns = ["supermercadoRecomendadoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("productoId"), Index("supermercadoRecomendadoId")]
)
data class ListaCompra(

    // Identificador único — Room lo genera automáticamente
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Producto de la lista
    val productoId: Long,

    // Cantidad que se quiere comprar
    val cantidadDeseada: Int = 1,

    // Supermercado recomendado donde está más barato
    val supermercadoRecomendadoId: Long? = null,

    // Último precio conocido del producto en el supermercado recomendado
    // Permite mostrar el coste estimado total de la lista
    val precioEstimado: Double? = null,

    // Si fue añadido por la app (a partir de hábitos) o por el usuario
    val añadidoAutomaticamente: Boolean = false,

    // Estado del producto en la lista:
    // "pendiente"       = aún no comprado
    // "comprado"        = ya en el carrito o en casa
    // "no_habia_stock"  = el usuario fue pero no había
    // "omitido"         = el usuario decidió no comprarlo esta vez
    val estado: String = "pendiente",

    // Cuándo se añadió a la lista (milisegundos)
    val fechaAdicion: Long = System.currentTimeMillis(),

    // Identificador del hogar — para sincronizar con Firebase
    // La lista es compartida entre todos los miembros del hogar
    val codigoHogar: String = ""
)
