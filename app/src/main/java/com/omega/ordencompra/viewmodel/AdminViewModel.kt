package com.omega.ordencompra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omega.ordencompra.data.db.entities.CatalogoProductoEntity
import com.omega.ordencompra.data.db.entities.ClienteEntity
import com.omega.ordencompra.data.firebase.FirebaseRepository
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
import javax.inject.Inject
import androidx.paging.PagingData
import androidx.paging.cachedIn

@HiltViewModel
class AdminViewModel @Inject constructor(
    application: Application,
    private val repo: FirebaseRepository
) : AndroidViewModel(application) {

    // Realtime flows (deprecated for large lists, kept for backward compatibility if needed)
    val productos: StateFlow<List<CatalogoProductoEntity>> = repo.getCatalogoProductos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientes: StateFlow<List<ClienteEntity>> = repo.getClientes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // PAGING & SEARCH - CATALOGO
    private val _searchQueryCatalogo = MutableStateFlow("")
    val searchQueryCatalogo: StateFlow<String> = _searchQueryCatalogo.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val productosPagingFlow: Flow<PagingData<CatalogoProductoEntity>> = _searchQueryCatalogo
        .flatMapLatest { query ->
            repo.getCatalogoProductosPaged(query).flow
        }.cachedIn(viewModelScope)

    fun searchCatalogo(query: String) {
        _searchQueryCatalogo.value = query
    }

    // PAGING & SEARCH - CLIENTES
    private val _searchQueryClientes = MutableStateFlow("")
    val searchQueryClientes: StateFlow<String> = _searchQueryClientes.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val clientesPagingFlow: Flow<PagingData<ClienteEntity>> = _searchQueryClientes
        .flatMapLatest { query ->
            repo.getClientesPaged(query).flow
        }.cachedIn(viewModelScope)

    fun searchClientes(query: String) {
        _searchQueryClientes.value = query
    }

    fun loadProductos() {} // Auto-collected via init
    fun loadClientes() {} // Auto-collected via init

    fun insertProducto(producto: CatalogoProductoEntity) {
        viewModelScope.launch {
            repo.insertProductoCatalogo(producto).onFailure { it.printStackTrace() }
        }
    }

    fun updateProducto(producto: CatalogoProductoEntity) {
        viewModelScope.launch {
            repo.updateProductoCatalogo(producto).onFailure { it.printStackTrace() }
        }
    }

    fun deleteProducto(producto: CatalogoProductoEntity) {
        viewModelScope.launch {
            repo.deleteProductoCatalogo(producto).onFailure { it.printStackTrace() }
        }
    }

    fun importProductosFromCsv(
        lines: List<String>,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                var count = 0
                val firstLine = lines.firstOrNull()
                val startIndex = if (firstLine != null && (
                            firstLine.contains("codigo", ignoreCase = true) ||
                            firstLine.contains("código", ignoreCase = true) ||
                            firstLine.contains("descripcion", ignoreCase = true) ||
                            firstLine.contains("descripción", ignoreCase = true) ||
                            firstLine.contains("nombre", ignoreCase = true) ||
                            firstLine.contains("precio", ignoreCase = true) ||
                            firstLine.contains("stock", ignoreCase = true)
                        )) 1 else 0
                
                for (i in startIndex until lines.size) {
                    val line = lines[i]
                    if (line.isBlank()) continue
                    try {
                        val parts = if (line.contains(";")) line.split(";") else line.split(",")
                        if (parts.isNotEmpty()) {
                            val codigo = parts[0].trim().removeSurrounding("\"")
                            if (codigo.isBlank()) continue
                            
                            val descripcion = parts.getOrNull(1)?.trim()?.removeSurrounding("\"") ?: ""
                            
                            val stock = parts.getOrNull(2)?.trim()?.removeSurrounding("\"")?.toIntOrNull() ?: 0
                            val precio = parts.getOrNull(3)?.trim()?.removeSurrounding("\"")?.toDoubleOrNull() ?: 0.0
                            
                            val producto = CatalogoProductoEntity(
                                id = codigo,
                                nombre = descripcion,
                                stock = stock,
                                precioUnitario = precio
                            )
                            repo.insertProductoCatalogo(producto).onSuccess { count++ }
                        }
                    } catch (e: Exception) {
                        // Skip malformed row
                    }
                }
                onSuccess(count)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al importar")
            }
        }
    }

    fun importClientesFromCsv(
        lines: List<String>,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                var count = 0
                val firstLine = lines.firstOrNull()
                val startIndex = if (firstLine != null && (
                            firstLine.contains("nombre", ignoreCase = true) ||
                            firstLine.contains("rif", ignoreCase = true) ||
                            firstLine.contains("correo", ignoreCase = true) ||
                            firstLine.contains("email", ignoreCase = true) ||
                            firstLine.contains("direccion", ignoreCase = true) ||
                            firstLine.contains("dirección", ignoreCase = true) ||
                            firstLine.contains("telefono", ignoreCase = true) ||
                            firstLine.contains("teléfono", ignoreCase = true)
                        )) 1 else 0

                for (i in startIndex until lines.size) {
                    val line = lines[i]
                    if (line.isBlank()) continue
                    try {
                        val parts = if (line.contains(";")) line.split(";") else line.split(",")
                        if (parts.isNotEmpty()) {
                            val nombre = parts[0].trim().removeSurrounding("\"")
                            if (nombre.isBlank()) continue

                            val rif = parts.getOrNull(1)?.trim()?.removeSurrounding("\"") ?: ""
                            val correo = parts.getOrNull(2)?.trim()?.removeSurrounding("\"") ?: ""
                            val direccion = parts.getOrNull(3)?.trim()?.removeSurrounding("\"") ?: ""

                            val cliente = ClienteEntity(
                                id = "",
                                nombre = nombre,
                                rif = rif,
                                email = correo,
                                direccion = direccion,
                                telefono = ""
                            )
                            repo.insertCliente(cliente).onSuccess { count++ }
                        }
                    } catch (e: Exception) {
                        // Skip malformed row
                    }
                }
                onSuccess(count)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al importar")
            }
        }
    }

    fun insertCliente(cliente: ClienteEntity) {
        viewModelScope.launch { repo.insertCliente(cliente).onFailure { it.printStackTrace() } }
    }

    fun updateCliente(cliente: ClienteEntity) {
        viewModelScope.launch { repo.updateCliente(cliente).onFailure { it.printStackTrace() } }
    }

    fun deleteCliente(cliente: ClienteEntity) {
        viewModelScope.launch { repo.deleteCliente(cliente).onFailure { it.printStackTrace() } }
    }
}
