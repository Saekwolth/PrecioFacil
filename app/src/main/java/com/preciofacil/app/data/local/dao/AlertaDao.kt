package com.preciofacil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.preciofacil.app.data.local.entity.Alerta
import kotlinx.coroutines.flow.Flow

/**
 * DAO de Alerta.
 * Define todas las operaciones sobre la tabla alertas.
 */
@Dao
interface AlertaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(alerta: Alerta): Long

    @Update
    suspend fun actualizar(alerta: Alerta)

    @Delete
    suspend fun borrar(alerta: Alerta)

    // Obtener todas las alertas ordenadas por fecha descendente
    @Query("SELECT * FROM alertas ORDER BY fecha DESC")
    fun obtenerTodas(): Flow<List<Alerta>>

    // Obtener solo las alertas no leídas
    @Query("SELECT * FROM alertas WHERE leida = 0 ORDER BY fecha DESC")
    fun obtenerNoLeidas(): Flow<List<Alerta>>

    // Contar alertas no leídas — para el badge del icono de alertas
    @Query("SELECT COUNT(*) FROM alertas WHERE leida = 0")
    fun contarNoLeidas(): Flow<Int>

    // Obtener alertas de un producto concreto
    @Query("SELECT * FROM alertas WHERE productoId = :productoId ORDER BY fecha DESC")
    fun obtenerPorProducto(productoId: Long): Flow<List<Alerta>>

    // Obtener alertas de un tipo concreto
    @Query("SELECT * FROM alertas WHERE tipo = :tipo ORDER BY fecha DESC")
    fun obtenerPorTipo(tipo: String): Flow<List<Alerta>>

    // Marcar una alerta como leída
    @Query("UPDATE alertas SET leida = 1 WHERE id = :id")
    suspend fun marcarComoLeida(id: Long)

    // Marcar todas las alertas como leídas
    @Query("UPDATE alertas SET leida = 1")
    suspend fun marcarTodasComoLeidas()

    // Borrar alertas más antiguas de X días — para no acumular demasiadas
    @Query("DELETE FROM alertas WHERE fecha < :fechaLimite")
    suspend fun borrarAnterioresA(fechaLimite: Long)

    // Obtener alertas ordenadas por impacto económico (variación en euros)
    @Query("SELECT * FROM alertas WHERE variacionEuros IS NOT NULL ORDER BY ABS(variacionEuros) DESC")
    fun obtenerPorImpactoEconomico(): Flow<List<Alerta>>
}
