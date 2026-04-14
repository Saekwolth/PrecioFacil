package com.preciofacil.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.preciofacil.app.data.local.database.PrecioFacilDatabase
import com.preciofacil.app.data.remote.HogarManager
import com.preciofacil.app.data.repository.SupermercadoRepository

/**
 * SupermercadoViewModel — proporciona la lista de supermercados al Dashboard.
 *
 * Usa LiveData para que la pantalla se actualice automáticamente
 * cuando cambian los datos en la base de datos local (Room).
 */
class SupermercadoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SupermercadoRepository
    val supermercados: androidx.lifecycle.LiveData<List<com.preciofacil.app.data.local.entity.Supermercado>>

    init {
        val dao = PrecioFacilDatabase.getInstance(application).supermercadoDao()
        val hogarManager = HogarManager(application)
        repository = SupermercadoRepository(dao, hogarManager)
        supermercados = repository.obtenerTodos().asLiveData()
    }
}
