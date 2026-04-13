package com.preciofacil.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.preciofacil.app.PrecioFacilApp
import com.preciofacil.app.data.remote.HogarManager
import com.preciofacil.app.data.remote.ResultadoHogar
import kotlinx.coroutines.launch

/**
 * HogarViewModel — lógica de la pantalla de configuración de hogar.
 *
 * El ViewModel separa la lógica de la pantalla visual.
 * MainActivity solo se encarga de mostrar lo que el ViewModel le dice.
 *
 * Estados posibles de la pantalla:
 * - BIENVENIDA: pantalla inicial (crear o unirse)
 * - CARGANDO: conectando con Firebase
 * - CODIGO_CREADO: hogar creado, mostrando el código al usuario
 * - UNIRSE: pantalla para introducir el código
 * - ERROR: algo salió mal
 * - COMPLETADO: todo listo, entrar a la app
 */
class HogarViewModel(application: Application) : AndroidViewModel(application) {

    private val hogarManager = HogarManager(application)

    // ── ESTADO DE LA PANTALLA ─────────────────────────────────────

    private val _estado = MutableLiveData<EstadoPantalla>(EstadoPantalla.Bienvenida)
    val estado: LiveData<EstadoPantalla> = _estado

    // ── INICIALIZACIÓN ────────────────────────────────────────────

    init {
        // Si este móvil ya tiene hogar configurado, saltamos directamente a la app
        if (hogarManager.tieneHogar()) {
            _estado.value = EstadoPantalla.Completado
        }
    }

    // ── ACCIONES DEL USUARIO ──────────────────────────────────────

    /**
     * El usuario pulsa "Crear hogar nuevo".
     * Conecta con Firebase y genera el código de 6 caracteres.
     */
    fun crearHogar() {
        _estado.value = EstadoPantalla.Cargando("Creando tu hogar...")

        viewModelScope.launch {
            when (val resultado = hogarManager.crearHogar()) {
                is ResultadoHogar.Exito -> {
                    _estado.value = EstadoPantalla.CodigoCreado(resultado.codigo)
                }
                is ResultadoHogar.Error -> {
                    _estado.value = EstadoPantalla.Error(resultado.mensaje)
                }
            }
        }
    }

    /**
     * El usuario pulsa "Unirse a hogar existente".
     * Solo cambia la pantalla — todavía no conecta con Firebase.
     */
    fun mostrarPantallaUnirse() {
        _estado.value = EstadoPantalla.Unirse
    }

    /**
     * El usuario introduce el código y pulsa confirmar.
     * Conecta con Firebase y verifica el código.
     */
    fun unirseAlHogar(codigo: String) {
        if (codigo.length < 6) {
            _estado.value = EstadoPantalla.Error("El código debe tener 6 caracteres")
            return
        }

        _estado.value = EstadoPantalla.Cargando("Buscando tu hogar...")

        viewModelScope.launch {
            when (val resultado = hogarManager.unirseAlHogar(codigo)) {
                is ResultadoHogar.Exito -> {
                    _estado.value = EstadoPantalla.Completado
                }
                is ResultadoHogar.Error -> {
                    _estado.value = EstadoPantalla.ErrorUnirse(resultado.mensaje)
                }
            }
        }
    }

    /**
     * El usuario pulsa "Volver" desde la pantalla de unirse.
     */
    fun volverABienvenida() {
        _estado.value = EstadoPantalla.Bienvenida
    }

    /**
     * El usuario pulsa "Continuar" después de ver el código creado.
     */
    fun continuar() {
        _estado.value = EstadoPantalla.Completado
    }
}

// ── ESTADOS DE LA PANTALLA ────────────────────────────────────────

/**
 * Todos los estados posibles de la pantalla.
 * MainActivity observa este estado y muestra la pantalla correcta.
 */
sealed class EstadoPantalla {
    /** Pantalla inicial: botones "Crear" y "Unirse" */
    object Bienvenida : EstadoPantalla()

    /** Rueda de carga con mensaje */
    data class Cargando(val mensaje: String) : EstadoPantalla()

    /** Hogar creado — mostrar el código al usuario */
    data class CodigoCreado(val codigo: String) : EstadoPantalla()

    /** Pantalla para introducir el código de hogar */
    object Unirse : EstadoPantalla()

    /** Error general — volver a bienvenida */
    data class Error(val mensaje: String) : EstadoPantalla()

    /** Error al unirse — quedarse en pantalla de unirse */
    data class ErrorUnirse(val mensaje: String) : EstadoPantalla()

    /** Todo listo — entrar a la app */
    object Completado : EstadoPantalla()
}
