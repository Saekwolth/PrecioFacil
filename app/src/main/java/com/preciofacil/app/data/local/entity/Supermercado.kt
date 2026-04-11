package com.preciofacil.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla: supermercados
 * Guarda los supermercados que el usuario visita habitualmente.
 */
@Entity(tableName = "supermercados")
data class Supermercado(

    // Identificador único — Room lo genera automáticamente
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Nombre del supermercado (ej: "Caprabo Massana")
    val nombre: String,

    // Palabras que aparecen en sus tickets para identificarlo automáticamente
    // Separadas por coma (ej: "CAPRABO,CAPRABO MASSANA,EurCaprabo")
    val palabrasClave: String = "",

    // Color para identificarlo visualmente en la app (ej: "#3B6D11")
    val color: String = "#888888",

    // Si el usuario lo usa actualmente
    val activo: Boolean = true,

    // País: "AD" (Andorra) o "ES" (España)
    val pais: String = "AD",

    // Sistema fiscal: "IGI_AD" o "IVA_ES"
    val tipoImpuesto: String = "IGI_AD",

    // Si los precios del ticket incluyen impuesto
    val aplicaImpuesto: Boolean = true,

    // Perfil de parser para leer sus tickets
    // "CAPRABO", "SUPER_U", "MERCADONA", "GENERICO", etc.
    val perfilParser: String = "GENERICO"
)
