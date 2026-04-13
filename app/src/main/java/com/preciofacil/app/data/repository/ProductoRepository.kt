package com.preciofacil.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.preciofacil.app.data.local.dao.ProductoDao
import com.preciofacil.app.data.local.entity.Producto
import com.preciofacil.app.data.remote.HogarManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * ProductoRepository — gestiona el catálogo de productos.
 *
 * El catálogo tiene dos niveles:
 * - GLOBAL: productos identificados por EAN, compartidos entre todos los usuarios
 * - HOGAR:  nombres personalizados que el hogar ha asignado a sus productos
 *
 * En esta versión (v1.0) todo va al nivel de hogar.
 * El catálogo global se implementará en v2.0.
 */
class ProductoRepository(
    private val dao: ProductoDao,
    private val hogarManager: HogarManager
) {

    private val firestore = FirebaseFirestore.getInstance()

    // ── LECTURA ──────────────────────────────────────────────────

    /**
     * Todos los productos, actualizándose en tiempo real.
     */
    fun obtenerTodos(): Flow<List<Producto>> = dao.obtenerTodos()

    /**
     * Busca productos cuyo nombre contiene el texto dado.
     * Es suspend (no Flow) porque es una búsqueda puntual.
     */
    suspend fun buscarPorNombre(texto: String): List<Producto> =
        dao.buscarPorNombre(texto)

    /**
     * Busca un producto por su código EAN (el "Ref:" del ticket).
     * Es la forma más fiable de identificar un producto.
     */
    suspend fun obtenerPorEan(ean: String): Producto? = dao.buscarPorEan(ean)

    /**
     * Busca un producto por su ID interno.
     */
    suspend fun obtenerPorId(id: Long): Producto? = dao.obtenerPorId(id)

    /**
     * Productos de una categoría concreta.
     */
    fun obtenerPorCategoria(categoria: String): Flow<List<Producto>> =
        dao.obtenerPorCategoria(categoria)

    /**
     * Productos marcados como habituales.
     */
    fun obtenerHabituales(): Flow<List<Producto>> = dao.obtenerHabituales()

    // ── ESCRITURA ─────────────────────────────────────────────────

    /**
     * Guarda un producto nuevo o actualiza uno existente.
     * Devuelve el ID asignado por Room.
     */
    suspend fun guardar(producto: Producto): Long {
        val idAsignado = dao.insertar(producto)

        val hogarId = hogarManager.obtenerHogarId() ?: return idAsignado
        val datos = producto.copy(id = idAsignado).aMapaFirestore()

        firestore
            .collection("hogares")
            .document(hogarId)
            .collection("productos")
            .document(idAsignado.toString())
            .set(datos)
            .await()

        return idAsignado
    }

    /**
     * Actualiza el nombre de un producto obteniendo primero el objeto completo
     * y luego guardando la versión modificada.
     */
    suspend fun renombrar(productoId: Long, nuevoNombre: String) {
        val producto = dao.obtenerPorId(productoId) ?: return
        val actualizado = producto.copy(nombreNormalizado = nuevoNombre)
        dao.actualizar(actualizado)

        val hogarId = hogarManager.obtenerHogarId() ?: return
        firestore
            .collection("hogares")
            .document(hogarId)
            .collection("productos")
            .document(productoId.toString())
            .update("nombreNormalizado", nuevoNombre)
            .await()
    }

    // ── SINCRONIZACIÓN ────────────────────────────────────────────

    /**
     * Descarga el catálogo de productos de Firestore.
     * Se llama al arrancar la app o cuando un segundo móvil se une al hogar.
     */
    suspend fun sincronizarDesdeNube() {
        val hogarId = hogarManager.obtenerHogarId() ?: return

        val documentos = firestore
            .collection("hogares")
            .document(hogarId)
            .collection("productos")
            .get()
            .await()

        documentos.forEach { doc ->
            val producto = doc.aProducto()
            if (producto != null) dao.insertar(producto)
        }
    }
}

// ── CONVERSIONES ──────────────────────────────────────────────────

private fun Producto.aMapaFirestore(): Map<String, Any?> = mapOf(
    "id" to id,
    "nombreNormalizado" to nombreNormalizado,
    "categoria" to categoria,
    "codigoEan" to codigoEan,
    "esHabitual" to esHabitual,
    "alias" to alias,
    "tipoIvaEspana" to tipoIvaEspana,
    "pesoVolumen" to pesoVolumen,
    "unidadMedida" to unidadMedida
)

private fun com.google.firebase.firestore.DocumentSnapshot.aProducto(): Producto? {
    return try {
        Producto(
            id = getLong("id") ?: 0L,
            nombreNormalizado = getString("nombreNormalizado") ?: return null,
            categoria = getString("categoria") ?: "",
            codigoEan = getString("codigoEan") ?: "",
            esHabitual = getBoolean("esHabitual") ?: false,
            alias = getString("alias") ?: "",
            tipoIvaEspana = getString("tipoIvaEspana") ?: "reducido_10",
            pesoVolumen = getDouble("pesoVolumen"),
            unidadMedida = getString("unidadMedida") ?: "ud"
        )
    } catch (e: Exception) {
        null
    }
}
