package com.preciofacil.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.preciofacil.app.parser.ProductoDetectado
import com.preciofacil.app.parser.ResultadoParser

/**
 * RevisionActivity — pantalla de revisión del ticket escaneado.
 *
 * Muestra cada producto detectado por el parser en una tarjeta editable.
 * El usuario puede:
 *   - Corregir el nombre si el OCR lo leyó mal
 *   - Corregir el precio
 *   - Marcar una línea como "no es producto" para ignorarla
 *   - Confirmar para guardar todo en la base de datos
 */
class RevisionActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var txtSupermercado: TextView
    private lateinit var txtResumen: TextView
    private lateinit var txtTotal: TextView
    private lateinit var contenedorProductos: LinearLayout
    private lateinit var btnConfirmar: MaterialButton

    // Datos recibidos del parser
    private var resultadoParser: ResultadoParser? = null

    // Lista de vistas de productos (para leer los valores editados)
    private val vistasProductos = mutableListOf<VistaProducto>()

    // Estructura para guardar referencia a cada tarjeta de producto
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

        // Recibir el resultado del parser desde EscaneoActivity
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

    // ── MOSTRAR DATOS ─────────────────────────────────────────────────

    private fun mostrarDatos(resultado: ResultadoParser) {
        txtSupermercado.text = "🏪 ${resultado.supermercado}"
        txtResumen.text = "${resultado.productos.size} productos detectados"
        txtTotal.text = if (resultado.totalTicket > 0) {
            "${"%.2f".format(resultado.totalTicket)} €"
        } else {
            "—"
        }

        // Crear una tarjeta por cada producto
        vistasProductos.clear()
        contenedorProductos.removeAllViews()

        resultado.productos.forEach { producto ->
            agregarTarjetaProducto(producto)
        }
    }

    private fun agregarTarjetaProducto(producto: ProductoDetectado) {
        val inflater = LayoutInflater.from(this)
        val vista = inflater.inflate(R.layout.item_producto_revision, contenedorProductos, false)

        val txtEAN = vista.findViewById<TextView>(R.id.txtEAN)
        val editNombre = vista.findViewById<TextInputEditText>(R.id.editNombre)
        val editPrecio = vista.findViewById<TextInputEditText>(R.id.editPrecio)
        val btnEliminar = vista.findViewById<MaterialButton>(R.id.btnEliminar)
        val card = vista.findViewById<View>(R.id.cardProducto)

        // Rellenar con los datos del parser
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

        // Botón "No es producto" — atenúa la tarjeta y la marca para ignorar
        btnEliminar.setOnClickListener {
            val idx = vistasProductos.indexOf(vistaProducto)
            if (idx >= 0) {
                val descartado = vistasProductos[idx].descartado
                vistasProductos[idx] = vistasProductos[idx].copy(descartado = !descartado)

                if (!descartado) {
                    // Marcar como descartado — atenuar
                    vista.alpha = 0.4f
                    btnEliminar.text = "↩ Restaurar"
                } else {
                    // Restaurar
                    vista.alpha = 1.0f
                    btnEliminar.text = "✕ No es producto"
                }
            }
        }

        contenedorProductos.addView(vista)
    }

    // ── CONFIRMAR Y GUARDAR ───────────────────────────────────────────

    private fun confirmarYGuardar() {
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

        // TODO Fase 4E: aquí se guardarán en Room
        // Por ahora mostramos un mensaje de éxito
        val mensaje = "✅ ${productosConfirmados.size} productos listos para guardar"
        Snackbar.make(findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_LONG).show()

        // Volver al Dashboard después de un momento
        btnConfirmar.postDelayed({
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }, 1500)
    }

    private fun mostrarError(mensaje: String) {
        Snackbar.make(findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_LONG).show()
    }
}
