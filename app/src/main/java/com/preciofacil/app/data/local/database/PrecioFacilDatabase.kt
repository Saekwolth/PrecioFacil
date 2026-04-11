package com.preciofacil.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.preciofacil.app.data.local.dao.AlertaDao
import com.preciofacil.app.data.local.dao.HabitoProductoDao
import com.preciofacil.app.data.local.dao.LineaTicketDao
import com.preciofacil.app.data.local.dao.ListaCompraDao
import com.preciofacil.app.data.local.dao.ProductoDao
import com.preciofacil.app.data.local.dao.RegistroPrecioDao
import com.preciofacil.app.data.local.dao.SupermercadoDao
import com.preciofacil.app.data.local.dao.TicketDao
import com.preciofacil.app.data.local.entity.Alerta
import com.preciofacil.app.data.local.entity.HabitoProducto
import com.preciofacil.app.data.local.entity.LineaTicket
import com.preciofacil.app.data.local.entity.ListaCompra
import com.preciofacil.app.data.local.entity.Producto
import com.preciofacil.app.data.local.entity.RegistroPrecio
import com.preciofacil.app.data.local.entity.Supermercado
import com.preciofacil.app.data.local.entity.Ticket

/**
 * Base de datos principal de PrecioFácil.
 *
 * Cada vez que se añade una tabla nueva o se modifica una existente,
 * hay que incrementar el número de versión (version = X).
 * Room gestiona automáticamente la migración sin borrar los datos.
 */
@Database(
    entities = [
        Supermercado::class,
        Producto::class,
        Ticket::class,
        LineaTicket::class,
        RegistroPrecio::class,
        HabitoProducto::class,
        Alerta::class,
        ListaCompra::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PrecioFacilDatabase : RoomDatabase() {

    // Acceso a cada tabla — se crean en los próximos archivos
    abstract fun supermercadoDao(): SupermercadoDao
    abstract fun productoDao(): ProductoDao
    abstract fun ticketDao(): TicketDao
    abstract fun lineaTicketDao(): LineaTicketDao
    abstract fun registroPrecioDao(): RegistroPrecioDao
    abstract fun habitoProductoDao(): HabitoProductoDao
    abstract fun alertaDao(): AlertaDao
    abstract fun listaCompraDao(): ListaCompraDao

    companion object {
        // Instancia única de la base de datos — patrón Singleton
        // Garantiza que solo existe una conexión a la base de datos
        @Volatile
        private var INSTANCE: PrecioFacilDatabase? = null

        fun getInstance(context: Context): PrecioFacilDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrecioFacilDatabase::class.java,
                    "preciofacil_database"
                )
                // Si en el futuro hay cambios de versión sin migración definida,
                // borra y recrea la base de datos en lugar de dar error
                // (solo para desarrollo — en producción usaremos migraciones)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
