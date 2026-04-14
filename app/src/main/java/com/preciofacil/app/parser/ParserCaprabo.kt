package com.preciofacil.app.parser

data class ProductoDetectado(
    val nombre: String,
    val ean: String,
    val precio: Double,
    val esDescuento: Boolean = false
)

data class ResultadoParser(
    val supermercado: String,
    val productos: List<ProductoDetectado>,
    val totalTicket: Double,
    val textoOriginal: String
)

/**
 * ParserCaprabo v2 — robusto frente al OCR caótico de tickets largos.
 *
 * Estrategia:
 *  - En lugar de buscar una zona de inicio, recorre TODO el texto
 *  - Cada vez que encuentra un EAN válido (Ref: + 13 dígitos), busca el nombre
 *    en las líneas anteriores
 *  - Los precios se recogen de las líneas sueltas con formato decimal
 *  - Se ignoran EANs con letras pegadas (Ref: 8480010359276E → se limpia)
 */
object ParserCaprabo {

    // Líneas que nunca son nombres de producto
    private val PALABRAS_A_IGNORAR = listOf(
        "totals", "total a pagar", "total compra", "e.f.t", "targeta",
        "autoritz", "visa", "client club", "imports en euros", "igi inclos",
        "igi inclòs", "rebut per", "operaci", "ctos.", "no s'admet",
        "llevat", "informac", "caprabo", "c/ costes", "grupcca", "nrt a-",
        "caixa", "cai xa", "calxe", "uni\n", "€/un", "€tot", "eurtot",
        "eurcarprabo", "eurcaprabo", "data ", "autor", "seqüènci", "seguenct",
        "sequenci", "n.trans", "n.tran", "aplicació", "aplicacto", "aplfcacto",
        "resp:", "tvr:", "emv", "saldo", "sal do", "descompte", "bonificació",
        "bonificacio", "promos", "% descompte", "ad400", "centre comercial",
        "int a", "wet\n", "rot\n", "re\n", "ket:", "ret:", "ref:toco",
        "descompte per", "btmeusu", "bse glute", "allaoa", "oderacion",
        "oderacts", "oderacton", "firma no", "saldo en", "en cclub",
        "8stat", "ww.", "grpcca", "la massana\n", "massena"
    )

    fun parsear(textoOCR: String): ResultadoParser {
        val lineas = textoOCR.lines().map { it.trim() }

        // ── PASO 1: encontrar todos los EANs válidos y sus posiciones ──
        data class EntradaEAN(val posicion: Int, val ean: String)

        val entradasEAN = mutableListOf<EntradaEAN>()

        lineas.forEachIndexed { idx, linea ->
            if (linea.lowercase().startsWith("ref:")) {
                val ean = extraerEAN(linea)
                if (ean != null) {
                    entradasEAN.add(EntradaEAN(idx, ean))
                }
            }
        }

        // ── PASO 2: para cada EAN, buscar el nombre en líneas anteriores ──
        val productos = mutableListOf<ProductoDetectado>()
        val preciosGlobales = extraerPrecios(textoOCR)
        var indicePrecio = 0

        entradasEAN.forEachIndexed { idxEAN, entrada ->
            // El nombre está en las 1-3 líneas antes del Ref:
            val posInicio = if (idxEAN == 0) 0 else entradasEAN[idxEAN - 1].posicion + 1
            val posBusqueda = maxOf(posInicio, entrada.posicion - 3)

            var nombre = ""
            for (pos in posBusqueda until entrada.posicion) {
                val candidato = lineas.getOrElse(pos) { "" }
                if (candidato.isNotBlank() && !esLineaAIgnorar(candidato)) {
                    val candidatoLimpio = limpiarNombre(candidato)
                    if (candidatoLimpio.length > 2) {
                        nombre = candidatoLimpio
                    }
                }
            }

            if (nombre.isBlank()) nombre = "Producto ${idxEAN + 1}"

            // Asignar el siguiente precio disponible
            val precio = preciosGlobales.getOrElse(indicePrecio) { 0.0 }
            if (preciosGlobales.size > indicePrecio) indicePrecio++

            productos.add(
                ProductoDetectado(
                    nombre = nombre,
                    ean = entrada.ean,
                    precio = precio,
                    esDescuento = precio < 0
                )
            )
        }

        val total = extraerTotal(textoOCR)

        return ResultadoParser(
            supermercado = "Caprabo La Massana",
            productos = productos,
            totalTicket = total,
            textoOriginal = textoOCR
        )
    }

    // ── EXTRACCIÓN DE EAN ────────────────────────────────────────────
    // Acepta "Ref: 8480010143690" y también "Ref: 8480010143690E" (limpia la letra)
    private fun extraerEAN(linea: String): String? {
        val sinRef = linea.replace("Ref:", "").replace("Rof:", "").trim()
        // Extraer solo dígitos consecutivos
        val soloDigitos = Regex("\\d+").find(sinRef)?.value ?: return null
        return if (soloDigitos.length == 13) soloDigitos else null
    }

    // ── EXTRACCIÓN DE PRECIOS ────────────────────────────────────────
    // Busca líneas que sean solo un precio: "1,99" o "-0,60" o "10,50"
    private fun extraerPrecios(texto: String): List<Double> {
        val precios = mutableListOf<Double>()
        val patron = Regex("^-?\\d{1,3},\\d{2}$")
        for (linea in texto.lines()) {
            val l = linea.trim()
            if (patron.matches(l)) {
                // Ignorar precios que parecen ser el total u otros totales grandes
                val valor = l.replace(",", ".").toDoubleOrNull() ?: continue
                precios.add(valor)
            }
        }
        return precios
    }

    // ── EXTRACCIÓN DEL TOTAL ─────────────────────────────────────────
    private fun extraerTotal(texto: String): Double {
        // Buscar "TOTAL A PAGAR 105,67€" o "105,67 EUR"
        for (linea in texto.lines()) {
            if (linea.contains("TOTAL A PAGAR", ignoreCase = true)) {
                val res = Regex("(\\d+,\\d{2})").find(linea)
                if (res != null) return res.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
            }
        }
        for (linea in texto.lines()) {
            if (linea.contains("EUR") && !linea.contains("EurCaprabo", ignoreCase = true)) {
                val res = Regex("(\\d+,\\d{2})\\s*EUR").find(linea)
                if (res != null) return res.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
            }
        }
        return 0.0
    }

    // ── UTILIDADES ───────────────────────────────────────────────────
    private fun esLineaAIgnorar(linea: String): Boolean {
        val l = linea.lowercase()
        if (PALABRAS_A_IGNORAR.any { l.contains(it.trimEnd()) }) return true
        if (linea.matches(Regex("\\d{5,}"))) return true       // número largo sin Ref
        if (linea.matches(Regex("-?\\d{1,3},\\d{2}"))) return true  // precio suelto
        if (linea.matches(Regex("[A-Z0-9]{1,3}"))) return true  // código corto
        if (linea.length < 3) return true
        return false
    }

    private fun limpiarNombre(linea: String): String {
        // Quitar cantidad+precio_unitario al inicio: "0,932PIT PLLSTRE" → "PIT PLLSTRE"
        // O "2 LINDT XOCOLATA" → "LINDT XOCOLATA"
        var resultado = linea.replace(Regex("^\\d+[,.]\\d+\\s*"), "")
        resultado = resultado.replace(Regex("^\\d+\\s+"), "")
        // Quitar precio pegado al final del nombre: "GALLINA BLANCA BROU 2,19" → "GALLINA BLANCA BROU"
        resultado = resultado.replace(Regex("\\s+\\d{1,2},\\d{2}$"), "")
        return resultado.trim()
    }
}
