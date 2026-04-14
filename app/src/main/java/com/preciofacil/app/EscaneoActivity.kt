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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.preciofacil.app.parser.ParserCaprabo
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * EscaneoActivity — selecciona foto, hace OCR y lanza la pantalla de revisión.
 */
class EscaneoActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var imagenTicket: ImageView
    private lateinit var txtPlaceholderImagen: TextView
    private lateinit var btnSeleccionarFoto: MaterialButton
    private lateinit var btnProcesar: MaterialButton
    private lateinit var layoutCargando: LinearLayout
    private lateinit var cardResultado: MaterialCardView
    private lateinit var txtResultadoOCR: TextView

    private var imagenUri: Uri? = null

    companion object {
        private const val CODIGO_GALERIA = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escaneo)
        inicializarVistas()
        configurarBotones()
    }

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
        btnSeleccionarFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, CODIGO_GALERIA)
        }
        btnProcesar.setOnClickListener {
            val uri = imagenUri
            if (uri != null) procesarImagenConOCR(uri)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODIGO_GALERIA && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                imagenUri = uri
                imagenTicket.setImageURI(uri)
                imagenTicket.visibility = View.VISIBLE
                txtPlaceholderImagen.visibility = View.GONE
                btnProcesar.visibility = View.VISIBLE
                cardResultado.visibility = View.GONE
            }
        }
    }

    private fun procesarImagenConOCR(uri: Uri) {
        layoutCargando.visibility = View.VISIBLE
        btnProcesar.isEnabled = false
        lifecycleScope.launch {
            try {
                val imagen = InputImage.fromFilePath(this@EscaneoActivity, uri)
                val reconocedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val resultado = reconocedor.process(imagen).await()
                val textoExtraido = resultado.text

                if (textoExtraido.isBlank()) {
                    mostrarError("No se encontró texto en la imagen.")
                    return@launch
                }

                // Parser de Caprabo
                val resultadoParser = ParserCaprabo.parsear(textoExtraido)

                if (resultadoParser.productos.isEmpty()) {
                    mostrarError("No se detectaron productos. Intenta con otra foto.")
                    return@launch
                }

                // Abrir pantalla de revisión
                val intent = Intent(this@EscaneoActivity, RevisionActivity::class.java)
                intent.putExtra(RevisionActivity.EXTRA_RESULTADO_PARSER, resultadoParser)
                startActivity(intent)

            } catch (e: Exception) {
                mostrarError("No se pudo leer el ticket: ${e.message}")
            } finally {
                layoutCargando.visibility = View.GONE
                btnProcesar.isEnabled = true
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        Snackbar.make(findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_LONG).show()
    }
}
