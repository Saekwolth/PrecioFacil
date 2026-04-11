package com.preciofacil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.preciofacil.app.data.local.entity.LineaTicket
import kotlinx.coroutines.flow.Flow

/**
 * DAO de LineaTicket.
 * Define todas las operaciones disponibles sobre la tabla lineas_ticket.
 */
@Dao
interface LineaTicketDao {

    // Insertar una línea nueva
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(linea: LineaTicket): Long

    // Insertar varias líneas a la vez — más eficiente al procesar un ticket completo
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarVarias(lineas: List<LineaTicket>)

    // Actualizar una línea (ej: cuando el usuario corrige el producto o precio)
    @Update
    suspend fun actualizar(linea: LineaTicket)

    // Borrar una línea
    @Delete
    suspend fun borrar(linea: LineaTicket)

    // Obtener todas las líneas de un ticket concreto
    @Query("SELECT * FROM lineas_ticket WHERE ticketId = :ticketId ORDER BY id ASC")
    fun obtenerPorTicket(ticketId: Long): Flow<List<LineaTicket>>

    // Obtener solo las líneas que son productos (excluye descuentos y totales)
    @Query("SELECT * FROM lineas_ticket WHERE ticketId = :ticketId AND esProducto = 1 ORDER BY id ASC")
    fun obtenerProductosPorTicket(ticketId: Long): Flow<List<LineaTicket>>

    // Obtener líneas pendientes de revisión de un ticket
    // Ordenadas por confianza: primero las menos fiables (nuevo, nombre_similar)
    @Query("""
        SELECT * FROM lineas_ticket 
        WHERE ticketId = :ticketId AND esProducto = 1 AND revisadoPorUsuario = 0
        ORDER BY CASE confianzaIdentificacion
            WHEN 'nuevo' THEN 1
            WHEN 'nombre_similar' THEN 2
            WHEN 'ean_exacto' THEN 3
            ELSE 4
        END ASC
    """)
    fun obtenerPendientesDeRevision(ticketId: Long): Flow<List<LineaTicket>>

    // Obtener todas las líneas de un producto concreto (historial de apariciones)
    @Query("SELECT * FROM lineas_ticket WHERE productoId = :productoId ORDER BY id DESC")
    suspend fun obtenerPorProducto(productoId: Long): List<LineaTicket>

    // Borrar todas las líneas de un ticket (al borrar el ticket)
    @Query("DELETE FROM lineas_ticket WHERE ticketId = :ticketId")
    suspend fun borrarPorTicket(ticketId: Long)

    // Contar líneas pendientes de revisión en un ticket
    @Query("SELECT COUNT(*) FROM lineas_ticket WHERE ticketId = :ticketId AND esProducto = 1 AND revisadoPorUsuario = 0")
    suspend fun contarPendientes(ticketId: Long): Int
}
