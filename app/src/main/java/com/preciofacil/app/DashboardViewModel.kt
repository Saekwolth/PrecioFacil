package com.preciofacil.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.preciofacil.app.data.local.database.PrecioFacilDatabase
import com.preciofacil.app.data.local.entity.Supermercado
import com.preciofacil.app.data.remote.HogarManager
import com.preciofacil.app.data.repository.SupermercadoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * DashboardViewModel — proporciona todos los datos de la pantalla principal.
 *
 * - Lista de supermercados (en tiempo real desde Room)
 * - Gasto total del mes en curso
 * - Número de tickets este mes
 * - Último ticket escaneado
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PrecioFacilDatabase.getInstance(application)
    private val hogarManager = HogarManager(application)

    // ── Supermercados en tiempo real ──────────────────────────────────
    val supermercados: LiveData<List<Supermercado>>

    // ── Datos del mes ─────────────────────────────────────────────────
    private val _gastoMes = MutableLiveData(0.0)
    val gastoMes: LiveData<Double> = _gastoMes

    private val _numTickets = MutableLiveData(0)
    val numTickets: LiveData<Int> = _numTickets

    private val _ultimoTicket = MutableLiveData("Escanea tu primer ticket para empezar.")
    val ultimoTicket: LiveData<String> = _ultimoTicket

    init {
        val dao = db.supermercadoDao()
        val repo = SupermercadoRepository(dao, hogarManager)
        supermercados = repo.obtenerTodos().asLiveData()

        cargarDatosMes()
    }

    fun cargarDatosMes() {
        viewModelScope.launch {
            val inicioMes = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val ahora = System.currentTimeMillis()

            // Gasto y número de tickets del mes
            val gasto = db.ticketDao().calcularGastoTotal(inicioMes, ahora) ?: 0.0
            val num = db.ticketDao().contarEntreFechas(inicioMes, ahora)
            _gastoMes.postValue(gasto)
            _numTickets.postValue(num)

            // Último ticket
            val tickets = db.ticketDao().obtenerTodos().first()
            if (tickets.isNotEmpty()) {
                val ultimo = tickets.first()
                val nombreSuper = db.supermercadoDao()
                    .obtenerPorId(ultimo.supermercadoId)?.nombre ?: "Supermercado"
                val fecha = android.text.format.DateFormat.format("dd/MM/yy", ultimo.fecha)
                _ultimoTicket.postValue(
                    "Último ticket: $nombreSuper\n$fecha — ${"%.2f".format(ultimo.total)} €"
                )
            } else {
                _ultimoTicket.postValue("Escanea tu primer ticket para empezar.")
            }
        }
    }
}
