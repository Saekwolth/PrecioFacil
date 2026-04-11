package com.preciofacil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.preciofacil.app.data.local.entity.Ticket
import kotlinx.coroutines.flow.Flow

/**
 * DAO de Ticket.
 * Define todas las operaciones disponibles sobre la tabla tickets.
 */
@Dao
interface TicketDao {

    // Insertar un ticket nuevo
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(ticket: Ticket): Long

    // Actualizar un ticket existente (ej: cambiar estado tras revisión)
    @Update
    suspend fun actualizar(ticket: Ticket)

    // Borrar un ticket
    @Delete
    suspend fun borrar(ticket: Ticket)

    // Obtener todos los tickets ordenados por fecha descendente (más reciente primero)
    @Query("SELECT * FROM tickets ORDER BY fecha DESC")
    fun obtenerTodos(): Flow<List<Ticket>>

    // Obtener un ticket por su id
    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Ticket?

    // Obtener tickets de un supermercado concreto
    @Query("SELECT * FROM tickets WHERE supermercadoId = :supermercadoId ORDER BY fecha DESC")
    fun obtenerPorSupermercado(supermercadoId: Long): Flow<List<Ticket>>

    // Obtener tickets pendientes de revisión
    @Query("SELECT * FROM tickets WHERE estado = 'pendiente_revision' ORDER BY fecha DESC")
    fun obtenerPendientes(): Flow<List<Ticket>>

    // Obtener tickets de un mes concreto (para estadísticas)
    // fechaInicio y fechaFin en milisegundos
    @Query("SELECT * FROM tickets WHERE fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY fecha DESC")
    fun obtenerEntreFechas(fechaInicio: Long, fechaFin: Long): Flow<List<Ticket>>

    // Calcular el gasto total en un período (para estadísticas)
    @Query("SELECT SUM(total) FROM tickets WHERE fecha BETWEEN :fechaInicio AND :fechaFin")
    suspend fun calcularGastoTotal(fechaInicio: Long, fechaFin: Long): Double?

    // Calcular el gasto total por supermercado en un período
    @Query("SELECT SUM(total) FROM tickets WHERE supermercadoId = :supermercadoId AND fecha BETWEEN :fechaInicio AND :fechaFin")
    suspend fun calcularGastoPorSupermercado(supermercadoId: Long, fechaInicio: Long, fechaFin: Long): Double?

    // Obtener el ticket más reciente de un supermercado
    @Query("SELECT * FROM tickets WHERE supermercadoId = :supermercadoId ORDER BY fecha DESC LIMIT 1")
    suspend fun obtenerUltimoPorSupermercado(supermercadoId: Long): Ticket?

    // Contar tickets en un período (para estadísticas)
    @Query("SELECT COUNT(*) FROM tickets WHERE fecha BETWEEN :fechaInicio AND :fechaFin")
    suspend fun contarEntreFechas(fechaInicio: Long, fechaFin: Long): Int
}
