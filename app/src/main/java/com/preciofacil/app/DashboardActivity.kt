package com.preciofacil.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.preciofacil.app.data.remote.HogarManager

/**
 * DashboardActivity — pantalla principal de PrecioFácil.
 *
 * Es lo primero que ve el usuario después de configurar el hogar.
 * Por ahora muestra el resumen básico. En próximas fases se irá
 * llenando con datos reales de tickets y alertas.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var hogarManager: HogarManager

    // ── VISTAS ────────────────────────────────────────────────────
    private lateinit var toolbar: MaterialToolbar
    private lateinit var txtSaludo: TextView
    private lateinit var txtCodigoHogar: TextView
    private lateinit var txtGastoMes: TextView
    private lateinit var txtNumTickets: TextView
    private lateinit var txtAlertas: TextView
    private lateinit var btnEscanear: MaterialButton
    private lateinit var cardCatalogo: MaterialCardView
    private lateinit var cardEstadisticas: MaterialCardView
    private lateinit var cardAjustes: MaterialCardView

    // ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        hogarManager = HogarManager(this)

        inicializarVistas()
        mostrarDatosHogar()
        configurarBotones()
    }

    // ── INICIALIZACIÓN ────────────────────────────────────────────

    private fun inicializarVistas() {
        toolbar = findViewById(R.id.toolbar)
        txtSaludo = findViewById(R.id.txtSaludo)
        txtCodigoHogar = findViewById(R.id.txtCodigoHogar)
        txtGastoMes = findViewById(R.id.txtGastoMes)
        txtNumTickets = findViewById(R.id.txtNumTickets)
        txtAlertas = findViewById(R.id.txtAlertas)
        btnEscanear = findViewById(R.id.btnEscanear)
        cardCatalogo = findViewById(R.id.cardCatalogo)
        cardEstadisticas = findViewById(R.id.cardEstadisticas)
        cardAjustes = findViewById(R.id.cardAjustes)

        setSupportActionBar(toolbar)
    }

    private fun mostrarDatosHogar() {
        val codigo = hogarManager.obtenerCodigoHogar()
        if (codigo != null) {
            txtCodigoHogar.text = "Hogar: $codigo"
        } else {
            txtCodigoHogar.text = "Hogar: no configurado"
        }
    }

    private fun configurarBotones() {
        btnEscanear.setOnClickListener {
            // Próxima fase: abrir cámara para escanear ticket
            mostrarMensajeTemporal("Escaneo de tickets — próximamente")
        }

        cardCatalogo.setOnClickListener {
            mostrarMensajeTemporal("Catálogo de productos — próximamente")
        }

        cardEstadisticas.setOnClickListener {
            mostrarMensajeTemporal("Estadísticas — próximamente")
        }

        cardAjustes.setOnClickListener {
            mostrarMensajeTemporal("Ajustes — próximamente")
        }
    }

    // ── UTILIDADES ────────────────────────────────────────────────

    private fun mostrarMensajeTemporal(mensaje: String) {
        com.google.android.material.snackbar.Snackbar
            .make(findViewById(android.R.id.content), mensaje, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
    }
}
