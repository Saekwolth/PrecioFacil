package com.preciofacil.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla: productos
 * Guarda todos los productos conocidos por la app.
 * El código EAN es el identificador principal para reconocer
 * un producto independientemente del supermercado o del nombre en el ticket.
 */
@Entity(tableName = "productos")
data class Producto(

    // Identificador único — Room lo genera automáticamente
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Nombre limpio y legible del producto (ej: "Leche entera 1L")
    // Lo asigna el usuario la primera vez que aparece el producto
    val nombreNormalizado: String,

    // Código EAN de 13 dígitos — identificador principal
    // Permite reconocer el producto en cualquier supermercado del mundo
    val codigoEan: String = "",

    // Categoría del producto (ej: "Lácteos", "Panadería", "Limpieza")
    val categoria: String = "",

    // Si el usuario lo compra con regularidad
    val esHabitual: Boolean = false,

    // Si se generan alertas de precio para este producto
    val avisosActivados: Boolean = true,

    // Variantes del nombre tal como aparecen en tickets
    // Separadas por coma (ej: "LECH ENT 1L,LECHE ENTERA,Leche Ent.")
    val alias: String = "",

    // Tipo de IVA aplicable cuando se compra en España
    // "superreducido_4" = alimentos básicos (leche, pan, huevos, fruta)
    // "reducido_10"     = alimentos en general (yogur, embutidos, conservas)
    // "general_21"      = limpieza, higiene, bebidas alcohólicas
    val tipoIvaEspana: String = "reducido_10",

    // Si el usuario ha confirmado manualmente el tipo de IVA
    // Si es false y la categoría es ambigua, la app avisa al usuario
    val ivaRevisadoPorUsuario: Boolean = false,

    // Peso o volumen del envase (ej: 1000 para 1L, 550 para 550g)
    // Permite calcular precio por kg/litro y detectar encarecimientos encubiertos
    val pesoVolumen: Double? = null,

    // Unidad del peso/volumen: "g", "kg", "ml", "l", "ud"
    val unidadMedida: String = "ud",

    // Si el producto tiene comportamiento estacional
    val esTemporada: Boolean = false,

    // Mes de inicio de temporada (1-12). Nulo si no es de temporada
    val temporadaMesInicio: Int? = null,

    // Mes de fin de temporada (1-12). Nulo si no es de temporada
    val temporadaMesFin: Int? = null,

    // Si la temporada la calculó la app (true) o la configuró el usuario (false)
    val temporadaAprendida: Boolean = false,

    // Si este producto es una versión reformateada de otro
    // (mismo producto, distinto tamaño o EAN)
    // Permite detectar encarecimientos encubiertos por reducción de formato
    val productoVinculadoId: Long? = null
)
