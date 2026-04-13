package com.preciofacil.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.preciofacil.app.data.local.dao.SupermercadoDao
import com.preciofacil.app.data.local.entity.Supermercado
import com.preciofacil.app.data.remote.HogarManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * SupermercadoRepository — gestiona los supermercados.
 *
 * Regla de oro (offline-first):
 *   LEER  → siempre desde Room (base de datos local del móvil)
 *   ESCRIBIR → primero Room, luego Firestore en segundo plano
 *
 * La pantalla nunca espera a Firebase para mostrar datos.
 */
class SupermercadoRepository(
    private val dao: SupermercadoDao,
    private val hogarManager: HogarManager
) {

    private val firestore = FirebaseFirestore.getInstance()

    // ── LECTURA ──────────────────────────────────────────────────

    /**
     * Lista de supermercados activos en tiempo real.
     * Flow = se actualiza automáticamente cuando cambia la base de datos.
     */
    fun obtenerTodos(): Flow<List<Supermercado>> = dao.obtenerTodos()

    /**
     * Busca un supermercado por su ID.
     */
    suspend fun obtenerPorId(id: Long): Supermercado? = dao.obtenerPorId(id)

    // ── ESCRITURA ─────────────────────────────────────────────────

    /**
     * Guarda un supermercado nuevo o actualiza uno existente.
     * 1. Lo guarda en Room (inmediato, sin internet)
     * 2. Lo sube a Firestore si hay hogar configurado
     */
    suspend fun guardar(supermercado: Supermercado) {
        // Paso 1: guardar local
        val idAsignado = dao.insertar(supermercado)

        // Paso 2: sincronizar con la nube (si hay hogar)
        val hogarId = hogarManager.obtenerHogarId() ?: return
        val datos = supermercado.copy(id = idAsignado).aMapaFirestore()

        firestore
            .collection("hogares")
            .document(hogarId)
            .collection("supermercados")
            .document(idAsignado.toString())
            .set(datos)
            .await()
    }

    /**
     * Elimina un supermercado (lo desactiva, no lo borra del historial).
     */
    suspend fun desactivar(supermercado: Supermercado) {
        val desactivado = supermercado.copy(activo = false)
        dao.actualizar(desactivado)

        val hogarId = hogarManager.obtenerHogarId() ?: return
        firestore
            .collection("hogares")
            .document(hogarId)
            .collection("supermercados")
            .document(supermercado.id.toString())
            .update("activo", false)
            .await()
    }

    // ── SINCRONIZACIÓN ────────────────────────────────────────────

    /**
     * Descarga los supermercados de Firestore y los guarda en Room.
     * Se llama al arrancar la app si hay conexión.
     */
    suspend fun sincronizarDesdeNube() {
        val hogarId = hogarManager.obtenerHogarId() ?: return

        val documentos = firestore
            .collection("hogares")
            .document(hogarId)
            .collection("supermercados")
            .get()
            .await()

        documentos.forEach { doc ->
            val supermercado = doc.aSupermercado()
            if (supermercado != null) dao.insertar(supermercado)
        }
    }
}

// ── CONVERSIONES ──────────────────────────────────────────────────

/**
 * Convierte un Supermercado en un mapa para Firestore.
 */
private fun Supermercado.aMapaFirestore(): Map<String, Any?> = mapOf(
    "id" to id,
    "nombre" to nombre,
    "palabrasClave" to palabrasClave,
    "color" to color,
    "activo" to activo,
    "pais" to pais,
    "tipoImpuesto" to tipoImpuesto,
    "aplicaImpuesto" to aplicaImpuesto
)

/**
 * Convierte un documento de Firestore en un Supermercado.
 */
private fun com.google.firebase.firestore.DocumentSnapshot.aSupermercado(): Supermercado? {
    return try {
        Supermercado(
            id = getLong("id") ?: 0L,
            nombre = getString("nombre") ?: return null,
            palabrasClave = getString("palabrasClave") ?: "",
            color = getString("color") ?: "#1E8449",
            activo = getBoolean("activo") ?: true,
            pais = getString("pais") ?: "AD",
            tipoImpuesto = getString("tipoImpuesto") ?: "IGI_AD",
            aplicaImpuesto = getBoolean("aplicaImpuesto") ?: true
        )
    } catch (e: Exception) {
        null
    }
}
