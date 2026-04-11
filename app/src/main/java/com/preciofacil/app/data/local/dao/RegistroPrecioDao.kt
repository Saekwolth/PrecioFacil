package com.preciofacil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.preciofacil.app.data.local.entity.RegistroPrecio
import kotlinx.coroutines.flow.Flow

/**
 * DAO de RegistroPrecio.
 * Define todas las operaciones sobre la tabla registros_precio.
 * Es la base de todas las comparativas, alertas y estadísticas.
 */
@Dao
interface RegistroPrecioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(registro: RegistroPrecio): Long

    @Update
    suspend fun actualizar(registro: RegistroPrecio)

    @Delete
    suspend fun borrar(registro: RegistroPrecio)

    // Historial completo de precios de un producto, ordenado por fecha descendente
    @Query("SELECT * FROM registros_precio WHERE productoId = :productoId ORDER BY fecha DESC")
    fun obtenerHistorialProducto(productoId: Long): Flow<List<RegistroPrecio>>

    // Historial de precios de un producto en un supermercado concreto
    @Query("SELECT * FROM registros_precio WHERE productoId = :productoId AND supermercadoId = :supermercadoId ORDER BY fecha DESC")
    fun obtenerHistorialEnSupermercado(productoId: Long, supermercadoId: Long): Flow<List<RegistroPrecio>>

    // Último precio conocido de un producto en un supermercado
    @Query("SELECT * FROM registros_precio WHERE productoId = :productoId AND supermercadoId = :supermercadoId AND esPrecioPromocional = 0 ORDER BY fecha DESC LIMIT 1")
    suspend fun obtenerUltimoPrecio(productoId: Long, supermercadoId: Long): RegistroPrecio?

    // Precio mínimo histórico de un producto (sin impuesto, sin promociones)
    @Query("SELECT MIN(precioSinImpuesto) FROM registros_precio WHERE productoId = :productoId AND esPrecioPromocional = 0")
    suspend fun obtenerPrecioMinimo(productoId: Long): Double?

    // Precio máximo histórico de un producto
    @Query("SELECT MAX(precioSinImpuesto) FROM registros_precio WHERE productoId = :productoId AND esPrecioPromocional = 0")
    suspend fun obtenerPrecioMaximo(productoId: Long): Double?

    // Precio medio histórico de un producto
    @Query("SELECT AVG(precioSinImpuesto) FROM registros_precio WHERE productoId = :productoId AND esPrecioPromocional = 0")
    suspend fun obtenerPrecioMedio(productoId: Long): Double?

    // Comparativa de precios entre supermercados para un producto
    // Devuelve el último precio en cada supermercado
    @Query("""
        SELECT * FROM registros_precio rp
        WHERE productoId = :productoId
        AND esPrecioPromocional = 0
        AND fecha = (
            SELECT MAX(fecha) FROM registros_precio
            WHERE productoId = :productoId
            AND supermercadoId = rp.supermercadoId
            AND esPrecioPromocional = 0
        )
        ORDER BY precioSinImpuesto ASC
    """)
    suspend fun obtenerComparativaSupermercados(productoId: Long): List<RegistroPrecio>

    // Registros de un ticket concreto
    @Query("SELECT * FROM registros_precio WHERE ticketId = :ticketId")
    suspend fun obtenerPorTicket(ticketId: Long): List<RegistroPrecio>

    // Productos con mayor variación de precio en euros en un período
    // Usado para el ranking de estadísticas
    @Query("""
        SELECT productoId, 
               MAX(precioSinImpuesto) - MIN(precioSinImpuesto) as variacion
        FROM registros_precio
        WHERE fecha BETWEEN :fechaInicio AND :fechaFin
        AND esPrecioPromocional = 0
        GROUP BY productoId
        ORDER BY variacion DESC
        LIMIT :limite
    """)
    suspend fun obtenerProductosMayorVariacion(fechaInicio: Long, fechaFin: Long, limite: Int = 10): List<VariacionProducto>
}

// Clase auxiliar para el resultado de la consulta de variación
data class VariacionProducto(
    val productoId: Long,
    val variacion: Double
)
