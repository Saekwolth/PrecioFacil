package com.preciofacil.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.preciofacil.app.ui.EstadoPantalla
import com.preciofacil.app.ui.HogarViewModel

/**
 * MainActivity — pantalla de entrada de la app.
 *
 * Esta pantalla gestiona el flujo de configuración del hogar:
 * 1. Si ya hay hogar → salta directamente al Dashboard (próxima fase)
 * 2. Si no hay hogar → muestra opciones de crear o unirse
 *
 * Toda la lógica está en HogarViewModel.
 * MainActivity solo observa el estado y muestra la pantalla correcta.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: HogarViewModel

    // ── REFERENCIAS A LAS VISTAS ──────────────────────────────────
    // Pantallas
    private lateinit var pantallasBienvenida: View
    private lateinit var pantallaUnirse: View
    private lateinit var pantallaCargando: View
    private lateinit var pantallaCodigoCreado: View

    // Botones
    private lateinit var btnCrearHogar: MaterialButton
    private lateinit var btnUnirseHogar: MaterialButton
    private lateinit var btnConfirmarCodigo: MaterialButton
    private lateinit var btnVolverBienvenida: MaterialButton
    private lateinit var btnContinuar: MaterialButton

    // Textos y campos
    private lateinit var inputCodigo: TextInputEditText
    private lateinit var txtError: TextView
    private lateinit var txtCargando: TextView
    private lateinit var txtCodigoGenerado: TextView

    // ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inicializarVistas()
        inicializarViewModel()
        configurarBotones()
    }

    // ── INICIALIZACIÓN ────────────────────────────────────────────

    private fun inicializarVistas() {
        pantallasBienvenida = findViewById(R.id.pantallasBienvenida)
        pantallaUnirse = findViewById(R.id.pantallaUnirse)
        pantallaCargando = findViewById(R.id.pantallaCargando)
        pantallaCodigoCreado = findViewById(R.id.pantallaCodigoCreado)

        btnCrearHogar = findViewById(R.id.btnCrearHogar)
        btnUnirseHogar = findViewById(R.id.btnUnirseHogar)
        btnConfirmarCodigo = findViewById(R.id.btnConfirmarCodigo)
        btnVolverBienvenida = findViewById(R.id.btnVolverBienvenida)
        btnContinuar = findViewById(R.id.btnContinuar)

        inputCodigo = findViewById(R.id.inputCodigo)
        txtError = findViewById(R.id.txtError)
        txtCargando = findViewById(R.id.txtCargando)
        txtCodigoGenerado = findViewById(R.id.txtCodigoGenerado)
    }

    private fun inicializarViewModel() {
        viewModel = ViewModelProvider(this)[HogarViewModel::class.java]

        // Observar el estado — cuando cambia, actualizar la pantalla
        viewModel.estado.observe(this) { estado ->
            when (estado) {
                is EstadoPantalla.Bienvenida -> mostrarBienvenida()
                is EstadoPantalla.Cargando -> mostrarCargando(estado.mensaje)
                is EstadoPantalla.CodigoCreado -> mostrarCodigoCreado(estado.codigo)
                is EstadoPantalla.Unirse -> mostrarUnirse()
                is EstadoPantalla.Error -> manejarError(estado.mensaje)
                is EstadoPantalla.ErrorUnirse -> mostrarErrorUnirse(estado.mensaje)
                is EstadoPantalla.Completado -> irAlDashboard()
            }
        }
    }

    private fun configurarBotones() {
        btnCrearHogar.setOnClickListener {
            viewModel.crearHogar()
        }

        btnUnirseHogar.setOnClickListener {
            viewModel.mostrarPantallaUnirse()
        }

        btnConfirmarCodigo.setOnClickListener {
            val codigo = inputCodigo.text.toString().trim().uppercase()
            viewModel.unirseAlHogar(codigo)
        }

        btnVolverBienvenida.setOnClickListener {
            viewModel.volverABienvenida()
        }

        btnContinuar.setOnClickListener {
            viewModel.continuar()
        }
    }

    // ── MOSTRAR PANTALLAS ─────────────────────────────────────────

    private fun ocultarTodo() {
        pantallasBienvenida.visibility = View.GONE
        pantallaUnirse.visibility = View.GONE
        pantallaCargando.visibility = View.GONE
        pantallaCodigoCreado.visibility = View.GONE
    }

    private fun mostrarBienvenida() {
        ocultarTodo()
        pantallasBienvenida.visibility = View.VISIBLE
    }

    private fun mostrarCargando(mensaje: String) {
        ocultarTodo()
        txtCargando.text = mensaje
        pantallaCargando.visibility = View.VISIBLE
    }

    private fun mostrarCodigoCreado(codigo: String) {
        ocultarTodo()
        txtCodigoGenerado.text = codigo
        pantallaCodigoCreado.visibility = View.VISIBLE
    }

    private fun mostrarUnirse() {
        ocultarTodo()
        txtError.visibility = View.GONE
        inputCodigo.text?.clear()
        pantallaUnirse.visibility = View.VISIBLE
    }

    private fun mostrarErrorUnirse(mensaje: String) {
        // No cambia de pantalla — solo muestra el error debajo del campo
        pantallaUnirse.visibility = View.VISIBLE
        txtError.text = mensaje
        txtError.visibility = View.VISIBLE
    }

    private fun manejarError(mensaje: String) {
        // Error general — volver a bienvenida con mensaje
        ocultarTodo()
        pantallasBienvenida.visibility = View.VISIBLE
        // Por ahora mostramos el error en consola — en próximas fases usaremos Snackbar
        android.util.Log.e("PrecioFacil", "Error: $mensaje")
    }

    private fun irAlDashboard() {
        // Por ahora simplemente cerramos esta Activity
        // En la próxima fase abriremos DashboardActivity
        startActivity(android.content.Intent(this, DashboardActivity::class.java))
        finish()
    }
}
