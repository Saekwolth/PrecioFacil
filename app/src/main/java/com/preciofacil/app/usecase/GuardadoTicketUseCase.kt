package com.preciofacil.app.usecase

import com.preciofacil.app.data.local.dao.LineaTicketDao
import com.preciofacil.app.data.local.dao.ProductoDao
import com.preciofacil.app.data.local.dao.RegistroPrecioDao
import com.preciofacil.app.data.local.dao.TicketDao
import com.preciofacil.app.data.local.entity.LineaTicket
import com.preciofacil.app.data.local.entity.Producto
import com.preciofacil.app.data.local.entity.RegistroPrecio
import com.preciofacil.app.data.local.entity.Ticket
import com.preciofacil.app.data.remote.HogarManager
import com.preciofacil.app.parser.ProductoDetectado

/**
 * GuardadoTicketUseCase — orquesta el guardado completo de un ticket revisado.
 *
 * Cuando el usuario pulsa "Confirmar y guardar", este caso de uso:
 *
 * 1. Crea el Ticket en Room
 * 2. Para cada producto confirmado:
 *    a. Busca si ya existe en la BD por EAN
 *    b. Si no existe, lo crea como producto nuevo
 *    c. Crea la LineaTicket vinculando ticket ↔ producto
 *    d. Crea un RegistroPrecio con el precio de hoy
 * 3. Devuelve el ID del ticket guardado
 *
 * Todo ocurre en Room (offline-first). La sincronización con Firebase
 * se añadirá en una fase posterior.
 */
class GuardadoTicketUseCase(
    private val ticketDao: TicketDao,
    private val lineaTicketDao: LineaTicketDao,
    private val productoDao: ProductoDao,
    private val registroPrecioDao: RegistroPrecioDao,
    private val hogarManager: HogarManager
) {

    /**
     * Guarda el ticket completo y devuelve el ID asignado.
     *
     * @param supermercadoId  ID del supermercado en Room
     * @param totalTicket     Importe total del ticket
     * @param textoOcr        Texto crudo del OCR (para referencia futura)
     * @param productos       Lista de productos confirmados por el usuario
     */
    suspend fun guardar(
        supermercadoId: Long,
        totalTicket: Double,
        textoOcr: String,
        productos: List<ProductoDetectado>
    ): Long {

        val codigoHogar = hogarManager.obtenerCodigoHogar() ?: ""

        // ── PASO 1: crear el ticket ──────────────────────────────────
        val ticket = Ticket(
            supermercadoId = supermercadoId,
            fecha = System.currentTimeMillis(),
            total = totalTicket,
            estado = "procesado",
            textoOcrRaw = textoOcr,
            codigoHogar = codigoHogar
        )
        val ticketId = ticketDao.insertar(ticket)

        // ── PASO 2: procesar cada producto ───────────────────────────
        val lineas = mutableListOf<LineaTicket>()

        for (productoDetectado in productos) {

            // 2a. Buscar si el producto ya existe por EAN
            var productoEnBD = if (productoDetectado.ean.isNotBlank()) {
                productoDao.buscarPorEan(productoDetectado.ean)
            } else null

            // 2b. Si no existe, crear producto nuevo
            if (productoEnBD == null) {
                val nombreNormalizado = productoDetectado.nombre
                    .lowercase()
                    .trim()

                val nuevoProducto = Producto(
                    codigoEan = productoDetectado.ean,
                    nombreNormalizado = nombreNormalizado,
                    categoria = "",
                    esHabitual = false
                )
                val productoId = productoDao.insertar(nuevoProducto)
                productoEnBD = nuevoProducto.copy(id = productoId)
            }

            // 2c. Crear la línea del ticket
            val linea = LineaTicket(
                ticketId = ticketId,
                textoOriginalOcr = productoDetectado.nombre,
                productoId = productoEnBD.id,
                precioTotal = productoDetectado.precio,
                precioUnitario = productoDetectado.precio,
                precioConImpuesto = productoDetectado.precio,
                // Para Andorra: dividir entre 1.045 para quitar el IGI del 4,5%
                precioSinImpuesto = productoDetectado.precio / 1.045,
                tipoImpuestoAplicado = "IGI_AD_4.5",
                cantidad = 1,
                revisadoPorUsuario = true,
                esProducto = true,
                confianzaIdentificacion = if (productoDetectado.ean.isNotBlank()) "ean_exacto" else "nuevo",
                esPrecioPromocional = productoDetectado.esDescuento
            )
            lineas.add(linea)

            // 2d. Crear registro de precio en el historial
            val registro = RegistroPrecio(
                productoId = productoEnBD.id,
                supermercadoId = supermercadoId,
                ticketId = ticketId,
                fecha = System.currentTimeMillis(),
                precioConImpuesto = productoDetectado.precio,
                precioSinImpuesto = productoDetectado.precio / 1.045,
                tipoImpuestoAplicado = "IGI_AD_4.5",
                esPrecioPromocional = productoDetectado.esDescuento
            )
            registroPrecioDao.insertar(registro)
        }

        // ── PASO 3: guardar todas las líneas de golpe ────────────────
        lineaTicketDao.insertarVarias(lineas)

        return ticketId
    }
}
