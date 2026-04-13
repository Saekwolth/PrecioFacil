package com.preciofacil.app.data.local.seed

import com.preciofacil.app.data.local.database.PrecioFacilDatabase
import com.preciofacil.app.data.local.entity.Supermercado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DatabaseSeeder — precarga los datos iniciales en la base de datos.
 *
 * Se ejecuta una sola vez cuando la app se instala por primera vez.
 * Añade los supermercados conocidos para que el usuario no tenga
 * que configurarlos manualmente.
 *
 * Datos extraídos de tickets reales analizados:
 * - Caprabo La Massana (Andorra) — tickets agosto 2025 / abril 2026
 * - Mercadona La Seu d'Urgell (España) — ticket febrero 2026
 */
object DatabaseSeeder {

    /**
     * Ejecuta la precarga solo si la base de datos está vacía.
     * Si ya tiene supermercados, no hace nada (evita duplicados).
     */
    suspend fun inicializarSiEsNecesario(database: PrecioFacilDatabase) {
        withContext(Dispatchers.IO) {
            val dao = database.supermercadoDao()
            val total = dao.contarTotal()
            if (total == 0) {
                supermercadosIniciales().forEach { dao.insertar(it) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SUPERMERCADOS INICIALES
    // ─────────────────────────────────────────────────────────────

    private fun supermercadosIniciales(): List<Supermercado> = listOf(

        // ── CAPRABO LA MASSANA ────────────────────────────────────
        // Andorra — IGI incluido en precios
        // Datos verificados en tickets reales de agosto 2025 y abril 2026
        Supermercado(
            nombre = "Caprabo La Massana",
            palabrasClave = "CAPRABO,GRUPCCA,EurCaprabo,IGI INCLÒS,A-700527-F",
            color = "#006633",
            activo = true,
            pais = "AD",
            tipoImpuesto = "IGI_AD",
            aplicaImpuesto = true
        ),

        // ── MERCADONA LA SEU D'URGELL ─────────────────────────────
        // España — IVA desglosado en el ticket (4% y 10%)
        // Datos verificados en ticket de febrero 2026
        // Dirección: Avda. Les Valls d'Andorra, S/N — 25700 La Seu d'Urgell
        Supermercado(
            nombre = "Mercadona La Seu d'Urgell",
            palabrasClave = "MERCADONA,HACENDADO,A-46103834",
            color = "#1E8449",
            activo = true,
            pais = "ES",
            tipoImpuesto = "IVA_ES",
            aplicaImpuesto = true
        ),

        // ── SUPER U ───────────────────────────────────────────────
        // Andorra — pendiente de analizar tickets reales
        // Se añade como plantilla para cuando se tengan tickets
        Supermercado(
            nombre = "Super U",
            palabrasClave = "SUPER U,SYSTEME U",
            color = "#E53935",
            activo = true,
            pais = "AD",
            tipoImpuesto = "IGI_AD",
            aplicaImpuesto = true
        )
    )
}
