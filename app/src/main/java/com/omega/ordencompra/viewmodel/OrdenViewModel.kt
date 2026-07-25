package com.omega.ordencompra.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omega.ordencompra.data.db.entities.CatalogoProductoEntity
import com.omega.ordencompra.data.db.entities.ClienteEntity
import com.omega.ordencompra.data.db.entities.HistorialEntity
import com.omega.ordencompra.data.db.entities.MensajeEntity
import com.omega.ordencompra.data.db.entities.NotificacionEntity
import com.omega.ordencompra.data.db.entities.OrdenEntity
import com.omega.ordencompra.data.db.entities.PrecioHistoricoEntity
import com.omega.ordencompra.data.db.entities.ProductoEntity
import com.omega.ordencompra.data.firebase.FirebaseRepository
import com.omega.ordencompra.util.NotificacionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import androidx.paging.PagingData
import androidx.paging.cachedIn

@HiltViewModel
class OrdenViewModel @Inject constructor(
    application: Application,
    private val repo: FirebaseRepository
) : AndroidViewModel(application) {

    val ordenes: StateFlow<List<OrdenEntity>> = repo.getOrdenes()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentOrden = MutableStateFlow<OrdenEntity?>(null)
    val currentOrden: StateFlow<OrdenEntity?> = _currentOrden.asStateFlow()

    private val _currentProductos = MutableStateFlow<List<ProductoEntity>>(emptyList())
    val currentProductos: StateFlow<List<ProductoEntity>> = _currentProductos.asStateFlow()

    val catalogo: StateFlow<List<CatalogoProductoEntity>> = repo.getCatalogoProductos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val clientes: StateFlow<List<ClienteEntity>> = repo.getClientes()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _lastCreatedOrdenId = MutableStateFlow<String?>(null)
    val lastCreatedOrdenId: StateFlow<String?> = _lastCreatedOrdenId.asStateFlow()

    var currentUserName: String = ""
    private val stockMutex = Mutex()

    // ORDER PAGING
    private val _ordenesPagingTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val ordenesPagingFlow: Flow<PagingData<OrdenEntity>> = _ordenesPagingTrigger
        .flatMapLatest { repo.getOrdenesPaged().flow }
        .cachedIn(viewModelScope)

    fun refreshOrdenesPaging() { _ordenesPagingTrigger.value++ }
    fun searchOrdenes(query: String) { }
    fun filterOrdenesByEstado(estado: String) { }

    // PRICE HISTORY
    private val _preciosHistoricos = MutableStateFlow<List<PrecioHistoricoEntity>>(emptyList())
    val preciosHistoricos: StateFlow<List<PrecioHistoricoEntity>> = _preciosHistoricos.asStateFlow()

    fun loadPreciosHistoricos(productoId: String) {
        viewModelScope.launch {
            repo.getPreciosHistoricosByProductoId(productoId).collect { _preciosHistoricos.value = it }
        }
    }

    fun trackPriceChange(producto: CatalogoProductoEntity, precioAnterior: Double, usuarioId: String) {
        if (precioAnterior == producto.precioUnitario) return
        viewModelScope.launch {
            val hist = PrecioHistoricoEntity(
                productoCatalogoId = producto.id,
                productoNombre = producto.nombre,
                precioAnterior = precioAnterior,
                precioNuevo = producto.precioUnitario,
                usuarioId = usuarioId,
                usuarioNombre = currentUserName,
                fecha = dateTimeFormat.format(java.util.Date())
            )
            repo.insertPrecioHistorico(hist)
        }
    }

    companion object {
        private val dateTimeFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale("es", "MX"))
        private val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("es", "MX"))
    }

    fun getHistorialByOrdenId(ordenId: String): Flow<List<HistorialEntity>> = repo.getHistorialByOrdenId(ordenId)
    fun getMensajesByOrdenId(ordenId: String): Flow<List<MensajeEntity>> = repo.getMensajesByOrdenId(ordenId)

    fun sendMensaje(ordenId: String, usuarioId: String, usuarioNombre: String, mensaje: String) {
        viewModelScope.launch {
            val msg = MensajeEntity(
                ordenId = ordenId,
                usuarioId = usuarioId,
                usuarioNombre = usuarioNombre,
                mensaje = mensaje,
                fecha = dateTimeFormat.format(java.util.Date())
            )
            repo.insertMensaje(msg)
        }
    }

    private fun fechaHoraActual(): String = dateTimeFormat.format(java.util.Date())

    private suspend fun registrarHistorial(ordenId: String, accion: String, detalle: String, usuarioId: String = "") {
        val historial = HistorialEntity(
            ordenId = ordenId,
            usuarioId = usuarioId,
            usuarioNombre = currentUserName,
            accion = accion,
            detalle = detalle,
            fecha = fechaHoraActual()
        )
        repo.insertHistorial(historial)
    }

    fun loadOrden(id: String) {
        viewModelScope.launch {
            repo.getOrdenById(id).onSuccess { _currentOrden.value = it }
        }
    }

    private var productosJob: kotlinx.coroutines.Job? = null

    fun loadProductos(ordenId: String) {
        productosJob?.cancel()
        productosJob = viewModelScope.launch {
            repo.getProductosByOrdenId(ordenId).collect { _currentProductos.value = it }
        }
    }

    suspend fun loadProductosForOrdenes(ordenIds: List<String>): Map<String, List<ProductoEntity>> {
        return repo.getProductosByOrdenIdsOnce(ordenIds)
            .getOrDefault(emptyList())
            .groupBy { it.ordenId }
    }

    fun insertOrdenWithProductos(orden: OrdenEntity, productos: List<ProductoEntity>, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            repo.insertOrden(orden).onSuccess { ordenId ->
                productos.forEach { producto ->
                    repo.insertProducto(producto.copy(ordenId = ordenId)).onFailure {
                        Log.w("OrdenVM", "insertProducto", it)
                    }
                }
                _lastCreatedOrdenId.value = ordenId
                onComplete(ordenId)
                refreshOrdenesPaging()
            }.onFailure { Log.w("OrdenVM", "insertOrden", it) }
        }
    }

    fun updateOrden(orden: OrdenEntity, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val oldEstado = _currentOrden.value?.estado
            val newEstado = orden.estado
            repo.updateOrden(orden).onSuccess {
                _currentOrden.value = orden
                if (oldEstado != null && oldEstado != newEstado) {
                    val isCancelOrReject = newEstado.equals("Cancelada", ignoreCase = true) || newEstado.equals("rechazada", ignoreCase = true)
                    val isApprove = newEstado.equals("Aprobada", ignoreCase = true)
                    val wasCanceled = oldEstado.equals("Cancelada", ignoreCase = true) || oldEstado.equals("rechazada", ignoreCase = true)
                    if (isCancelOrReject || (isApprove && wasCanceled)) {
                        repo.getProductosByOrdenIdOnce(orden.id).onSuccess { productos ->
                            productos.forEach { prod ->
                                val delta = if (isApprove) -prod.cantidad else prod.cantidad
                                repo.adjustStock(prod.productoCatalogoId, delta).onFailure {
                                    Log.w("OrdenVM", "adjustStock estado", it)
                                }
                            }
                        }.onFailure { Log.w("OrdenVM", "getProductos estado", it) }
                    }
                    launch { crearNotificacionOrden(orden.id, orden.numeroOrden, newEstado) }
                    launch {
                        val title = "OC-${orden.numeroOrden} - $newEstado"
                        val body = when {
                            newEstado.equals("Aprobada", ignoreCase = true) -> "Tu pedido ha sido aprobado"
                            newEstado.equals("rechazada", ignoreCase = true) -> "Tu pedido ha sido rechazado"
                            newEstado.equals("Cancelada", ignoreCase = true) -> "Tu pedido ha sido cancelado"
                            else -> "Tu pedido ahora está en estado: $newEstado"
                        }
                        repo.sendPushNotification(orden.usuarioId, title, body, orden.id)
                    }
                    if (isApprove) {
                        launch { notificarUsuario(orden.id, orden.numeroOrden) }
                    }
                    registrarHistorial(orden.id, "Estado cambiado", "Estado: $oldEstado → $newEstado", orden.usuarioId)
                }
                onComplete(true)
                refreshOrdenesPaging()
            }.onFailure { onComplete(false) }
        }
    }

    fun updateOrdenConProductos(orden: OrdenEntity, productos: List<ProductoEntity>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            stockMutex.withLock {
                repo.getProductosByOrdenIdOnce(orden.id).onSuccess { oldProductos ->
                    oldProductos.forEach { oldProd ->
                        repo.adjustStock(oldProd.productoCatalogoId, oldProd.cantidad).onFailure {
                            Log.w("OrdenVM", "adjustStock restore", it)
                        }
                    }
                    productos.forEach { prod ->
                        repo.adjustStock(prod.productoCatalogoId, -prod.cantidad).onFailure {
                            Log.w("OrdenVM", "adjustStock deduct", it)
                        }
                    }
                    repo.deleteProductosByOrdenId(orden.id).onFailure { Log.w("OrdenVM", "deleteProductos", it) }
                    productos.forEach { producto ->
                        repo.insertProducto(producto.copy(ordenId = orden.id, id = "")).onFailure {
                            Log.w("OrdenVM", "insertProducto edit", it)
                        }
                    }
                    val newTotal = productos.sumOf { it.total }
                    repo.updateOrden(orden.copy(total = newTotal)).onFailure { Log.w("OrdenVM", "updateTotal", it) }
                    registrarHistorial(orden.id, "Productos editados", "Productos actualizados. Nuevo total: $newTotal", orden.usuarioId)
                    _currentOrden.value = orden.copy(total = newTotal)
                    onComplete()
                    refreshOrdenesPaging()
                }.onFailure { Log.w("OrdenVM", "getProductos edit", it) }
            }
        }
    }

    fun duplicarOrden(ordenOriginal: OrdenEntity, productosOriginales: List<ProductoEntity>, usuarioId: String, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val nextNumber = repo.getNextOrderNumber().getOrNull()
            val numero = if (nextNumber != null) {
                nextNumber.toString().padStart(4, '0')
            } else {
                val max = ordenes.value.mapNotNull { it.numeroOrden.toIntOrNull() }.maxOrNull() ?: 0
                (max + 1).toString().padStart(4, '0')
            }
            val nuevaOrden = OrdenEntity(
                clienteId = ordenOriginal.clienteId,
                clienteNombre = ordenOriginal.clienteNombre,
                usuarioId = usuarioId,
                usuarioNombre = currentUserName,
                fecha = dateFormat.format(java.util.Date()),
                estado = "Pendiente",
                total = productosOriginales.sumOf { it.total },
                observaciones = "",
                numeroOrden = numero
            )
            repo.insertOrden(nuevaOrden).onSuccess { ordenId ->
                stockMutex.withLock {
                    productosOriginales.forEach { prod ->
                        repo.insertProducto(prod.copy(ordenId = ordenId, id = "")).onFailure {
                            Log.w("OrdenVM", "insertProducto dup", it)
                        }
                    }
                    productosOriginales.forEach { prod ->
                        repo.adjustStock(prod.productoCatalogoId, -prod.cantidad).onFailure {
                            Log.w("OrdenVM", "adjustStock dup", it)
                        }
                    }
                }
                registrarHistorial(ordenId, "Duplicada", "Orden duplicada desde OC-${ordenOriginal.numeroOrden}", usuarioId)
                _lastCreatedOrdenId.value = ordenId
                onSuccess(ordenId)
                refreshOrdenesPaging()
            }
        }
    }

    fun crearOrden(clienteId: String, clienteNombre: String, usuarioId: String, productos: List<ProductoEntity>, total: Double, onSuccess: (String) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val nextNumber = repo.getNextOrderNumber().getOrNull()
            val numero = if (nextNumber != null) {
                nextNumber.toString().padStart(4, '0')
            } else {
                val max = ordenes.value.mapNotNull { it.numeroOrden.toIntOrNull() }.maxOrNull() ?: 0
                (max + 1).toString().padStart(4, '0')
            }

            val orden = OrdenEntity(
                clienteId = clienteId,
                clienteNombre = clienteNombre,
                usuarioId = usuarioId,
                usuarioNombre = currentUserName,
                fecha = dateFormat.format(java.util.Date()),
                estado = "Pendiente",
                total = total,
                observaciones = "",
                numeroOrden = numero
            )
            repo.insertOrden(orden).onSuccess { ordenId ->
                stockMutex.withLock {
                    productos.forEach { producto ->
                        repo.insertProducto(producto.copy(ordenId = ordenId)).onFailure {
                            Log.w("OrdenVM", "insertProducto crear", it)
                        }
                    }
                    productos.forEach { prod ->
                        repo.adjustStock(prod.productoCatalogoId, -prod.cantidad).onFailure {
                            Log.w("OrdenVM", "adjustStock crear", it)
                        }
                    }
                }
                _lastCreatedOrdenId.value = ordenId
                registrarHistorial(ordenId, "Creada", "Orden creada con ${productos.size} producto(s). Total: $total", usuarioId)
                launch {
                    notificarAdmins(numero, clienteNombre)
                    val admins = repo.getUsersOnce().filter { it.rol == "admin" }
                    admins.forEach { admin ->
                        insertNotificacion(admin.id, "Nuevo pedido OC-$numero de $clienteNombre", "nueva_orden", ordenId)
                    }
                }
                onSuccess(ordenId)
                refreshOrdenesPaging()
            }.onFailure {
                onError()
            }
        }
    }

    private val _topProductos = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val topProductos: StateFlow<List<Pair<String, Double>>> = _topProductos.asStateFlow()

    fun loadTopProductos(ordenIds: List<String>) {
        viewModelScope.launch {
            repo.getProductosByOrdenIdsOnce(ordenIds).onSuccess { allProductos ->
                val productosMap = mutableMapOf<String, Pair<String, Double>>()
                allProductos.forEach { p ->
                    val current = productosMap[p.productoCatalogoId]
                    productosMap[p.productoCatalogoId] = Pair(
                        p.nombre,
                        (current?.second ?: 0.0) + p.cantidad
                    )
                }
                _topProductos.value = productosMap.entries
                    .map { (id, pair) -> id to pair.second }
                    .sortedByDescending { it.second }
                    .take(10)
            }
        }
    }

    private suspend fun insertNotificacion(usuarioId: String, mensaje: String, tipo: String, relacionId: String = "") {
        if (usuarioId.isBlank()) return
        val notif = NotificacionEntity(
            mensaje = mensaje,
            tipo = tipo,
            fecha = dateTimeFormat.format(java.util.Date()),
            usuarioId = usuarioId,
            relacionId = relacionId
        )
        repo.insertNotificacion(notif)
    }

    private suspend fun crearNotificacionOrden(ordenId: String, numeroOrden: String, estado: String) {
        repo.getOrdenById(ordenId).onSuccess { orden ->
            if (orden != null && orden.usuarioId.isNotBlank()) {
                val msg = when {
                    estado.equals("Aprobada", ignoreCase = true) -> "Tu pedido OC-$numeroOrden ha sido aprobado"
                    estado.equals("rechazada", ignoreCase = true) -> "Tu pedido OC-$numeroOrden ha sido rechazado"
                    else -> "Tu pedido OC-$numeroOrden está ahora $estado"
                }
                insertNotificacion(orden.usuarioId, msg, "orden", ordenId)
            }
        }
    }

    private suspend fun notificarAdmins(numeroOrden: String, clienteNombre: String) {
        val admins = repo.getUsersOnce().filter { it.rol == "admin" }
        val correos = admins.mapNotNull { it.email.takeIf { e -> e.isNotBlank() } }
        NotificacionService.notificarOrdenCreada(numeroOrden, clienteNombre, correos)
    }

    private suspend fun notificarUsuario(ordenId: String, numeroOrden: String) {
        repo.getOrdenById(ordenId).onSuccess { orden ->
            if (orden != null && orden.usuarioId.isNotBlank()) {
                repo.getUsersOnce().find { it.id == orden.usuarioId }?.let { user ->
                    if (user.email.isNotBlank()) {
                        NotificacionService.notificarOrdenAprobada(numeroOrden, user.email)
                    }
                }
            }
        }
    }

    fun batchApprove(ordenIds: List<String>, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            repo.batchApproveOrdenes(ordenIds).onSuccess { count ->
                ordenIds.forEach { id ->
                    registrarHistorial(id, "Aprobación batch", "Aprobada en lote", "")
                    launch {
                        repo.getOrdenById(id).onSuccess { orden ->
                            if (orden != null) {
                                crearNotificacionOrden(id, orden.numeroOrden, "Aprobada")
                            }
                        }
                    }
                }
                onComplete(count)
                refreshOrdenesPaging()
            }.onFailure { onComplete(0) }
        }
    }

    fun deleteOrden(orden: OrdenEntity, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            stockMutex.withLock {
                repo.getProductosByOrdenIdOnce(orden.id).onSuccess { productos ->
                    productos.forEach { prod ->
                        repo.adjustStock(prod.productoCatalogoId, prod.cantidad).onFailure {
                            Log.w("OrdenVM", "adjustStock delete", it)
                        }
                    }
                }.onFailure { Log.w("OrdenVM", "getProductos delete", it) }
                repo.deleteProductosByOrdenId(orden.id).onFailure { Log.w("OrdenVM", "deleteProductos", it) }
                repo.deleteOrden(orden).onSuccess { onComplete(true); refreshOrdenesPaging() }.onFailure { onComplete(false) }
            }
        }
    }
}
