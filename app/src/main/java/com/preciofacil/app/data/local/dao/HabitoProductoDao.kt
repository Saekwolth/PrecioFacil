package com.preciofacil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.preciofacil.app.data.local.entity.HabitoProducto
import kotlinx.coroutines.flow.Flow

/**
 * DAO de HabitoProducto.
 * Define todas las operaciones sobre la tabla habitos_producto.
 */
@Dao
interface HabitoProductoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(habito: HabitoProducto): Long

    @Update
    suspend fun actualizar(habito: HabitoProducto)

    @Delete
    suspend fun borrar(habito: HabitoProducto)

    // Obtener el hábito de un producto concreto
    @Query("SELECT * FROM habitos_producto WHERE productoId = :productoId LIMIT 1")
    suspend fun obtenerPorProducto(productoId: Long): HabitoProducto?

    // Obtener todos los hábitos con avisos activados
    @Query("SELECT * FROM habitos_producto WHERE avisosHabitoActivados = 1")
    fun obtenerConAvisosActivados(): Flow<List<HabitoProducto>>

    // Obtener hábitos con suficientes datos para generar alertas
    @Query("SELECT * FROM habitos_producto WHERE ticketsProcesados >= minimoTicketsParaCalcular AND avisosHabitoActivados = 1")
    fun obtenerHabitosConsolidados(): Flow<List<HabitoProducto>>

    // Obtener productos que toca comprar hoy
    // (última compra + frecuencia media <= hoy)
    @Query("""
        SELECT * FROM habitos_producto 
        WHERE avisosHabitoActivados = 1
        AND ticketsProcesados >= minimoTicketsParaCalcular
        AND ultimaVezComprado IS NOT NULL
        AND (ultimaVezComprado + (frecuenciaMediaDias * 86400000)) <= :ahora
    """)
    suspend fun obtenerProductosQueTocaComprar(ahora: Long = System.currentTimeMillis()): List<HabitoProducto>

    // Incrementar el contador de alertas silenciadas
    // Si llega a 2, la app sugerirá marcar el producto como de temporada
    @Query("UPDATE habitos_producto SET alertasSilenciadasSeguidas = alertasSilenciadasSeguidas + 1 WHERE productoId = :productoId")
    suspend fun incrementarAlertasSilenciadas(productoId: Long)

    // Resetear el contador de alertas silenciadas (cuando el usuario sí compra el producto)
    @Query("UPDATE habitos_producto SET alertasSilenciadasSeguidas = 0 WHERE productoId = :productoId")
    suspend fun resetearAlertasSilenciadas(productoId: Long)

    // Actualizar la última vez comprado y el contador de tickets procesados
    @Query("UPDATE habitos_producto SET ultimaVezComprado = :fecha, ticketsProcesados = ticketsProcesados + 1 WHERE productoId = :productoId")
    suspend fun registrarCompra(productoId: Long, fecha: Long)
}
