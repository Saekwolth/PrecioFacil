package com.preciofacil.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.Task
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * EscaneoActivity — pantalla para escanear un ticket de supermercado.
 *
 * Paso 4A: el usuario elige una foto de la galería.
 * Paso 4B: ML Kit extrae el texto de la imagen (OCR).
 *
 * En próximas fases esta pantalla también:
 * - Parsea el texto para identificar productos y precios
 * - Muestra la pantalla de revisión antes de guardar
 */
class EscaneoActivity : AppCompatActivity() {

    // ── VISTAS ────────────────────────────────────────────────────────
    private lateinit var toolbar: MaterialToolbar
    private lateinit var imagenTicket: ImageView
    private lateinit var txtPlaceholderImagen: TextView
    private lateinit var btnSeleccionarFoto: MaterialButton
    private lateinit var btnProcesar: MaterialButton
    private lateinit var layoutCargando: LinearLayout
    private lateinit var cardResultado: MaterialCardView
    private lateinit var txtResultadoOCR: TextView

    // URI de la imagen elegida por el usuario
    private var imagenUri: Uri? = null

    // Código para identificar el resultado de la galería
    companion object {
        private const val CODIGO_GALERIA = 1001
    }

    // ─────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escaneo)

        inicializarVistas()
        configurarBotones()
    }

    // ── INICIALIZACIÓN ────────────────────────────────────────────────

    private fun inicializarVistas() {
        toolbar = findViewById(R.id.toolbar)
        imagenTicket = findViewById(R.id.imagenTicket)
        txtPlaceholderImagen = findViewById(R.id.txtPlaceholderImagen)
        btnSeleccionarFoto = findViewById(R.id.btnSeleccionarFoto)
        btnProcesar = findViewById(R.id.btnProcesar)
        layoutCargando = findViewById(R.id.layoutCargando)
        cardResultado = findViewById(R.id.cardResultado)
        txtResultadoOCR = findViewById(R.id.txtResultadoOCR)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun configurarBotones() {
        // Abrir la galería de imágenes
        btnSeleccionarFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, CODIGO_GALERIA)
        }

        // Procesar la imagen con OCR
        btnProcesar.setOnClickListener {
            val uri = imagenUri
            if (uri != null) {
                procesarImagenConOCR(uri)
            }
        }
    }

    // ── RESULTADO DE LA GALERÍA ───────────────────────────────────────

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CODIGO_GALERIA && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                imagenUri = uri

                // Mostrar la imagen seleccionada
                imagenTicket.setImageURI(uri)
                imagenTicket.visibility = View.VISIBLE
                txtPlaceholderImagen.visibility = View.GONE

                // Mostrar el botón de procesar
                btnProcesar.visibility = View.VISIBLE

                // Ocultar resultado anterior si lo había
                cardResultado.visibility = View.GONE
            }
        }
    }

    // ── OCR CON ML KIT ───────────────────────────────────────────────

    /**
     * Lee el texto de la imagen usando Google ML Kit.
     * ML Kit analiza la foto y devuelve todo el texto que encuentra.
     */
    private fun procesarImagenConOCR(uri: Uri) {
        // Mostrar indicador de carga
        layoutCargando.visibility = View.VISIBLE
        btnProcesar.isEnabled = false

        lifecycleScope.launch {
            try {
                // Preparar la imagen para ML Kit
                val imagen = InputImage.fromFilePath(this@EscaneoActivity, uri)

                // Crear el reconocedor de texto
                val reconocedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                // Ejecutar el OCR
                val resultado = reconocedor.process(imagen).await()

                // Extraer el texto completo
                val textoExtraido = resultado.text

                // Mostrar el resultado
                mostrarResultadoOCR(textoExtraido)

            } catch (e: Exception) {
                mostrarError("No se pudo leer el ticket: ${e.message}")
            } finally {
                layoutCargando.visibility = View.GONE
                btnProcesar.isEnabled = true
            }
        }
    }

    private fun mostrarResultadoOCR(texto: String) {
        if (texto.isBlank()) {
            mostrarError("No se encontró texto en la imagen. Intenta con una foto más nítida.")
            return
        }

        txtResultadoOCR.text = texto
        cardResultado.visibility = View.VISIBLE
    }

    private fun mostrarError(mensaje: String) {
        Snackbar.make(findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_LONG).show()
    }
}
