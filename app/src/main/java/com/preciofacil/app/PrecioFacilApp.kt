package com.preciofacil.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.preciofacil.app.data.local.database.PrecioFacilDatabase
import com.preciofacil.app.data.local.seed.DatabaseSeeder
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * PrecioFacilApp — Punto de entrada de la aplicación.
 *
 * Android crea esta clase ANTES que cualquier pantalla.
 * Es el lugar correcto para inicializar Firebase y la base de datos.
 */
class PrecioFacilApp : Application() {

    // Base de datos local — accesible desde cualquier parte de la app
    val database: PrecioFacilDatabase by lazy {
        PrecioFacilDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Inicializar Firebase — necesario antes de usar Firestore o Auth
        FirebaseApp.initializeApp(this)

        // Precargar supermercados iniciales si es la primera vez
        MainScope().launch {
            DatabaseSeeder.inicializarSiEsNecesario(database)
        }
    }
}
