package com.preciofacil.app.usecase

import com.preciofacil.app.data.local.dao.AlertaDao
import com.preciofacil.app.data.local.dao.ProductoDao
import com.preciofacil.app.data.local.dao.RegistroPrecioDao
import com.preciofacil.app.data.local.entity.Alerta
import com.preciofacil.app.data.local.entity.RegistroPrecio
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/**
 * AlertaUseCase — compara precios nuevos con el historial y genera alertas.
 * Incluye el nombre del producto en el mensaje de alerta.
 */
class AlertaUseCase(
    private val registroPrecioDao: RegistroPrecioDao,
    private val alertaDao: AlertaDao,
    private val productoDao: ProductoDao
) {

    private val UMBRAL_EUROS = 0.05
    private val UMBRAL_PORCENTAJE = 1.0

    suspend fun generarAlertas(
        nuevosRegistros: List<RegistroPrecio>,
        supermercadoId: Long,
        ticketId: Long,
        nombreSuper: String
    ) {
        for (registro in nuevosRegistros) {
            analizarProducto(registro, supermercadoId, ticketId, nombreSuper)
        }
    }

    private suspend fun analizarProducto(
        registroNuevo: RegistroPrecio,
        supermercadoId: Long,
        ticketId: Long,
        nombreSuper: String
    ) {
        // Obtener TODO el historial del producto en este supermercado
        val historial = registroPrecioDao
            .obtenerHistorialEnSupermercado(registroNuevo.productoId, supermercadoId)
            .first()

        // Necesitamos al menos 2 registros para comparar
        if (historial.size < 2) return

        val precioNuevo = historial[0].precioConImpuesto
        val precioAnterior = historial[1].precioConImpuesto

        if (historial[1].esPrecioPromocional) return

        val variacionEuros = precioNuevo - precioAnterior
        val variacionPct = if (precioAnterior > 0) {
            (variacionEuros / precioAnterior) * 100
        } else return

        if (abs(variacionEuros) < UMBRAL_EUROS) return
        if (abs(variacionPct) < UMBRAL_PORCENTAJE) return

        // Obtener el nombre del producto
        val nombreProducto = productoDao.obtenerPorId(registroNuevo.productoId)
            ?.nombreNormalizado
            ?.uppercase()
            ?: "Producto desconocido"

        val tipo = if (variacionEuros > 0) "subida_precio" else "bajada_precio"
        val signo = if (variacionEuros > 0) "+" else ""
        val emoji = if (variacionEuros > 0) "📈" else "📉"

        val mensaje = "$emoji $nombreProducto\n" +
            "$nombreSuper: $signo${"%.2f".format(variacionEuros)}€ " +
            "($signo${"%.1f".format(variacionPct)}%)\n" +
            "Antes: ${"%.2f".format(precioAnterior)}€ → " +
            "Ahora: ${"%.2f".format(precioNuevo)}€"

        val alerta = Alerta(
            tipo = tipo,
            productoId = registroNuevo.productoId,
            supermercadoId = supermercadoId,
            ticketId = ticketId,
            mensaje = mensaje,
            variacionEuros = variacionEuros,
            variacionPorcentaje = variacionPct,
            fecha = System.currentTimeMillis(),
            leida = false
        )

        alertaDao.insertar(alerta)
    }
}
