package com.preciofacil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.preciofacil.app.data.local.entity.Supermercado
import kotlinx.coroutines.flow.Flow

/**
 * DAO de Supermercado.
 * Define todas las operaciones disponibles sobre la tabla supermercados.
 * Flow significa que la pantalla se actualiza automáticamente
 * cuando los datos cambian, sin tener que recargar manualmente.
 */
@Dao
interface SupermercadoDao {

    // Insertar un supermercado nuevo
    // Si ya existe uno con el mismo id, lo reemplaza
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(supermercado: Supermercado): Long

    // Actualizar un supermercado existente
    @Update
    suspend fun actualizar(supermercado: Supermercado)

    // Borrar un supermercado
    @Delete
    suspend fun borrar(supermercado: Supermercado)

    // Obtener todos los supermercados activos, ordenados por nombre
    @Query("SELECT * FROM supermercados WHERE activo = 1 ORDER BY nombre ASC")
    fun obtenerTodosActivos(): Flow<List<Supermercado>>

    // Obtener todos los supermercados (activos e inactivos)
    @Query("SELECT * FROM supermercados ORDER BY nombre ASC")
    fun obtenerTodos(): Flow<List<Supermercado>>

    // Obtener un supermercado por su id
    @Query("SELECT * FROM supermercados WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Supermercado?

    // Buscar supermercado por palabras clave del ticket
    // Usado por el parser para identificar automáticamente el supermercado
    @Query("SELECT * FROM supermercados WHERE palabrasClave LIKE '%' || :palabraClave || '%' LIMIT 1")
    suspend fun buscarPorPalabraClave(palabraClave: String): Supermercado?

    // Contar cuántos supermercados hay en total
    @Query("SELECT COUNT(*) FROM supermercados")
    suspend fun contarTotal(): Int
}
