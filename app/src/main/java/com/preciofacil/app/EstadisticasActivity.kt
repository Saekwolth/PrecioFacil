package com.preciofacil.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.appbar.MaterialToolbar
import com.preciofacil.app.data.local.database.PrecioFacilDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * EstadisticasActivity — muestra estadísticas de gasto y productos.
 *
 * - Gráfica de barras con gasto de los últimos 6 meses
 * - Comparativa este mes vs mes anterior
 * - Lista de productos conocidos con último precio
 * - Historial de variaciones de precio detectadas
 */
class EstadisticasActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var graficaGasto: BarChart
    private lateinit var txtGastoEsteMes: TextView
    private lateinit var txtGastoMesAnterior: TextView
    private lateinit var txtVariacionMes: TextView
    private lateinit var txtNumProductos: TextView
    private lateinit var contenedorProductos: LinearLayout
    private lateinit var contenedorAlertas: LinearLayout
    private lateinit var txtSinAlertas: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas)

        inicializarVistas()
        cargarDatos()
    }

    private fun inicializarVistas() {
        toolbar = findViewById(R.id.toolbar)
        graficaGasto = findViewById(R.id.graficaGasto)
        txtGastoEsteMes = findViewById(R.id.txtGastoEsteMes)
        txtGastoMesAnterior = findViewById(R.id.txtGastoMesAnterior)
        txtVariacionMes = findViewById(R.id.txtVariacionMes)
        txtNumProductos = findViewById(R.id.txtNumProductos)
        contenedorProductos = findViewById(R.id.contenedorProductos)
        contenedorAlertas = findViewById(R.id.contenedorAlertas)
        txtSinAlertas = findViewById(R.id.txtSinAlertas)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            val db = PrecioFacilDatabase.getInstance(applicationContext)

            // ── Gasto últimos 6 meses ────────────────────────────────
            val gastosMensuales = mutableListOf<Float>()
            val etiquetasMeses = mutableListOf<String>()
            val nombresMeses = listOf("Ene","Feb","Mar","Abr","May","Jun",
                                      "Jul","Ago","Sep","Oct","Nov","Dic")

            // calendario actual
            var gastoEsteMes = 0.0
            var gastoMesAnterior = 0.0

            for (i in 5 downTo 0) {
                val calMes = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -i)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val inicioMes = calMes.timeInMillis
                val finMes = Calendar.getInstance().apply {
                    timeInMillis = inicioMes
                    add(Calendar.MONTH, 1)
                    add(Calendar.MILLISECOND, -1)
                }.timeInMillis

                val gasto = db.ticketDao().calcularGastoTotal(inicioMes, finMes) ?: 0.0
                gastosMensuales.add(gasto.toFloat())
                etiquetasMeses.add(nombresMeses[calMes.get(Calendar.MONTH)])

                if (i == 0) gastoEsteMes = gasto
                if (i == 1) gastoMesAnterior = gasto
            }

            // Mostrar resumen textual
            txtGastoEsteMes.text = "${"%.2f".format(gastoEsteMes)} €"
            txtGastoMesAnterior.text = "${"%.2f".format(gastoMesAnterior)} €"

            if (gastoMesAnterior > 0) {
                val variacion = gastoEsteMes - gastoMesAnterior
                val variacionPct = (variacion / gastoMesAnterior) * 100
                val signo = if (variacion > 0) "+" else ""
                txtVariacionMes.text = "$signo${"%.1f".format(variacionPct)}%"
                txtVariacionMes.setTextColor(
                    if (variacion > 0) Color.parseColor("#C62828")
                    else Color.parseColor("#2E7D32")
                )
            } else {
                txtVariacionMes.text = "—"
            }

            // Dibujar gráfica
            dibujarGrafica(gastosMensuales, etiquetasMeses)

            // ── Productos conocidos ──────────────────────────────────
            val productos = db.productoDao().obtenerTodos().first()
            txtNumProductos.text = "${productos.size} productos en tu base de datos"

            contenedorProductos.removeAllViews()
            productos.take(10).forEach { producto ->
                // Buscar último precio
                val ultimoRegistro = db.registroPrecioDao()
                    .obtenerHistorialProducto(producto.id).first().firstOrNull()

                val fila = crearFilaProducto(
                    nombre = producto.nombreNormalizado.uppercase(),
                    precio = ultimoRegistro?.precioConImpuesto,
                    esUltimo = productos.indexOf(producto) == minOf(9, productos.size - 1)
                )
                contenedorProductos.addView(fila)
            }

            if (productos.size > 10) {
                val txtMas = TextView(this@EstadisticasActivity).apply {
                    text = "... y ${productos.size - 10} productos más"
                    textSize = 12f
                    setTextColor(Color.parseColor("#888888"))
                    setPadding(0, 8, 0, 0)
                }
                contenedorProductos.addView(txtMas)
            }

            // ── Alertas / variaciones ────────────────────────────────
            val alertas = db.alertaDao().obtenerPorImpactoEconomico().first()

            contenedorAlertas.removeAllViews()
            if (alertas.isEmpty()) {
                txtSinAlertas.visibility = View.VISIBLE
            } else {
                txtSinAlertas.visibility = View.GONE
                alertas.take(5).forEach { alerta ->
                    val txt = TextView(this@EstadisticasActivity).apply {
                        text = alerta.mensaje
                        textSize = 13f
                        setTextColor(Color.parseColor("#333333"))
                        setPadding(0, 0, 0, 16)
                        setLineSpacing(2f, 1f)
                    }
                    contenedorAlertas.addView(txt)
                }
            }
        }
    }

    private fun dibujarGrafica(valores: List<Float>, etiquetas: List<String>) {
        val entradas = valores.mapIndexed { idx, valor -> BarEntry(idx.toFloat(), valor) }
        val dataset = BarDataSet(entradas, "Gasto (€)").apply {
            color = Color.parseColor("#1A1A2E")
            valueTextColor = Color.parseColor("#444444")
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) =
                    if (value > 0) "${"%.0f".format(value)}€" else ""
            }
        }

        graficaGasto.apply {
            data = BarData(dataset).apply { barWidth = 0.6f }
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)
            setDrawBorders(false)
            animateY(800)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) =
                        etiquetas.getOrElse(value.toInt()) { "" }
                }
                textColor = Color.parseColor("#666666")
                textSize = 11f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#EEEEEE")
                textColor = Color.parseColor("#666666")
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            invalidate()
        }
    }

    private fun crearFilaProducto(nombre: String, precio: Double?, esUltimo: Boolean): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)

            if (!esUltimo) {
                setBackgroundResource(android.R.drawable.divider_horizontal_bright)
            }

            val txtNombre = TextView(this@EstadisticasActivity).apply {
                text = nombre
                textSize = 13f
                setTextColor(Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            val txtPrecio = TextView(this@EstadisticasActivity).apply {
                text = if (precio != null) "${"%.2f".format(precio)} €" else "—"
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1A1A2E"))
                gravity = Gravity.END
            }

            addView(txtNombre)
            addView(txtPrecio)
        }
    }
}
