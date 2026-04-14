package com.preciofacil.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.preciofacil.app.data.remote.HogarManager

class DashboardActivity : AppCompatActivity() {

    private lateinit var hogarManager: HogarManager

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
    private lateinit var listaSupermercados: RecyclerView
    private lateinit var txtSinSupermercados: TextView

    private lateinit var viewModel: DashboardViewModel
    private lateinit var adapter: SupermercadoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        hogarManager = HogarManager(this)
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        inicializarVistas()
        configurarListaSupermercados()
        mostrarDatosHogar()
        configurarBotones()
        observarDatos()
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarDatosMes()
    }

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
        listaSupermercados = findViewById(R.id.listaSupermercados)
        txtSinSupermercados = findViewById(R.id.txtSinSupermercados)
        setSupportActionBar(toolbar)
    }

    private fun configurarListaSupermercados() {
        adapter = SupermercadoAdapter()
        listaSupermercados.layoutManager = LinearLayoutManager(this)
        listaSupermercados.adapter = adapter
    }

    private fun mostrarDatosHogar() {
        val codigo = hogarManager.obtenerCodigoHogar()
        txtCodigoHogar.text = if (codigo != null) "Hogar: $codigo" else "Hogar: no configurado"
    }

    private fun observarDatos() {
        // Supermercados
        viewModel.supermercados.observe(this) { lista ->
            if (lista.isNullOrEmpty()) {
                listaSupermercados.visibility = View.GONE
                txtSinSupermercados.visibility = View.VISIBLE
            } else {
                listaSupermercados.visibility = View.VISIBLE
                txtSinSupermercados.visibility = View.GONE
                adapter.actualizarLista(lista)
            }
        }

        // Gasto del mes
        viewModel.gastoMes.observe(this) { gasto ->
            txtGastoMes.text = "${"%.2f".format(gasto)} €"
        }

        // Número de tickets
        viewModel.numTickets.observe(this) { num ->
            txtNumTickets.text = when (num) {
                0 -> "Sin tickets este mes"
                1 -> "1 ticket este mes"
                else -> "$num tickets este mes"
            }
        }

        // Último ticket
        viewModel.ultimoTicket.observe(this) { texto ->
            // Solo mostrar si no hay alertas
        }

        // Alertas no leídas — se muestran en la tarjeta de alertas
        viewModel.alertasNoLeidas.observe(this) { alertas ->
            if (alertas.isNullOrEmpty()) {
                // Sin alertas — mostrar último ticket o mensaje neutro
                val ultimoTexto = viewModel.ultimoTicket.value
                    ?: "Sin alertas por ahora.\nEscanea tu primer ticket para empezar."
                txtAlertas.text = ultimoTexto
            } else {
                // Mostrar las 3 alertas más recientes
                val sb = StringBuilder()
                alertas.take(3).forEach { alerta ->
                    sb.appendLine(alerta.mensaje)
                    if (alertas.indexOf(alerta) < minOf(2, alertas.size - 1)) {
                        sb.appendLine("─────────────────")
                    }
                }
                if (alertas.size > 3) {
                    sb.append("+ ${alertas.size - 3} alertas más")
                }
                txtAlertas.text = sb.toString().trimEnd()
            }
        }
    }

    private fun configurarBotones() {
        btnEscanear.setOnClickListener {
            startActivity(Intent(this, EscaneoActivity::class.java))
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

    private fun mostrarMensajeTemporal(mensaje: String) {
        Snackbar.make(findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_SHORT).show()
    }
}
