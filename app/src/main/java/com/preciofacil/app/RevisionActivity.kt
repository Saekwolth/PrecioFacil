package com.preciofacil.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.preciofacil.app.data.local.database.PrecioFacilDatabase
import com.preciofacil.app.data.remote.HogarManager
import com.preciofacil.app.parser.ProductoDetectado
import com.preciofacil.app.parser.ResultadoParser
import com.preciofacil.app.usecase.GuardadoTicketUseCase
import kotlinx.coroutines.launch

class RevisionActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var txtSupermercado: TextView
    private lateinit var txtResumen: TextView
    private lateinit var txtTotal: TextView
    private lateinit var contenedorProductos: LinearLayout
    private lateinit var btnConfirmar: MaterialButton

    private var resultadoParser: ResultadoParser? = null
    private val vistasProductos = mutableListOf<VistaProducto>()

    data class VistaProducto(
        val productoOriginal: ProductoDetectado,
        val editNombre: TextInputEditText,
        val editPrecio: TextInputEditText,
        var descartado: Boolean = false,
        val card: View
    )

    companion object {
        const val EXTRA_RESULTADO_PARSER = "extra_resultado_parser"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_revision)
        inicializarVistas()

        @Suppress("DEPRECATION")
        resultadoParser = intent.getSerializableExtra(EXTRA_RESULTADO_PARSER) as? ResultadoParser

        val resultado = resultadoParser
        if (resultado == null) {
            mostrarError("Error al cargar los datos del ticket.")
            finish()
            return
        }
        mostrarDatos(resultado)
    }

    private fun inicializarVistas() {
        toolbar = findViewById(R.id.toolbar)
        txtSupermercado = findViewById(R.id.txtSupermercado)
        txtResumen = findViewById(R.id.txtResumen)
        txtTotal = findViewById(R.id.txtTotal)
        contenedorProductos = findViewById(R.id.contenedorProductos)
        btnConfirmar = findViewById(R.id.btnConfirmar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        btnConfirmar.setOnClickListener { confirmarYGuardar() }
    }

    private fun mostrarDatos(resultado: ResultadoParser) {
        txtSupermercado.text = "🏪 ${resultado.supermercado}"
        txtResumen.text = "${resultado.productos.size} productos detectados"
        txtTotal.text = if (resultado.totalTicket > 0) {
            "${"%.2f".format(resultado.totalTicket)} €"
        } else "—"
        vistasProductos.clear()
        contenedorProductos.removeAllViews()
        resultado.productos.forEach { agregarTarjetaProducto(it) }
    }

    private fun agregarTarjetaProducto(producto: ProductoDetectado) {
        val vista = LayoutInflater.from(this)
            .inflate(R.layout.item_producto_revision, contenedorProductos, false)

        val txtEAN = vista.findViewById<TextView>(R.id.txtEAN)
        val editNombre = vista.findViewById<TextInputEditText>(R.id.editNombre)
        val editPrecio = vista.findViewById<TextInputEditText>(R.id.editPrecio)
        val btnEliminar = vista.findViewById<MaterialButton>(R.id.btnEliminar)

        txtEAN.text = "EAN: ${producto.ean}"
        editNombre.setText(producto.nombre)
        editPrecio.setText(if (producto.precio != 0.0) "%.2f".format(producto.precio) else "")

        val vistaProducto = VistaProducto(
            productoOriginal = producto,
            editNombre = editNombre,
            editPrecio = editPrecio,
            card = vista
        )
        vistasProductos.add(vistaProducto)

        btnEliminar.setOnClickListener {
            val idx = vistasProductos.indexOf(vistaProducto)
            if (idx >= 0) {
                val estaDescartado = vistasProductos[idx].descartado
                vistasProductos[idx] = vistasProductos[idx].copy(descartado = !estaDescartado)
                if (!estaDescartado) {
                    vista.alpha = 0.4f
                    btnEliminar.text = "↩ Restaurar"
                } else {
                    vista.alpha = 1.0f
                    btnEliminar.text = "✕ No es producto"
                }
            }
        }
        contenedorProductos.addView(vista)
    }

    private fun confirmarYGuardar() {
        val resultado = resultadoParser ?: return

        val productosConfirmados = vistasProductos
            .filter { !it.descartado }
            .map { vista ->
                val nombreFinal = vista.editNombre.text.toString().trim()
                    .ifBlank { vista.productoOriginal.nombre }
                val precioFinal = vista.editPrecio.text.toString()
                    .replace(",", ".")
                    .toDoubleOrNull() ?: vista.productoOriginal.precio
                ProductoDetectado(
                    nombre = nombreFinal,
                    ean = vista.productoOriginal.ean,
                    precio = precioFinal,
                    esDescuento = precioFinal < 0
                )
            }

        if (productosConfirmados.isEmpty()) {
            mostrarError("No hay productos para guardar.")
            return
        }

        btnConfirmar.isEnabled = false
        btnConfirmar.text = "Guardando..."

        lifecycleScope.launch {
            try {
                val db = PrecioFacilDatabase.getInstance(applicationContext)
                val hogarManager = HogarManager(applicationContext)

                val useCase = GuardadoTicketUseCase(
                    ticketDao = db.ticketDao(),
                    lineaTicketDao = db.lineaTicketDao(),
                    productoDao = db.productoDao(),
                    registroPrecioDao = db.registroPrecioDao(),
                    alertaDao = db.alertaDao(),
                    hogarManager = hogarManager
                )

                // Supermercado ID 1 = Caprabo La Massana (creado por DatabaseSeeder)
                val supermercadoId = 1L

                val ticketId = useCase.guardar(
                    supermercadoId = supermercadoId,
                    totalTicket = resultado.totalTicket,
                    textoOcr = resultado.textoOriginal,
                    productos = productosConfirmados,
                    nombreSupermercado = resultado.supermercado
                )

                mostrarExito(productosConfirmados.size, ticketId)

            } catch (e: Exception) {
                mostrarError("Error al guardar: ${e.message}")
                btnConfirmar.isEnabled = true
                btnConfirmar.text = "💾  Confirmar y guardar"
            }
        }
    }

    private fun mostrarExito(numProductos: Int, ticketId: Long) {
        val mensaje = "✅ Ticket #$ticketId guardado — $numProductos productos"
        Snackbar.make(findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_LONG).show()
        btnConfirmar.postDelayed({
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }, 2000)
    }

    private fun mostrarError(mensaje: String) {
        Snackbar.make(findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_LONG).show()
    }
}
