package com.preciofacil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.preciofacil.app.data.local.entity.Producto
import kotlinx.coroutines.flow.Flow

/**
 * DAO de Producto.
 * Define todas las operaciones disponibles sobre la tabla productos.
 */
@Dao
interface ProductoDao {

    // Insertar un producto nuevo
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(producto: Producto): Long

    // Actualizar un producto existente
    @Update
    suspend fun actualizar(producto: Producto)

    // Borrar un producto
    @Delete
    suspend fun borrar(producto: Producto)

    // Obtener todos los productos ordenados por nombre
    @Query("SELECT * FROM productos ORDER BY nombreNormalizado ASC")
    fun obtenerTodos(): Flow<List<Producto>>

    // Obtener solo los productos habituales
    @Query("SELECT * FROM productos WHERE esHabitual = 1 ORDER BY nombreNormalizado ASC")
    fun obtenerHabituales(): Flow<List<Producto>>

    // Obtener un producto por su id
    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Producto?

    // Buscar producto por EAN — identificación principal
    // El EAN es único por producto, así que devuelve como máximo uno
    @Query("SELECT * FROM productos WHERE codigoEan = :ean LIMIT 1")
    suspend fun buscarPorEan(ean: String): Producto?

    // Buscar productos por nombre — para fuzzy matching
    // Devuelve todos los que contengan el texto buscado
    @Query("SELECT * FROM productos WHERE nombreNormalizado LIKE '%' || :texto || '%' OR alias LIKE '%' || :texto || '%' ORDER BY nombreNormalizado ASC")
    suspend fun buscarPorNombre(texto: String): List<Producto>

    // Buscar por categoría
    @Query("SELECT * FROM productos WHERE categoria = :categoria ORDER BY nombreNormalizado ASC")
    fun obtenerPorCategoria(categoria: String): Flow<List<Producto>>

    // Obtener productos vinculados a otro (misma familia, distinto formato)
    @Query("SELECT * FROM productos WHERE productoVinculadoId = :productoId")
    suspend fun obtenerVinculados(productoId: Long): List<Producto>

    // Contar cuántos productos hay en total
    @Query("SELECT COUNT(*) FROM productos")
    suspend fun contarTotal(): Int

    // Obtener todas las categorías existentes (para filtros)
    @Query("SELECT DISTINCT categoria FROM productos WHERE categoria != '' ORDER BY categoria ASC")
    suspend fun obtenerCategorias(): List<String>
}
