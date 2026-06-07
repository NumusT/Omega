package com.omega.ordencompra.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.toObject
import com.omega.ordencompra.data.db.entities.CatalogoProductoEntity
import com.omega.ordencompra.data.db.entities.ClienteEntity
import com.omega.ordencompra.data.db.entities.HistorialEntity
import com.omega.ordencompra.data.db.entities.OrdenEntity
import com.omega.ordencompra.data.db.entities.ProductoEntity
import com.omega.ordencompra.data.db.entities.UserEntity
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance().apply {
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        firestoreSettings = settings
    }

    private val usuariosRef = db.collection("usuarios")
    private val clientesRef = db.collection("clientes")
    private val catalogoRef = db.collection("catalogo_productos")
    private val ordenesRef = db.collection("ordenes")
    private val productosRef = db.collection("productos")
    private val historialRef = db.collection("historial")

    fun getUsers(): Flow<List<UserEntity>> = callbackFlow {
        val reg = usuariosRef.addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { it.toObject<UserEntity>()?.copy(id = it.id) } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun getClientes(): Flow<List<ClienteEntity>> = callbackFlow {
        val reg = clientesRef.orderBy("nombre").addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val nombre = doc.getString("nombre") ?: ""
                    val telefono = doc.getString("telefono") ?: ""
                    val email = doc.getString("email") ?: ""
                    val direccion = doc.getString("direccion") ?: ""
                    val rif = doc.getString("rif") ?: ""
                    ClienteEntity(id, nombre, telefono, email, direccion, rif)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun getCatalogoProductos(): Flow<List<CatalogoProductoEntity>> = callbackFlow {
        val reg = catalogoRef.orderBy("nombre").addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val codigo = doc.getString("codigo") ?: ""
                    val nombre = doc.getString("nombre") ?: ""
                    val stock = doc.getLong("stock")?.toInt() ?: 0
                    val precioUnitario = doc.getDouble("precioUnitario")
                        ?: doc.getLong("precioUnitario")?.toDouble()
                        ?: 0.0
                    CatalogoProductoEntity(id, codigo, nombre, stock, precioUnitario)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun getOrdenes(): Flow<List<OrdenEntity>> = callbackFlow {
        val reg = ordenesRef.orderBy("fecha", Query.Direction.DESCENDING).addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val numeroOrden = doc.getString("numeroOrden") ?: ""
                    val clienteId = doc.getString("clienteId") ?: ""
                    val clienteNombre = doc.getString("clienteNombre") ?: ""
                    val usuarioId = doc.getString("usuarioId") ?: ""
                    val fecha = doc.getString("fecha") ?: ""
                    val total = doc.getDouble("total")
                        ?: doc.getLong("total")?.toDouble()
                        ?: 0.0
                    val estado = doc.getString("estado") ?: "Pendiente"
                    val observaciones = doc.getString("observaciones") ?: ""
                    OrdenEntity(id, numeroOrden, clienteId, clienteNombre, usuarioId, fecha, total, estado, observaciones)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun getProductosByOrdenId(ordenId: String): Flow<List<ProductoEntity>> = callbackFlow {
        val reg = productosRef.whereEqualTo("ordenId", ordenId).addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val ordId = doc.getString("ordenId") ?: ""
                    val prodCatId = doc.getString("productoCatalogoId") ?: ""
                    val nombre = doc.getString("nombre") ?: ""
                    val cantidad = doc.getLong("cantidad")?.toInt() ?: 0
                    val precioUnitario = doc.getDouble("precioUnitario")
                        ?: doc.getLong("precioUnitario")?.toDouble()
                        ?: 0.0
                    val total = doc.getDouble("total")
                        ?: doc.getLong("total")?.toDouble()
                        ?: 0.0
                    ProductoEntity(id, ordId, prodCatId, nombre, cantidad, precioUnitario, total)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun login(username: String, password: String): Result<UserEntity?> = runCatching {
        val snap = usuariosRef.whereEqualTo("username", username).whereEqualTo("password", password).get().await()
        snap.documents.firstOrNull()?.toObject<UserEntity>()?.copy(id = snap.documents.first().id)
    }

    suspend fun getUserByUsername(username: String): Result<UserEntity?> = runCatching {
        val snap = usuariosRef.whereEqualTo("username", username).get().await()
        snap.documents.firstOrNull()?.toObject<UserEntity>()?.copy(id = snap.documents.first().id)
    }

    suspend fun getClienteById(id: String): Result<ClienteEntity?> = runCatching {
        val doc = clientesRef.document(id).get().await()
        if (!doc.exists()) return@runCatching null
        val nombre = doc.getString("nombre") ?: ""
        val telefono = doc.getString("telefono") ?: ""
        val email = doc.getString("email") ?: ""
        val direccion = doc.getString("direccion") ?: ""
        val rif = doc.getString("rif") ?: ""
        ClienteEntity(doc.id, nombre, telefono, email, direccion, rif)
    }

    suspend fun getCatalogoById(id: String): Result<CatalogoProductoEntity?> = runCatching {
        val doc = catalogoRef.document(id).get().await()
        if (!doc.exists()) return@runCatching null
        val codigo = doc.getString("codigo") ?: ""
        val nombre = doc.getString("nombre") ?: ""
        val stock = doc.getLong("stock")?.toInt() ?: 0
        val precioUnitario = doc.getDouble("precioUnitario")
            ?: doc.getLong("precioUnitario")?.toDouble()
            ?: 0.0
        CatalogoProductoEntity(doc.id, codigo, nombre, stock, precioUnitario)
    }

    suspend fun getOrdenById(id: String): Result<OrdenEntity?> = runCatching {
        val doc = ordenesRef.document(id).get().await()
        if (!doc.exists()) return@runCatching null
        val numeroOrden = doc.getString("numeroOrden") ?: ""
        val clienteId = doc.getString("clienteId") ?: ""
        val clienteNombre = doc.getString("clienteNombre") ?: ""
        val usuarioId = doc.getString("usuarioId") ?: ""
        val fecha = doc.getString("fecha") ?: ""
        val total = doc.getDouble("total")
            ?: doc.getLong("total")?.toDouble()
            ?: 0.0
        val estado = doc.getString("estado") ?: "Pendiente"
        val observaciones = doc.getString("observaciones") ?: ""
        OrdenEntity(doc.id, numeroOrden, clienteId, clienteNombre, usuarioId, fecha, total, estado, observaciones)
    }

    suspend fun insertUser(user: UserEntity): Result<String> = runCatching {
        val doc = usuariosRef.add(user).await()
        doc.id
    }

    suspend fun updateUser(user: UserEntity): Result<Unit> = runCatching {
        user.id.takeIf { it.isNotBlank() }?.let { usuariosRef.document(it).set(user).await() }
    }

    suspend fun deleteUser(user: UserEntity): Result<Unit> = runCatching {
        user.id.takeIf { it.isNotBlank() }?.let { usuariosRef.document(it).delete().await() }
    }

    suspend fun insertCliente(cliente: ClienteEntity): Result<String> = runCatching {
        val doc = clientesRef.add(cliente).await()
        doc.id
    }

    suspend fun updateCliente(cliente: ClienteEntity): Result<Unit> = runCatching {
        cliente.id.takeIf { it.isNotBlank() }?.let { clientesRef.document(it).set(cliente).await() }
    }

    suspend fun deleteCliente(cliente: ClienteEntity): Result<Unit> = runCatching {
        cliente.id.takeIf { it.isNotBlank() }?.let { clientesRef.document(it).delete().await() }
    }

    suspend fun insertProductoCatalogo(producto: CatalogoProductoEntity): Result<String> = runCatching {
        if (producto.id.isNotBlank()) {
            catalogoRef.document(producto.id).set(producto).await()
            producto.id
        } else {
            val doc = catalogoRef.add(producto).await()
            doc.id
        }
    }

    suspend fun updateProductoCatalogo(producto: CatalogoProductoEntity): Result<Unit> = runCatching {
        producto.id.takeIf { it.isNotBlank() }?.let { catalogoRef.document(it).set(producto).await() }
    }

    suspend fun deleteProductoCatalogo(producto: CatalogoProductoEntity): Result<Unit> = runCatching {
        producto.id.takeIf { it.isNotBlank() }?.let { catalogoRef.document(it).delete().await() }
    }

    suspend fun insertOrden(orden: OrdenEntity): Result<String> = runCatching {
        val doc = ordenesRef.add(orden).await()
        doc.id
    }

    suspend fun updateOrden(orden: OrdenEntity): Result<Unit> = runCatching {
        orden.id.takeIf { it.isNotBlank() }?.let { ordenesRef.document(it).set(orden).await() }
    }

    suspend fun deleteOrden(orden: OrdenEntity): Result<Unit> = runCatching {
        orden.id.takeIf { it.isNotBlank() }?.let { ordenesRef.document(it).delete().await() }
    }

    suspend fun insertProducto(producto: ProductoEntity): Result<String> = runCatching {
        val doc = productosRef.add(producto).await()
        doc.id
    }

    suspend fun deleteProductosByOrdenId(ordenId: String): Result<Unit> = runCatching {
        val snap = productosRef.whereEqualTo("ordenId", ordenId).get().await()
        snap.documents.forEach { it.reference.delete().await() }
    }

    suspend fun getProductosByOrdenIdOnce(ordenId: String): Result<List<ProductoEntity>> = runCatching {
        val snap = productosRef.whereEqualTo("ordenId", ordenId).get().await()
        snap.documents.mapNotNull { doc ->
            ProductoEntity(
                id = doc.id,
                ordenId = doc.getString("ordenId") ?: "",
                productoCatalogoId = doc.getString("productoCatalogoId") ?: "",
                nombre = doc.getString("nombre") ?: "",
                cantidad = doc.getLong("cantidad")?.toInt() ?: 0,
                precioUnitario = doc.getDouble("precioUnitario")
                    ?: doc.getLong("precioUnitario")?.toDouble()
                    ?: 0.0,
                total = doc.getDouble("total")
                    ?: doc.getLong("total")?.toDouble()
                    ?: 0.0
            )
        }
    }

    suspend fun getProductosByOrdenIdsOnce(ordenIds: List<String>): Result<List<ProductoEntity>> = runCatching {
        val result = mutableListOf<ProductoEntity>()
        ordenIds.chunked(30).forEach { batch ->
            val snap = productosRef.whereIn("ordenId", batch).get().await()
            snap.documents.mapNotNull { doc ->
                try {
                    ProductoEntity(
                        id = doc.id,
                        ordenId = doc.getString("ordenId") ?: "",
                        productoCatalogoId = doc.getString("productoCatalogoId") ?: "",
                        nombre = doc.getString("nombre") ?: "",
                        cantidad = doc.getLong("cantidad")?.toInt() ?: 0,
                        precioUnitario = doc.getDouble("precioUnitario")
                            ?: doc.getLong("precioUnitario")?.toDouble()
                            ?: 0.0,
                        total = doc.getDouble("total")
                            ?: doc.getLong("total")?.toDouble()
                            ?: 0.0
                    )
                } catch (e: Exception) { null }
            }.let { result.addAll(it) }
        }
        result
    }

    suspend fun getNextOrderNumber(): Result<Int> = runCatching {
        val counterRef = db.collection("counters").document("ordenes")
        db.runTransaction { transaction ->
            val snapshot = transaction.get(counterRef)
            val next = snapshot.getLong("nextNumber")?.toInt() ?: 1
            transaction.set(counterRef, mapOf("nextNumber" to (next + 1)))
            next
        }.await()
    }

    suspend fun insertHistorial(historial: HistorialEntity): Result<String> = runCatching {
        val doc = historialRef.add(historial).await()
        doc.id
    }

    fun getHistorialByOrdenId(ordenId: String): Flow<List<HistorialEntity>> = callbackFlow {
        val reg = historialRef
            .whereEqualTo("ordenId", ordenId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    try {
                        HistorialEntity(
                            id = doc.id,
                            ordenId = doc.getString("ordenId") ?: "",
                            usuarioId = doc.getString("usuarioId") ?: "",
                            usuarioNombre = doc.getString("usuarioNombre") ?: "",
                            accion = doc.getString("accion") ?: "",
                            detalle = doc.getString("detalle") ?: "",
                            fecha = doc.getString("fecha") ?: ""
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    // PAGING METHODS
    fun getCatalogoProductosPaged(searchQuery: String = ""): Pager<com.google.firebase.firestore.DocumentSnapshot, CatalogoProductoEntity> {
        var query: Query = catalogoRef.orderBy(FieldPath.documentId())
        if (searchQuery.isNotBlank()) {
            query = query.whereGreaterThanOrEqualTo(FieldPath.documentId(), searchQuery)
                         .whereLessThanOrEqualTo(FieldPath.documentId(), searchQuery + "\uf8ff")
        }
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                FirestorePagingSource(query) { doc ->
                    try {
                        val id = doc.id
                        val codigo = doc.getString("codigo")?.takeIf { it.isNotBlank() } ?: id
                        val nombre = doc.getString("nombre") ?: ""
                        val stock = doc.getLong("stock")?.toInt() ?: 0
                        val precioUnitario = doc.getDouble("precioUnitario")
                            ?: doc.getLong("precioUnitario")?.toDouble()
                            ?: 0.0
                        CatalogoProductoEntity(id, codigo, nombre, stock, precioUnitario)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        )
    }

    fun getClientesPaged(searchQuery: String = ""): Pager<com.google.firebase.firestore.DocumentSnapshot, ClienteEntity> {
        var query: Query = clientesRef.orderBy("nombre")
        if (searchQuery.isNotBlank()) {
            query = query.whereGreaterThanOrEqualTo("nombre", searchQuery)
                         .whereLessThanOrEqualTo("nombre", searchQuery + "\uf8ff")
        }
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                FirestorePagingSource(query) { doc ->
                    try {
                        val id = doc.id
                        val nombre = doc.getString("nombre") ?: ""
                        val telefono = doc.getString("telefono") ?: ""
                        val email = doc.getString("email") ?: ""
                        val direccion = doc.getString("direccion") ?: ""
                        val rif = doc.getString("rif") ?: ""
                        ClienteEntity(id, nombre, telefono, email, direccion, rif)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        )
    }
}
