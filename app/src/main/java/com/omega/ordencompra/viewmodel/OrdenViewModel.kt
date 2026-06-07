package com.omega.ordencompra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omega.ordencompra.data.db.entities.CatalogoProductoEntity
import com.omega.ordencompra.data.db.entities.ClienteEntity
import com.omega.ordencompra.data.db.entities.HistorialEntity
import com.omega.ordencompra.data.db.entities.OrdenEntity
import com.omega.ordencompra.data.db.entities.ProductoEntity
import com.omega.ordencompra.data.firebase.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrdenViewModel @Inject constructor(
    application: Application,
    private val repo: FirebaseRepository
) : AndroidViewModel(application) {

    val ordenes: StateFlow<List<OrdenEntity>> = repo.getOrdenes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentOrden = MutableStateFlow<OrdenEntity?>(null)
    val currentOrden: StateFlow<OrdenEntity?> = _currentOrden.asStateFlow()

    private val _currentProductos = MutableStateFlow<List<ProductoEntity>>(emptyList())
    val currentProductos: StateFlow<List<ProductoEntity>> = _currentProductos.asStateFlow()

    val catalogoProductos: StateFlow<List<CatalogoProductoEntity>> = repo.getCatalogoProductos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val catalogo: StateFlow<List<CatalogoProductoEntity>> = catalogoProductos

    val clientesList: StateFlow<List<ClienteEntity>> = repo.getClientes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val clientes: StateFlow<List<ClienteEntity>> = clientesList

    private val _lastCreatedOrdenId = MutableStateFlow<String?>(null)
    val lastCreatedOrdenId: StateFlow<String?> = _lastCreatedOrdenId.asStateFlow()

    var currentUserName: String = ""

    fun getHistorialByOrdenId(ordenId: String): Flow<List<HistorialEntity>> = repo.getHistorialByOrdenId(ordenId)

    private fun fechaHoraActual(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale("es", "MX")).format(java.util.Date())
    }

    private suspend fun registrarHistorial(ordenId: String, accion: String, detalle: String) {
        val historial = HistorialEntity(
            ordenId = ordenId,
            usuarioId = "",
            usuarioNombre = currentUserName,
            accion = accion,
            detalle = detalle,
            fecha = fechaHoraActual()
        )
        repo.insertHistorial(historial)
    }

    fun loadOrdenes() {} // Auto-collected via init
    fun loadCatalogo() {} // Auto-collected via init
    fun loadClientesList() {} // Auto-collected via init

    fun loadOrden(id: String) {
        viewModelScope.launch {
            repo.getOrdenById(id).onSuccess { _currentOrden.value = it }
        }
    }

    fun loadProductos(ordenId: String) {
        viewModelScope.launch {
            repo.getProductosByOrdenId(ordenId).collect { _currentProductos.value = it }
        }
    }

    fun insertOrdenWithProductos(orden: OrdenEntity, productos: List<ProductoEntity>, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            repo.insertOrden(orden).onSuccess { ordenId ->
                productos.forEach { producto ->
                    repo.insertProducto(producto.copy(ordenId = ordenId))
                }
                _lastCreatedOrdenId.value = ordenId
                onComplete(ordenId)
            }.onFailure { it.printStackTrace() }
        }
    }

    fun updateOrden(orden: OrdenEntity) {
        viewModelScope.launch {
            val oldEstado = _currentOrden.value?.estado
            val newEstado = orden.estado
            repo.updateOrden(orden).onSuccess {
                _currentOrden.value = orden
                if (oldEstado != null && oldEstado != newEstado) {
                    if (newEstado.equals("Cancelada", ignoreCase = true) || newEstado.equals("rechazada", ignoreCase = true)) {
                        repo.getProductosByOrdenIdOnce(orden.id).onSuccess { productos ->
                            productos.forEach { prod ->
                                repo.getCatalogoById(prod.productoCatalogoId).onSuccess { catProd ->
                                    if (catProd != null) {
                                        repo.updateProductoCatalogo(catProd.copy(stock = catProd.stock + prod.cantidad))
                                    }
                                }
                            }
                        }
                    }
                    registrarHistorial(orden.id, "Estado cambiado", "Estado: $oldEstado → $newEstado")
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    fun updateOrdenConProductos(orden: OrdenEntity, productos: List<ProductoEntity>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repo.getProductosByOrdenIdOnce(orden.id).onSuccess { oldProductos ->
                oldProductos.forEach { oldProd ->
                    repo.getCatalogoById(oldProd.productoCatalogoId).onSuccess { catProd ->
                        if (catProd != null) {
                            repo.updateProductoCatalogo(catProd.copy(stock = catProd.stock + oldProd.cantidad))
                        }
                    }
                }
                productos.forEach { prod ->
                    repo.getCatalogoById(prod.productoCatalogoId).onSuccess { catProd ->
                        if (catProd != null) {
                            val newStock = (catProd.stock - prod.cantidad).coerceAtLeast(0)
                            repo.updateProductoCatalogo(catProd.copy(stock = newStock))
                        }
                    }
                }
                repo.deleteProductosByOrdenId(orden.id)
                productos.forEach { producto ->
                    repo.insertProducto(producto.copy(ordenId = orden.id, id = ""))
                }
                val newTotal = productos.sumOf { it.total }
                repo.updateOrden(orden.copy(total = newTotal))
                registrarHistorial(orden.id, "Productos editados", "Productos actualizados. Nuevo total: $newTotal")
                _currentOrden.value = orden.copy(total = newTotal)
                onComplete()
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
                fecha = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("es", "MX")).format(java.util.Date()),
                estado = "Pendiente",
                total = productosOriginales.sumOf { it.total },
                observaciones = "",
                numeroOrden = numero
            )
            repo.insertOrden(nuevaOrden).onSuccess { ordenId ->
                productosOriginales.forEach { prod ->
                    repo.insertProducto(prod.copy(ordenId = ordenId, id = ""))
                }
                productosOriginales.forEach { prod ->
                    repo.getCatalogoById(prod.productoCatalogoId).onSuccess { catProd ->
                        if (catProd != null) {
                            val newStock = (catProd.stock - prod.cantidad).coerceAtLeast(0)
                            repo.updateProductoCatalogo(catProd.copy(stock = newStock))
                        }
                    }
                }
                registrarHistorial(ordenId, "Duplicada", "Orden duplicada desde OC-${ordenOriginal.numeroOrden}")
                _lastCreatedOrdenId.value = ordenId
                onSuccess(ordenId)
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
                fecha = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("es", "MX")).format(java.util.Date()),
                estado = "Pendiente",
                total = total,
                observaciones = "",
                numeroOrden = numero
            )
            repo.insertOrden(orden).onSuccess { ordenId ->
                productos.forEach { producto ->
                    repo.insertProducto(producto.copy(ordenId = ordenId))
                }
                // Decrement stock after order is confirmed
                productos.forEach { prod ->
                    repo.getCatalogoById(prod.productoCatalogoId).onSuccess { catalogProd ->
                        if (catalogProd != null) {
                            val updatedStock = (catalogProd.stock - prod.cantidad).coerceAtLeast(0)
                            repo.updateProductoCatalogo(catalogProd.copy(stock = updatedStock))
                        }
                    }
                }
                _lastCreatedOrdenId.value = ordenId
                registrarHistorial(ordenId, "Creada", "Orden creada con ${productos.size} producto(s). Total: $total")
                onSuccess(ordenId)
            }.onFailure {
                onError()
            }
        }
    }

    private val _topProductos = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val topProductos: StateFlow<List<Pair<String, Int>>> = _topProductos.asStateFlow()

    fun loadTopProductos(ordenIds: List<String>) {
        viewModelScope.launch {
            repo.getProductosByOrdenIdsOnce(ordenIds).onSuccess { allProductos ->
                val productosMap = mutableMapOf<String, Pair<String, Int>>()
                allProductos.forEach { p ->
                    val current = productosMap[p.productoCatalogoId]
                    productosMap[p.productoCatalogoId] = Pair(
                        p.nombre,
                        (current?.second ?: 0) + p.cantidad
                    )
                }
                _topProductos.value = productosMap.entries
                    .map { (id, pair) -> id to pair.second }
                    .sortedByDescending { it.second }
                    .take(10)
            }
        }
    }

    fun deleteOrden(orden: OrdenEntity) {
        viewModelScope.launch {
            repo.getProductosByOrdenIdOnce(orden.id).onSuccess { productos ->
                productos.forEach { prod ->
                    repo.getCatalogoById(prod.productoCatalogoId).onSuccess { catProd ->
                        if (catProd != null) {
                            repo.updateProductoCatalogo(catProd.copy(stock = catProd.stock + prod.cantidad))
                        }
                    }
                }
            }
            repo.deleteProductosByOrdenId(orden.id)
            repo.deleteOrden(orden)
        }
    }
}
