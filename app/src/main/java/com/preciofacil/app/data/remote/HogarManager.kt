package com.preciofacil.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * HogarManager — Gestiona el código de hogar compartido.
 *
 * El "hogar" es el grupo familiar que comparte los datos de la app.
 * En lugar de cuentas individuales con email/contraseña, todos los
 * miembros del hogar usan el mismo código de 6 caracteres.
 *
 * Cómo funciona:
 * 1. El primer móvil CREA el hogar → genera un código de 6 letras
 * 2. Los demás móviles UNEN AL HOGAR → introducen ese código
 * 3. Todos ven los mismos tickets, precios y alertas
 */
class HogarManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Preferencias locales — guardan el código de hogar en el móvil
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "preciofacil_hogar", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_CODIGO_HOGAR = "codigo_hogar"
        private const val KEY_HOGAR_ID = "hogar_id"
        private const val COLECCION_HOGARES = "hogares"
    }

    // ─────────────────────────────────────────────────────────────
    // ESTADO DEL HOGAR
    // ─────────────────────────────────────────────────────────────

    /**
     * Devuelve el código de hogar guardado en este móvil, o null si
     * el usuario todavía no ha creado ni unido ningún hogar.
     */
    fun obtenerCodigoHogar(): String? {
        return prefs.getString(KEY_CODIGO_HOGAR, null)
    }

    /**
     * Devuelve el ID interno del hogar (diferente al código visible).
     * Es el que se usa para acceder a los datos en Firestore.
     */
    fun obtenerHogarId(): String? {
        return prefs.getString(KEY_HOGAR_ID, null)
    }

    /**
     * Indica si este móvil ya está vinculado a un hogar.
     */
    fun tieneHogar(): Boolean {
        return obtenerCodigoHogar() != null
    }

    // ─────────────────────────────────────────────────────────────
    // CREAR UN HOGAR NUEVO
    // ─────────────────────────────────────────────────────────────

    /**
     * Crea un hogar nuevo y devuelve el código de 6 caracteres.
     * El primer miembro del hogar llama a esta función.
     *
     * Ejemplo de código generado: "XK7M2P"
     */
    suspend fun crearHogar(): ResultadoHogar {
        return try {
            // 1. Iniciar sesión anónima en Firebase (sin email ni contraseña)
            val usuario = auth.signInAnonymously().await().user
                ?: return ResultadoHogar.Error("No se pudo conectar con Firebase")

            // 2. Generar código de 6 caracteres legible
            val codigo = generarCodigo()

            // 3. Guardar el hogar en Firestore
            val datosHogar = hashMapOf(
                "codigo" to codigo,
                "creadoPor" to usuario.uid,
                "creadoEn" to System.currentTimeMillis(),
                "miembros" to listOf(usuario.uid)
            )

            val documentoRef = firestore
                .collection(COLECCION_HOGARES)
                .document() // ID automático de Firestore

            documentoRef.set(datosHogar).await()

            // 4. Guardar en el móvil para no tener que volver a introducirlo
            prefs.edit()
                .putString(KEY_CODIGO_HOGAR, codigo)
                .putString(KEY_HOGAR_ID, documentoRef.id)
                .apply()

            ResultadoHogar.Exito(codigo, documentoRef.id)

        } catch (e: Exception) {
            ResultadoHogar.Error("Error al crear el hogar: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // UNIRSE A UN HOGAR EXISTENTE
    // ─────────────────────────────────────────────────────────────

    /**
     * Un segundo móvil introduce el código de hogar para unirse.
     * Si el código es válido, este móvil queda vinculado al mismo hogar.
     */
    suspend fun unirseAlHogar(codigoIntroducido: String): ResultadoHogar {
        return try {
            val codigoLimpio = codigoIntroducido.trim().uppercase()

            // 1. Iniciar sesión anónima
            val usuario = auth.signInAnonymously().await().user
                ?: return ResultadoHogar.Error("No se pudo conectar con Firebase")

            // 2. Buscar el hogar con ese código en Firestore
            val resultado = firestore
                .collection(COLECCION_HOGARES)
                .whereEqualTo("codigo", codigoLimpio)
                .get()
                .await()

            if (resultado.isEmpty) {
                return ResultadoHogar.Error("Código incorrecto. Comprueba que lo has escrito bien.")
            }

            val documentoHogar = resultado.documents.first()
            val hogarId = documentoHogar.id

            // 3. Añadir este móvil a la lista de miembros
            @Suppress("UNCHECKED_CAST")
            val miembrosActuales = documentoHogar.get("miembros") as? List<String> ?: emptyList()
            if (!miembrosActuales.contains(usuario.uid)) {
                firestore.collection(COLECCION_HOGARES)
                    .document(hogarId)
                    .update("miembros", miembrosActuales + usuario.uid)
                    .await()
            }

            // 4. Guardar en el móvil
            prefs.edit()
                .putString(KEY_CODIGO_HOGAR, codigoLimpio)
                .putString(KEY_HOGAR_ID, hogarId)
                .apply()

            ResultadoHogar.Exito(codigoLimpio, hogarId)

        } catch (e: Exception) {
            ResultadoHogar.Error("Error al unirse al hogar: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CERRAR SESIÓN / SALIR DEL HOGAR
    // ─────────────────────────────────────────────────────────────

    /**
     * Desvincula este móvil del hogar. Los datos locales se mantienen,
     * pero se deja de sincronizar con la nube.
     */
    fun salirDelHogar() {
        prefs.edit()
            .remove(KEY_CODIGO_HOGAR)
            .remove(KEY_HOGAR_ID)
            .apply()
        auth.signOut()
    }

    // ─────────────────────────────────────────────────────────────
    // UTILIDADES PRIVADAS
    // ─────────────────────────────────────────────────────────────

    /**
     * Genera un código de 6 caracteres fácil de leer y escribir.
     * Solo usa letras y números que no se confunden visualmente
     * (sin 0/O, sin 1/I/L).
     */
    private fun generarCodigo(): String {
        val caracteres = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        return (1..6).map { caracteres.random() }.joinToString("")
    }
}

// ─────────────────────────────────────────────────────────────
// RESULTADOS — qué puede devolver HogarManager
// ─────────────────────────────────────────────────────────────

/**
 * Resultado sellado — solo puede ser Exito o Error.
 * Así el código que llama a HogarManager sabe exactamente
 * qué pasó sin tener que gestionar excepciones.
 */
sealed class ResultadoHogar {
    data class Exito(val codigo: String, val hogarId: String) : ResultadoHogar()
    data class Error(val mensaje: String) : ResultadoHogar()
}
