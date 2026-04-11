package com.preciofacil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.preciofacil.app.data.local.entity.ListaCompra
import kotlinx.coroutines.flow.Flow

/**
 * DAO de ListaCompra.
 * Define todas las operaciones sobre la tabla lista_compra.
 */
@Dao
interface ListaCompraDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(item: ListaCompra): Long

    @Update
    suspend fun actualizar(item: ListaCompra)

    @Delete
    suspend fun borrar(item: ListaCompra)

    // Obtener todos los items de la lista activa
    @Query("SELECT * FROM lista_compra ORDER BY añadidoAutomaticamente DESC, estado ASC")
    fun obtenerTodos(): Flow<List<ListaCompra>>

    // Obtener solo los items pendientes (sin comprar)
    @Query("SELECT * FROM lista_compra WHERE estado = 'pendiente' ORDER BY añadidoAutomaticamente DESC")
    fun obtenerPendientes(): Flow<List<ListaCompra>>

    // Obtener un item por su producto
    @Query("SELECT * FROM lista_compra WHERE productoId = :productoId LIMIT 1")
    suspend fun obtenerPorProducto(productoId: Long): ListaCompra?

    // Marcar un item como comprado
    @Query("UPDATE lista_compra SET estado = 'comprado' WHERE id = :id")
    suspend fun marcarComoComprado(id: Long)

    // Marcar un item como no había stock
    @Query("UPDATE lista_compra SET estado = 'no_habia_stock' WHERE id = :id")
    suspend fun marcarComoSinStock(id: Long)

    // Marcar un item como omitido
    @Query("UPDATE lista_compra SET estado = 'omitido' WHERE id = :id")
    suspend fun marcarComoOmitido(id: Long)

    // Calcular el coste estimado total de la lista
    @Query("SELECT SUM(precioEstimado * cantidadDeseada) FROM lista_compra WHERE estado = 'pendiente' AND precioEstimado IS NOT NULL")
    fun calcularCosteEstimado(): Flow<Double?>

    // Limpiar los items ya comprados de la lista
    @Query("DELETE FROM lista_compra WHERE estado = 'comprado'")
    suspend fun limpiarComprados()

    // Contar items pendientes — para el badge de la lista
    @Query("SELECT COUNT(*) FROM lista_compra WHERE estado = 'pendiente'")
    fun contarPendientes(): Flow<Int>

    // Comprobar si un producto ya está en la lista
    @Query("SELECT COUNT(*) FROM lista_compra WHERE productoId = :productoId AND estado = 'pendiente'")
    suspend fun estaEnLista(productoId: Long): Int
}
