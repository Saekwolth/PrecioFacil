package com.preciofacil.app.parser

/**
 * Representa un producto detectado en el ticket.
 * En esta fase solo tiene nombre, EAN y precio.
 * En fases posteriores se añadirá cantidad y otros campos.
 */
data class ProductoDetectado(
    val nombre: String,
    val ean: String,          // Código de barras de 13 dígitos
    val precio: Double,
    val esDescuento: Boolean = false
)

/**
 * Resultado completo del parsing de un ticket.
 */
data class ResultadoParser(
    val supermercado: String,
    val productos: List<ProductoDetectado>,
    val totalTicket: Double,
    val textoOriginal: String
)

/**
 * ParserCaprabo — interpreta el texto OCR de un ticket de Caprabo La Massana.
 *
 * Estructura del ticket de Caprabo:
 *   - Cabecera: "caprabo", "La Massana", dirección, etc.
 *   - Productos: NOMBRE DEL PRODUCTO
 *                Ref: 8480010143690
 *   - Descuentos: "descompte" o "Bonificació Import" con precio negativo
 *   - Final de productos: línea "TOTALS"
 *   - Precios: aparecen al final del texto en columnas separadas
 *
 * Estrategia:
 *   1. Extraer nombres y EANs en orden (están juntos)
 *   2. Extraer precios en orden (aparecen separados al final)
 *   3. Emparejar por posición
 */
object ParserCaprabo {

    // Palabras que indican que una línea NO es un producto
    private val LINEAS_A_IGNORAR = setOf(
        "totals", "total a pagar", "e.f.t.", "targeta", "autorització",
        "autoritzacio", "visa debit", "visa", "client club caprabo",
        "imports en euros", "igi inclos", "igi inclòs", "rebut per al client",
        "operación sin conta", "ctos. firma no necesaria", "no s'admeten",
        "llevat estigui", "més informació", "caprabo la massana",
        "c/ costes de giberga", "grupcca", "nrt a-", "cai xa oberta",
        "caixa oberta", "uni", "€/un", "€tot", "et.c.", "eurtot",
        "eurcaprabo", "eurcarprabo", "data ", "autor", "seqüènci",
        "sequenci", "sequènci", "n.trans", "n.tran", "aplicació",
        "aplicacio", "resp:", "tvr:", "emv", "saldo en", "sal do en",
        "descompte", "bonificació import", "bonificacio import",
        "promos ", "% descompte", "total compra"
    )

    /**
     * Punto de entrada principal.
     * Recibe el texto crudo del OCR y devuelve los productos detectados.
     */
    fun parsear(textoOCR: String): ResultadoParser {
        val lineas = textoOCR.lines().map { it.trim() }.filter { it.isNotBlank() }

        val nombres = mutableListOf<String>()
        val eans = mutableListOf<String>()

        var dentroDeProductos = false
        var i = 0

        // ── PASO 1: extraer nombres y EANs ──────────────────────────
        while (i < lineas.size) {
            val linea = lineas[i]

            // Detectar inicio de zona de productos
            if (contienePalabrasClave(linea, listOf("caprabo00", "caprabo 00", "caixa oberta", "cai xa oberta"))) {
                dentroDeProductos = true
                i++
                continue
            }

            // Detectar fin de zona de productos
            if (linea.lowercase().startsWith("totals") || linea.lowercase().startsWith("total a pagar")) {
                break
            }

            if (dentroDeProductos) {
                // Línea de EAN
                if (linea.lowercase().startsWith("ref:")) {
                    val ean = extraerEAN(linea)
                    if (ean != null) {
                        eans.add(ean)
                        // El nombre ya debería estar en la lista
                        // Si no hay nombre para este EAN, añadir placeholder
                        if (eans.size > nombres.size) {
                            nombres.add("Producto desconocido")
                        }
                    }
                    i++
                    continue
                }

                // Línea de nombre de producto (si no es una línea a ignorar)
                if (!esLineaAIgnorar(linea)) {
                    // Limpiar cantidad al inicio (ej: "2 BOSSA PAPER" → "BOSSA PAPER")
                    val nombreLimpio = limpiarNombre(linea)
                    if (nombreLimpio.isNotBlank() && nombreLimpio.length > 2) {
                        nombres.add(nombreLimpio)
                    }
                }
            }

            i++
        }

        // ── PASO 2: extraer precios del texto completo ───────────────
        val precios = extraerPrecios(textoOCR)

        // ── PASO 3: extraer total del ticket ────────────────────────
        val total = extraerTotal(textoOCR)

        // ── PASO 4: emparejar productos con EANs y precios ──────────
        val productos = emparejarProductos(nombres, eans, precios)

        return ResultadoParser(
            supermercado = "Caprabo La Massana",
            productos = productos,
            totalTicket = total,
            textoOriginal = textoOCR
        )
    }

    // ── EXTRACCIÓN DE EAN ────────────────────────────────────────────

    /**
     * Extrae el código EAN de una línea como "Ref: 8480010143690"
     * Valida que tenga exactamente 13 dígitos y supera el dígito de control.
     */
    private fun extraerEAN(linea: String): String? {
        val soloDigitos = linea.replace("Ref:", "").replace(" ", "").trim()
        return if (soloDigitos.length == 13 && soloDigitos.all { it.isDigit() }) {
            soloDigitos
        } else {
            // Intentar extraer 13 dígitos seguidos de la línea
            val patron = Regex("\\d{13}")
            patron.find(linea)?.value
        }
    }

    // ── EXTRACCIÓN DE PRECIOS ────────────────────────────────────────

    /**
     * Extrae todos los precios del ticket.
     * Los precios en Caprabo aparecen como "1,99" o "10,50" — con coma decimal.
     * Se buscan en todo el texto para capturar los que están en columnas separadas.
     */
    private fun extraerPrecios(texto: String): List<Double> {
        val precios = mutableListOf<Double>()
        val lineas = texto.lines()

        // Buscar precios en la zona de precios (después de los nombres)
        // Un precio es un número con coma: 1,99 o 10,50 o 0,10
        val patronPrecio = Regex("^-?\\d{1,3},\\d{2}$")

        for (linea in lineas) {
            val lineaTrimmed = linea.trim()
            if (patronPrecio.matches(lineaTrimmed)) {
                val precio = lineaTrimmed.replace(",", ".").toDoubleOrNull()
                if (precio != null) {
                    precios.add(precio)
                }
            }
        }

        return precios
    }

    /**
     * Extrae el total del ticket buscando "TOTAL A PAGAR" o "6,67 EUR"
     */
    private fun extraerTotal(texto: String): Double {
        val lineas = texto.lines()
        for (linea in lineas) {
            if (linea.contains("EUR") && !linea.contains("EurCaprabo")) {
                val patron = Regex("(\\d+,\\d{2})\\s*EUR")
                val resultado = patron.find(linea)
                if (resultado != null) {
                    return resultado.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
                }
            }
        }
        return 0.0
    }

    // ── EMPAREJAMIENTO ───────────────────────────────────────────────

    /**
     * Empareja nombres con EANs y precios por orden de aparición.
     * Si hay más nombres que EANs o precios, los productos sin datos
     * quedan con EAN vacío o precio 0.
     */
    private fun emparejarProductos(
        nombres: List<String>,
        eans: List<String>,
        precios: List<Double>
    ): List<ProductoDetectado> {
        val productos = mutableListOf<ProductoDetectado>()

        // Usar el número de EANs como referencia (es el dato más fiable)
        val numProductos = minOf(nombres.size, eans.size)

        for (idx in 0 until numProductos) {
            val nombre = nombres.getOrElse(idx) { "Producto ${idx + 1}" }
            val ean = eans.getOrElse(idx) { "" }
            val precio = precios.getOrElse(idx) { 0.0 }

            productos.add(
                ProductoDetectado(
                    nombre = nombre,
                    ean = ean,
                    precio = precio,
                    esDescuento = precio < 0
                )
            )
        }

        return productos
    }

    // ── UTILIDADES ───────────────────────────────────────────────────

    private fun esLineaAIgnorar(linea: String): Boolean {
        val lineaLower = linea.lowercase()
        return LINEAS_A_IGNORAR.any { lineaLower.contains(it) }
            || linea.matches(Regex("\\d{4,}"))      // solo números largos (códigos)
            || linea.matches(Regex("-?\\d{1,3},\\d{2}")) // precio suelto
            || linea.length < 3                      // línea demasiado corta
    }

    private fun limpiarNombre(linea: String): String {
        // Quitar cantidad al inicio: "2 BOSSA PAPER" → "BOSSA PAPER"
        // También: "1,085 PLATAN AMERICA" → "PLATAN AMERICA"
        return linea.replace(Regex("^\\d+[,.]?\\d*\\s+"), "").trim()
    }

    private fun contienePalabrasClave(linea: String, palabras: List<String>): Boolean {
        val lineaLower = linea.lowercase()
        return palabras.any { lineaLower.contains(it.lowercase()) }
    }
}
