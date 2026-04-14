package com.preciofacil.app.usecase

import com.preciofacil.app.data.local.dao.LineaTicketDao
import com.preciofacil.app.data.local.dao.ProductoDao
import com.preciofacil.app.data.local.dao.RegistroPrecioDao
import com.preciofacil.app.data.local.dao.TicketDao
import com.preciofacil.app.data.local.dao.AlertaDao
import com.preciofacil.app.data.local.entity.LineaTicket
import com.preciofacil.app.data.local.entity.Producto
import com.preciofacil.app.data.local.entity.RegistroPrecio
import com.preciofacil.app.data.local.entity.Ticket
import com.preciofacil.app.data.remote.HogarManager
import com.preciofacil.app.parser.ProductoDetectado

/**
 * GuardadoTicketUseCase — orquesta el guardado completo de un ticket revisado.
 *
 * Cuando el usuario pulsa "Confirmar y guardar":
 * 1. Crea el Ticket en Room
 * 2. Para cada producto: busca o crea en BD, crea LineaTicket y RegistroPrecio
 * 3. Llama a AlertaUseCase para generar alertas de variación de precio
 */
class GuardadoTicketUseCase(
    private val ticketDao: TicketDao,
    private val lineaTicketDao: LineaTicketDao,
    private val productoDao: ProductoDao,
    private val registroPrecioDao: RegistroPrecioDao,
    private val alertaDao: AlertaDao,
    private val hogarManager: HogarManager
) {

    suspend fun guardar(
        supermercadoId: Long,
        totalTicket: Double,
        textoOcr: String,
        productos: List<ProductoDetectado>,
        nombreSupermercado: String = "Supermercado"
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
        val registros = mutableListOf<RegistroPrecio>()

        for (productoDetectado in productos) {

            // Buscar si el producto ya existe por EAN
            var productoEnBD = if (productoDetectado.ean.isNotBlank()) {
                productoDao.buscarPorEan(productoDetectado.ean)
            } else null

            // Si no existe, crear producto nuevo
            if (productoEnBD == null) {
                val nuevoProducto = Producto(
                    codigoEan = productoDetectado.ean,
                    nombreNormalizado = productoDetectado.nombre.lowercase().trim(),
                    categoria = "",
                    esHabitual = false
                )
                val productoId = productoDao.insertar(nuevoProducto)
                productoEnBD = nuevoProducto.copy(id = productoId)
            }

            // Crear la línea del ticket
            val linea = LineaTicket(
                ticketId = ticketId,
                textoOriginalOcr = productoDetectado.nombre,
                productoId = productoEnBD.id,
                precioTotal = productoDetectado.precio,
                precioUnitario = productoDetectado.precio,
                precioConImpuesto = productoDetectado.precio,
                precioSinImpuesto = productoDetectado.precio / 1.045,
                tipoImpuestoAplicado = "IGI_AD_4.5",
                cantidad = 1,
                revisadoPorUsuario = true,
                esProducto = true,
                confianzaIdentificacion = if (productoDetectado.ean.isNotBlank()) "ean_exacto" else "nuevo",
                esPrecioPromocional = productoDetectado.esDescuento
            )
            lineas.add(linea)

            // Crear registro de precio
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
            val registroId = registroPrecioDao.insertar(registro)
            registros.add(registro.copy(id = registroId))
        }

        // ── PASO 3: guardar todas las líneas ─────────────────────────
        lineaTicketDao.insertarVarias(lineas)

        // ── PASO 4: generar alertas de variación de precio ───────────
        val alertaUseCase = AlertaUseCase(registroPrecioDao, alertaDao, productoDao)
        alertaUseCase.generarAlertas(
            nuevosRegistros = registros,
            supermercadoId = supermercadoId,
            ticketId = ticketId,
            nombreSuper = nombreSupermercado
        )

        return ticketId
    }
}
