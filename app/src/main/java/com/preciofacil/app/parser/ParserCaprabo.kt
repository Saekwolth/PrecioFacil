package com.preciofacil.app.parser

import java.io.Serializable

data class ProductoDetectado(
    val nombre: String,
    val ean: String,
    val precio: Double,
    val esDescuento: Boolean = false
) : Serializable

data class ResultadoParser(
    val supermercado: String,
    val productos: List<ProductoDetectado>,
    val totalTicket: Double,
    val textoOriginal: String
) : Serializable

/**
 * ParserCaprabo v3 — robusto frente a distintos formatos de ticket Caprabo y Super U.
 *
 * Estrategia: localizar TODAS las líneas "Ref:" del texto,
 * para cada una buscar el nombre en las líneas anteriores,
 * y los precios en las líneas sueltas de formato decimal.
 *
 * Mejoras v3:
 * - No necesita marcador de cabecera para arrancar
 * - Acepta EAN-13, EAN-8 y códigos internos cortos (granel)
 * - Corrige confusiones OCR en EANs (I→1, O→0, S→5, etc.)
 * - Detecta supermercado automáticamente
 */
object ParserCaprabo {

    private val PALABRAS_A_IGNORAR = listOf(
        "totals", "total a pagar", "total compra", "e.f.t", "targeta",
        "autoritz", "visa", "client club", "imports en euros", "igi inclos",
        "igi inclòs", "rebut per", "operaci", "ctos.", "no s'admet",
        "llevat", "informac", "caprabo", "c/ costes", "grupcca", "nrt a-",
        "caixa", "cai xa", "calxe", "€/un", "€tot", "eurtot",
        "eurcarprabo", "eurcaprabo", "data ", "autor", "seqüènci", "seguenct",
        "sequenci", "n.trans", "n.tran", "aplicació", "aplicacto", "aplfcacto",
        "resp:", "tvr:", "emv", "saldo", "sal do", "descompte", "bonificació",
        "bonificacio", "promos", "% descompte", "ad400", "centre comercial",
        "descompte per", "firma no", "en cclub", "ww.", "grpcca",
        "articles", "venda client", "codi :", "descripcio", "quantitat",
        "import", "fidelia", "n.tarjeta", "seq sncia", "data operaci",
        "hora operaci", "euros fidelia", "signatura", "operacio amb pin",
        "pres. acumulats", "total descomptes", "fidelia pg",
        "av.meritxell", "av. meritxell", "super u", "superu"
    )

    fun parsear(textoOCR: String): ResultadoParser {
        val lineas = textoOCR.lines().map { it.trim() }

        // ── PASO 1: localizar todas las líneas Ref: ──────────────────
        data class EntradaRef(val posicion: Int, val ean: String)
        val entradasRef = mutableListOf<EntradaRef>()

        lineas.forEachIndexed { idx, linea ->
            val lineaLower = linea.lowercase()
            if (lineaLower.startsWith("ref:") || lineaLower.startsWith("ref ")) {
                val ean = extraerCodigoRef(linea)
                if (ean != null) {
                    entradasRef.add(EntradaRef(idx, ean))
                }
            }
        }

        if (entradasRef.isEmpty()) {
            return ResultadoParser(
                supermercado = detectarSupermercado(textoOCR),
                productos = emptyList(),
                totalTicket = extraerTotal(textoOCR),
                textoOriginal = textoOCR
            )
        }

        // ── PASO 2: para cada Ref:, buscar nombre en líneas anteriores
        val productos = mutableListOf<ProductoDetectado>()
        val preciosGlobales = extraerPrecios(textoOCR)
        var indicePrecio = 0

        entradasRef.forEachIndexed { idxRef, entrada ->
            val posInicio = if (idxRef == 0) 0 else entradasRef[idxRef - 1].posicion + 1
            val posBusqueda = maxOf(posInicio, entrada.posicion - 4)

            var nombre = ""
            for (pos in posBusqueda until entrada.posicion) {
                val candidato = lineas.getOrElse(pos) { "" }
                if (candidato.isNotBlank() && !esLineaAIgnorar(candidato)) {
                    val limpio = limpiarNombre(candidato)
                    if (limpio.length > 2) {
                        nombre = limpio
                    }
                }
            }

            if (nombre.isBlank()) nombre = "Producto ${idxRef + 1}"

            val precio = preciosGlobales.getOrElse(indicePrecio) { 0.0 }
            if (indicePrecio < preciosGlobales.size) indicePrecio++

            productos.add(
                ProductoDetectado(
                    nombre = nombre,
                    ean = entrada.ean,
                    precio = precio,
                    esDescuento = precio < 0
                )
            )
        }

        return ResultadoParser(
            supermercado = detectarSupermercado(textoOCR),
            productos = productos,
            totalTicket = extraerTotal(textoOCR),
            textoOriginal = textoOCR
        )
    }

    // ── DETECCIÓN DE SUPERMERCADO ────────────────────────────────────
    private fun detectarSupermercado(texto: String): String {
        val lower = texto.lowercase()
        return when {
            lower.contains("super u") || lower.contains("superu") -> "Super U"
            lower.contains("mercadona") -> "Mercadona"
            lower.contains("caprabo") -> "Caprabo La Massana"
            else -> "Supermercado"
        }
    }

    // ── EXTRACCIÓN DE CÓDIGO REF ─────────────────────────────────────
    /**
     * Extrae el código de una línea "Ref: XXXX"
     * Corrige confusiones OCR: I→1, O→0, S→5, G→6, Z→2
     * Acepta EAN-13, EAN-8 y códigos internos cortos (4-6 dígitos)
     */
    private fun extraerCodigoRef(linea: String): String? {
        val sinRef = linea
            .replace(Regex("(?i)ref:?\\s*"), "")
            .trim()

        // Primero intentar con solo dígitos (caso limpio)
        val soloDigitos = sinRef.filter { it.isDigit() }

        // Si no hay suficientes dígitos, aplicar correcciones OCR
        val candidato = if (soloDigitos.length < 4) {
            sinRef
                .replace('I', '1').replace('l', '1').replace('|', '1')
                .replace('O', '0').replace('o', '0')
                .replace('S', '5').replace('G', '6').replace('Z', '2')
                .filter { it.isDigit() }
        } else {
            soloDigitos
        }

        return when (candidato.length) {
            13 -> candidato
            8  -> candidato
            in 4..6 -> candidato
            else -> if (candidato.length > 13) candidato.take(13) else null
        }
    }

    // ── EXTRACCIÓN DE PRECIOS ────────────────────────────────────────
    private fun extraerPrecios(texto: String): List<Double> {
        val precios = mutableListOf<Double>()
        val patron = Regex("^-?\\d{1,3},\\d{2}$")
        for (linea in texto.lines()) {
            val l = linea.trim()
            if (patron.matches(l)) {
                l.replace(",", ".").toDoubleOrNull()?.let { precios.add(it) }
            }
        }
        return precios
    }

    // ── EXTRACCIÓN DEL TOTAL ─────────────────────────────────────────
    private fun extraerTotal(texto: String): Double {
        for (linea in texto.lines()) {
            val l = linea.uppercase()
            if (l.contains("TOTAL A PAGAR") || (l.contains("TOTAL") && !l.contains("TOTALS"))) {
                val res = Regex("(\\d+,\\d{2})").find(linea)
                if (res != null) {
                    val valor = res.groupValues[1].replace(",", ".").toDoubleOrNull() ?: continue
                    if (valor > 1.0) return valor
                }
            }
        }
        for (linea in texto.lines()) {
            if (linea.contains("EUR", ignoreCase = true) &&
                !linea.contains("EurCaprabo", ignoreCase = true) &&
                !linea.contains("Fidelia", ignoreCase = true)) {
                val res = Regex("(\\d+,\\d{2})\\s*EUR").find(linea)
                if (res != null) return res.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
            }
        }
        return 0.0
    }

    // ── UTILIDADES ───────────────────────────────────────────────────
    private fun esLineaAIgnorar(linea: String): Boolean {
        val l = linea.lowercase()
        if (PALABRAS_A_IGNORAR.any { l.contains(it) }) return true
        if (linea.matches(Regex("\\d{5,}"))) return true
        if (linea.matches(Regex("-?\\d{1,3},\\d{2}"))) return true
        if (linea.matches(Regex("[A-Z0-9*]{1,4}"))) return true
        if (linea.length < 3) return true
        if (linea.count { it == '|' || it == 'I' } > 4) return true
        return false
    }

    private fun limpiarNombre(linea: String): String {
        var r = linea
        r = r.replace(Regex("^\\d+[,.]\\d+\\s*"), "")
        r = r.replace(Regex("^\\d+\\s+"), "")
        r = r.replace(Regex("\\s+\\d{1,2},\\d{2}\\s+\\d{1,2},\\d{2}$"), "")
        r = r.replace(Regex("\\s+\\d{1,2},\\d{2}$"), "")
        return r.trim()
    }
}
