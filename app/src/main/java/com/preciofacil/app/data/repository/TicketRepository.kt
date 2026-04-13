package com.preciofacil.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.preciofacil.app.data.local.dao.LineaTicketDao
import com.preciofacil.app.data.local.dao.RegistroPrecioDao
import com.preciofacil.app.data.local.dao.TicketDao
import com.preciofacil.app.data.local.entity.LineaTicket
import com.preciofacil.app.data.local.entity.RegistroPrecio
import com.preciofacil.app.data.local.entity.Ticket
import com.preciofacil.app.data.remote.HogarManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * TicketRepository — gestiona tickets, líneas y registros de precio.
 *
 * Un ticket es el documento central de la app:
 * - Ticket: la cabecera (supermercado, fecha, total)
 * - LineaTicket: cada producto del ticket
 * - RegistroPrecio: el precio pagado, que alimenta el historial
 *
 * Las fotos de tickets NO se sincronizan con la nube (solo metadatos).
 */
class TicketRepository(
    private val ticketDao: TicketDao,
    private val lineaTicketDao: LineaTicketDao,
    private val registroPrecioDao: RegistroPrecioDao,
    private val hogarManager: HogarManager
) {

    private val firestore = FirebaseFirestore.getInstance()

    // ── LECTURA ──────────────────────────────────────────────────

    /**
     * Todos los tickets, del más reciente al más antiguo.
     */
    fun obtenerTodos(): Flow<List<Ticket>> = ticketDao.obtenerTodos()

    /**
     * Tickets de un supermercado concreto.
     */
    fun obtenerPorSupermercado(supermercadoId: Long): Flow<List<Ticket>> =
        ticketDao.obtenerPorSupermercado(supermercadoId)

    /**
     * Tickets pendientes de revisión.
     */
    fun obtenerPendientes(): Flow<List<Ticket>> = ticketDao.obtenerPendientes()

    /**
     * Las líneas (productos) de un ticket concreto.
     */
    fun obtenerLineas(ticketId: Long): Flow<List<LineaTicket>> =
        lineaTicketDao.obtenerPorTicket(ticketId)

    /**
     * Historial de precios de un producto concreto.
     */
    fun obtenerHistorialPrecios(productoId: Long): Flow<List<RegistroPrecio>> =
        registroPrecioDao.obtenerHistorialProducto(productoId)

    /**
     * El precio más reciente de un producto en un supermercado.
     * Útil para comparar con el precio nuevo al escanear un ticket.
     */
    suspend fun obtenerUltimoPrecio(
        productoId: Long,
        supermercadoId: Long
    ): RegistroPrecio? = registroPrecioDao.obtenerUltimoPrecio(productoId, supermercadoId)

    // ── ESCRITURA ─────────────────────────────────────────────────

    /**
     * Guarda un ticket completo con todas sus líneas y registros de precio.
     *
     * Este es el método principal que se llama cuando el usuario
     * confirma un ticket después de revisarlo.
     *
     * Orden obligatorio:
     * 1. Guardar el ticket (cabecera)
     * 2. Guardar las líneas (con referencia al ticket)
     * 3. Guardar los registros de precio (con referencia a producto)
     * 4. Sincronizar metadatos con Firestore
     */
    suspend fun guardarTicketCompleto(
        ticket: Ticket,
        lineas: List<LineaTicket>,
        registros: List<RegistroPrecio>
    ): Long {
        // 1. Guardar ticket y obtener su ID
        val ticketId = ticketDao.insertar(ticket)

        // 2. Guardar líneas con el ID del ticket correcto
        val lineasConId = lineas.map { it.copy(ticketId = ticketId) }
        lineasConId.forEach { lineaTicketDao.insertar(it) }

        // 3. Guardar registros de precio
        registros.forEach { registroPrecioDao.insertar(it) }

        // 4. Sincronizar metadatos del ticket (sin la foto)
        sincronizarTicketANube(ticket.copy(id = ticketId))

        return ticketId
    }

    /**
     * Marca un ticket como revisado actualizando su estado.
     */
    suspend fun marcarComoRevisado(ticketId: Long) {
        val ticket = ticketDao.obtenerPorId(ticketId) ?: return
        val actualizado = ticket.copy(estado = "revisado")
        ticketDao.actualizar(actualizado)

        val hogarId = hogarManager.obtenerHogarId() ?: return
        firestore
            .collection("hogares")
            .document(hogarId)
            .collection("tickets")
            .document(ticketId.toString())
            .update("estado", "revisado")
            .await()
    }

    // ── SINCRONIZACIÓN ────────────────────────────────────────────

    /**
     * Sube los metadatos de un ticket a Firestore.
     * IMPORTANTE: la ruta de la foto local NO se sube a la nube.
     */
    private suspend fun sincronizarTicketANube(ticket: Ticket) {
        val hogarId = hogarManager.obtenerHogarId() ?: return

        val datos = mapOf(
            "id" to ticket.id,
            "supermercadoId" to ticket.supermercadoId,
            "fecha" to ticket.fecha,
            "total" to ticket.total,
            "estado" to ticket.estado,
            "codigoHogar" to ticket.codigoHogar
            // rutaFoto → NO se sube, es una ruta local del móvil que escaneó
        )

        firestore
            .collection("hogares")
            .document(hogarId)
            .collection("tickets")
            .document(ticket.id.toString())
            .set(datos)
            .await()
    }

    /**
     * Descarga el estado de los tickets de Firestore y actualiza Room.
     * Solo sincroniza el estado — las fotos son locales a cada móvil.
     */
    suspend fun sincronizarDesdeNube() {
        val hogarId = hogarManager.obtenerHogarId() ?: return

        val documentos = firestore
            .collection("hogares")
            .document(hogarId)
            .collection("tickets")
            .get()
            .await()

        documentos.forEach { doc ->
            val ticketId = doc.getLong("id") ?: return@forEach
            val ticket = ticketDao.obtenerPorId(ticketId) ?: return@forEach
            val estadoNube = doc.getString("estado") ?: return@forEach
            // Solo actualiza si el estado en la nube es diferente al local
            if (ticket.estado != estadoNube) {
                ticketDao.actualizar(ticket.copy(estado = estadoNube))
            }
        }
    }
}
