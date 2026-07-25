package com.omega.ordencompra.data.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.toObject
import com.omega.ordencompra.data.db.entities.AccessLogEntity
import com.omega.ordencompra.data.db.entities.AjusteStockEntity
import com.omega.ordencompra.data.db.entities.MensajeEntity
import com.omega.ordencompra.data.db.entities.NotificacionEntity
import com.omega.ordencompra.data.db.entities.CatalogoProductoEntity
import com.omega.ordencompra.data.db.entities.ClienteEntity
import com.omega.ordencompra.data.db.entities.HistorialEntity
import com.omega.ordencompra.data.db.entities.OrdenEntity
import com.omega.ordencompra.data.db.entities.PrecioHistoricoEntity
import com.omega.ordencompra.data.db.entities.ProductoEntity
import com.omega.ordencompra.data.db.entities.UserEntity
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val ajustesStockRef = db.collection("ajustes_stock")
    private val accesoRef = db.collection("registro_acceso")
    private val notificacionesRef = db.collection("notificaciones")
    private val preciosHistoricosRef = db.collection("precios_historicos")
    private val mensajesRef = db.collection("mensajes")
    private val pushQueueRef = db.collection("push_queue")

    suspend fun getUsersOnce(): List<UserEntity> {
        val snap = usuariosRef.get().await()
        return snap.documents.mapNotNull { it.toObject<UserEntity>()?.copy(id = it.id) }
    }

    companion object { private const val TAG = "FirebaseRepository" }

    fun getUsers(): Flow<List<UserEntity>> = callbackFlow {
        val reg = usuariosRef.addSnapshotListener { snap, error ->
            if (error != null) { Log.w(TAG, "getUsers error", error); return@addSnapshotListener }
            val list = snap?.documents?.mapNotNull { it.toObject<UserEntity>()?.copy(id = it.id) } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    private val _clientes = MutableStateFlow<List<ClienteEntity>>(emptyList())
    private val _catalogoProductos = MutableStateFlow<List<CatalogoProductoEntity>>(emptyList())

    init {
        clientesRef.orderBy("nombre").addSnapshotListener { snap, error ->
            if (error != null) { Log.w(TAG, "clientes error", error); return@addSnapshotListener }
            _clientes.value = snap?.documents?.mapNotNull { doc ->
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
        }
        catalogoRef.orderBy("nombre").addSnapshotListener { snap, error ->
            if (error != null) { Log.w(TAG, "catalogo error", error); return@addSnapshotListener }
            _catalogoProductos.value = snap?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val codigo = doc.getString("codigo") ?: ""
                    val nombre = doc.getString("nombre") ?: ""
                    val stock = doc.getDouble("stock") ?: doc.getLong("stock")?.toDouble() ?: 0.0
                    val precioUnitario = doc.getDouble("precioUnitario")
                        ?: doc.getLong("precioUnitario")?.toDouble()
                        ?: 0.0
                    CatalogoProductoEntity(id, codigo, nombre, stock, precioUnitario, doc.getDouble("costo") ?: doc.getLong("costo")?.toDouble() ?: 0.0, doc.getString("fotoUrl") ?: "")
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
        }
    }

    fun getClientes(): Flow<List<ClienteEntity>> = _clientes.asStateFlow()

    fun getCatalogoProductos(): Flow<List<CatalogoProductoEntity>> = _catalogoProductos.asStateFlow()

    fun getOrdenes(): Flow<List<OrdenEntity>> = callbackFlow {
        val reg = ordenesRef.orderBy("fecha", Query.Direction.DESCENDING).limit(50).addSnapshotListener { snap, error ->
            if (error != null) { Log.w(TAG, "getOrdenes error", error); return@addSnapshotListener }
            val list = snap?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val numeroOrden = doc.getString("numeroOrden") ?: ""
                    val clienteId = doc.getString("clienteId") ?: ""
                    val clienteNombre = doc.getString("clienteNombre") ?: ""
                    val usuarioId = doc.getString("usuarioId") ?: ""
                    val usuarioNombre = doc.getString("usuarioNombre") ?: ""
                    val fecha = doc.getString("fecha") ?: ""
                    val total = doc.getDouble("total")
                        ?: doc.getLong("total")?.toDouble()
                        ?: 0.0
                    val estado = doc.getString("estado") ?: "Pendiente"
                    val observaciones = doc.getString("observaciones") ?: ""
                    OrdenEntity(id, numeroOrden, clienteId, clienteNombre, usuarioId, usuarioNombre, fecha, total, estado, observaciones)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun getProductosByOrdenId(ordenId: String): Flow<List<ProductoEntity>> = callbackFlow {
        val reg = productosRef.whereEqualTo("ordenId", ordenId).addSnapshotListener { snap, error ->
            if (error != null) { Log.w(TAG, "getProductosByOrdenId error", error); return@addSnapshotListener }
            val list = snap?.documents?.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val ordId = doc.getString("ordenId") ?: ""
                    val prodCatId = doc.getString("productoCatalogoId") ?: ""
                    val nombre = doc.getString("nombre") ?: ""
                    val cantidad = doc.getDouble("cantidad") ?: doc.getLong("cantidad")?.toDouble() ?: 0.0
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
                        val stock = doc.getDouble("stock") ?: doc.getLong("stock")?.toDouble() ?: 0.0
        val precioUnitario = doc.getDouble("precioUnitario")
            ?: doc.getLong("precioUnitario")?.toDouble()
            ?: 0.0
        val costo = doc.getDouble("costo") ?: doc.getLong("costo")?.toDouble() ?: 0.0
        val fotoUrl = doc.getString("fotoUrl") ?: ""
        CatalogoProductoEntity(doc.id, codigo, nombre, stock, precioUnitario, costo, fotoUrl)
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
        OrdenEntity(doc.id, numeroOrden, clienteId, clienteNombre, usuarioId, doc.getString("usuarioNombre") ?: "", fecha, total, estado, observaciones)
    }

    suspend fun insertUser(user: UserEntity): Result<String> = runCatching {
        val doc = usuariosRef.add(user).await()
        doc.id
    }

    suspend fun updateUser(user: UserEntity): Result<Unit> = runCatching {
        if (user.id.isBlank()) throw IllegalArgumentException("User ID vacío")
        usuariosRef.document(user.id).set(user, SetOptions.merge()).await()
    }

    suspend fun deleteUser(user: UserEntity): Result<Unit> = runCatching {
        if (user.id.isBlank()) throw IllegalArgumentException("User ID vacío")
        usuariosRef.document(user.id).delete().await()
    }

    suspend fun saveFcmToken(userId: String, token: String) {
        if (userId.isBlank()) return
        usuariosRef.document(userId).update("fcmToken", token).await()
    }

    suspend fun insertCliente(cliente: ClienteEntity): Result<String> = runCatching {
        val doc = clientesRef.add(cliente).await()
        doc.id
    }

    suspend fun updateCliente(cliente: ClienteEntity): Result<Unit> = runCatching {
        if (cliente.id.isBlank()) throw IllegalArgumentException("Cliente ID vacío")
        clientesRef.document(cliente.id).set(cliente, SetOptions.merge()).await()
    }

    suspend fun deleteCliente(cliente: ClienteEntity): Result<Unit> = runCatching {
        if (cliente.id.isBlank()) throw IllegalArgumentException("Cliente ID vacío")
        clientesRef.document(cliente.id).delete().await()
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
        if (producto.id.isBlank()) throw IllegalArgumentException("Producto ID vacío")
        catalogoRef.document(producto.id).set(producto, SetOptions.merge()).await()
    }

    suspend fun deleteProductoCatalogo(producto: CatalogoProductoEntity): Result<Unit> = runCatching {
        if (producto.id.isBlank()) throw IllegalArgumentException("Producto ID vacío")
        catalogoRef.document(producto.id).delete().await()
    }

    suspend fun adjustStock(catalogoId: String, delta: Double): Result<Unit> = runCatching {
        if (catalogoId.isBlank()) throw IllegalArgumentException("catalogoId vacío")
        catalogoRef.document(catalogoId).let { ref ->
            db.runTransaction { transaction ->
                val snap = transaction.get(ref)
                val current = snap.getDouble("stock") ?: snap.getLong("stock")?.toDouble() ?: 0.0
                transaction.update(ref, "stock", (current + delta).coerceAtLeast(0.0))
                null
            }.await()
        }
    }

    suspend fun insertOrden(orden: OrdenEntity): Result<String> = runCatching {
        val doc = ordenesRef.add(orden).await()
        doc.id
    }

    suspend fun updateOrden(orden: OrdenEntity): Result<Unit> = runCatching {
        if (orden.id.isBlank()) throw IllegalArgumentException("Orden ID vacío")
        ordenesRef.document(orden.id).set(orden, SetOptions.merge()).await()
    }

    suspend fun deleteOrden(orden: OrdenEntity): Result<Unit> = runCatching {
        if (orden.id.isBlank()) throw IllegalArgumentException("Orden ID vacío")
        ordenesRef.document(orden.id).delete().await()
    }

    suspend fun insertProducto(producto: ProductoEntity): Result<String> = runCatching {
        val doc = productosRef.add(producto).await()
        doc.id
    }

    suspend fun deleteProductosByOrdenId(ordenId: String): Result<Unit> = runCatching {
        val snap = productosRef.whereEqualTo("ordenId", ordenId).get().await()
        if (snap.documents.isEmpty()) return@runCatching
        val batch: WriteBatch = db.batch()
        snap.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    suspend fun getProductosByOrdenIdOnce(ordenId: String): Result<List<ProductoEntity>> = runCatching {
        val snap = productosRef.whereEqualTo("ordenId", ordenId).get().await()
        snap.documents.mapNotNull { doc ->
            ProductoEntity(
                id = doc.id,
                ordenId = doc.getString("ordenId") ?: "",
                productoCatalogoId = doc.getString("productoCatalogoId") ?: "",
                nombre = doc.getString("nombre") ?: "",
                cantidad = doc.getDouble("cantidad") ?: doc.getLong("cantidad")?.toDouble() ?: 0.0,
                precioUnitario = doc.getDouble("precioUnitario")
                    ?: doc.getLong("precioUnitario")?.toDouble()
                    ?: 0.0,
                total = doc.getDouble("total")
                    ?: doc.getLong("total")?.toDouble()
                    ?: 0.0
            )
        }
    }

    suspend fun getProductosByCatalogoId(catalogoId: String): Result<List<ProductoEntity>> = runCatching {
        val snap = productosRef.whereEqualTo("productoCatalogoId", catalogoId).get().await()
        snap.documents.mapNotNull { doc ->
            ProductoEntity(
                id = doc.id,
                ordenId = doc.getString("ordenId") ?: "",
                productoCatalogoId = doc.getString("productoCatalogoId") ?: "",
                nombre = doc.getString("nombre") ?: "",
                cantidad = doc.getDouble("cantidad") ?: doc.getLong("cantidad")?.toDouble() ?: 0.0,
                precioUnitario = doc.getDouble("precioUnitario")
                    ?: doc.getLong("precioUnitario")?.toDouble()
                    ?: 0.0,
                total = doc.getDouble("total")
                    ?: doc.getLong("total")?.toDouble()
                    ?: 0.0
            )
        }
    }

    suspend fun getOrdenesMapByIds(ordenIds: List<String>): Result<Map<String, String>> = runCatching {
        val result = mutableMapOf<String, String>()
        ordenIds.chunked(30).forEach { batch ->
            val snap = ordenesRef.whereIn(FieldPath.documentId(), batch).get().await()
            snap.documents.forEach { doc ->
                result[doc.id] = doc.getString("numeroOrden") ?: doc.id
            }
        }
        result
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
                        cantidad = doc.getDouble("cantidad") ?: doc.getLong("cantidad")?.toDouble() ?: 0.0,
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
            .addSnapshotListener { snap, error ->
                if (error != null) { Log.w(TAG, "historial error", error); return@addSnapshotListener }
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

    // AJUSTES STOCK
    suspend fun insertAjusteStock(ajuste: AjusteStockEntity): Result<String> = runCatching {
        val doc = ajustesStockRef.add(ajuste).await()
        doc.id
    }

    fun getAjustesStockByProductoId(productoId: String): Flow<List<AjusteStockEntity>> = callbackFlow {
        val reg = ajustesStockRef
            .whereEqualTo("productoCatalogoId", productoId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { Log.w(TAG, "ajustes error", error); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { doc ->
                    try {
                        AjusteStockEntity(
                            id = doc.id,
                            productoCatalogoId = doc.getString("productoCatalogoId") ?: "",
                            productoNombre = doc.getString("productoNombre") ?: "",
                            stockAnterior = doc.getDouble("stockAnterior") ?: doc.getLong("stockAnterior")?.toDouble() ?: 0.0,
                            stockNuevo = doc.getDouble("stockNuevo") ?: doc.getLong("stockNuevo")?.toDouble() ?: 0.0,
                            motivo = doc.getString("motivo") ?: "",
                            usuarioId = doc.getString("usuarioId") ?: "",
                            usuarioNombre = doc.getString("usuarioNombre") ?: "",
                            fecha = doc.getString("fecha") ?: ""
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    // REGISTRO ACCESO
    suspend fun insertAccessLog(log: AccessLogEntity): Result<String> = runCatching {
        val doc = accesoRef.add(log).await()
        doc.id
    }

    fun getAccessLogs(): Flow<List<AccessLogEntity>> = callbackFlow {
        val reg = accesoRef.orderBy("fecha", Query.Direction.DESCENDING).addSnapshotListener { snap, error ->
            if (error != null) { Log.w(TAG, "acceso error", error); return@addSnapshotListener }
            val list = snap?.documents?.mapNotNull { doc ->
                try {
                    AccessLogEntity(
                        id = doc.id,
                        usuarioId = doc.getString("usuarioId") ?: "",
                        usuarioNombre = doc.getString("usuarioNombre") ?: "",
                        accion = doc.getString("accion") ?: "",
                        detalle = doc.getString("detalle") ?: "",
                        fecha = doc.getString("fecha") ?: "",
                        ip = doc.getString("ip") ?: ""
                    )
                } catch (e: Exception) { null }
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    // NOTIFICACIONES
    suspend fun insertNotificacion(notif: NotificacionEntity): Result<String> = runCatching {
        val doc = notificacionesRef.add(notif).await()
        doc.id
    }

    fun getNotificacionesByUserId(usuarioId: String): Flow<List<NotificacionEntity>> = callbackFlow {
        val reg = notificacionesRef
            .whereEqualTo("usuarioId", usuarioId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { Log.w(TAG, "notificaciones error", error); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { doc ->
                    try {
                        NotificacionEntity(
                            id = doc.id,
                            mensaje = doc.getString("mensaje") ?: "",
                            tipo = doc.getString("tipo") ?: "",
                            leida = doc.getBoolean("leida") ?: false,
                            fecha = doc.getString("fecha") ?: "",
                            usuarioId = doc.getString("usuarioId") ?: "",
                            relacionId = doc.getString("relacionId") ?: ""
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun marcarNotificacionLeida(notifId: String) {
        if (notifId.isBlank()) return
        notificacionesRef.document(notifId).update("leida", true).await()
    }

    suspend fun getUnreadNotificacionesCount(usuarioId: String): Result<Int> = runCatching {
        val snap = notificacionesRef
            .whereEqualTo("usuarioId", usuarioId)
            .whereEqualTo("leida", false)
            .get().await()
        snap.size()
    }

    suspend fun marcarTodasLeidas(usuarioId: String) {
        val snap = notificacionesRef
            .whereEqualTo("usuarioId", usuarioId)
            .whereEqualTo("leida", false)
            .get().await()
        val batch = db.batch()
        snap.documents.forEach { batch.update(it.reference, "leida", true) }
        batch.commit().await()
    }

    // BATCH APPROVE
    suspend fun batchApproveOrdenes(ordenIds: List<String>): Result<Int> = runCatching {
        var count = 0
        ordenIds.chunked(30).forEach { batch ->
            val snap = ordenesRef.whereIn(FieldPath.documentId(), batch).get().await()
            val writes = db.batch()
            snap.documents.forEach { doc ->
                val estado = doc.getString("estado") ?: ""
                if (estado.equals("Pendiente", ignoreCase = true)) {
                    writes.update(doc.reference, "estado", "Aprobada")
                    count++
                }
            }
            writes.commit().await()
        }
        count
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
        val stock = doc.getDouble("stock") ?: doc.getLong("stock")?.toDouble() ?: 0.0
                        val precioUnitario = doc.getDouble("precioUnitario")
                            ?: doc.getLong("precioUnitario")?.toDouble()
                            ?: 0.0
                    val costo = doc.getDouble("costo") ?: doc.getLong("costo")?.toDouble() ?: 0.0
                    val fotoUrl = doc.getString("fotoUrl") ?: ""
                    CatalogoProductoEntity(id, codigo, nombre, stock, precioUnitario, costo, fotoUrl)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        )
    }

    // PRECIOS HISTORICOS
    suspend fun insertPrecioHistorico(hist: PrecioHistoricoEntity): Result<String> = runCatching {
        val doc = preciosHistoricosRef.add(hist).await()
        doc.id
    }

    fun getPreciosHistoricosByProductoId(productoId: String): Flow<List<PrecioHistoricoEntity>> = callbackFlow {
        val reg = preciosHistoricosRef
            .whereEqualTo("productoCatalogoId", productoId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { Log.w(TAG, "preciosHist error", error); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { doc ->
                    try {
                        PrecioHistoricoEntity(
                            id = doc.id,
                            productoCatalogoId = doc.getString("productoCatalogoId") ?: "",
                            productoNombre = doc.getString("productoNombre") ?: "",
                            precioAnterior = doc.getDouble("precioAnterior") ?: 0.0,
                            precioNuevo = doc.getDouble("precioNuevo") ?: 0.0,
                            usuarioId = doc.getString("usuarioId") ?: "",
                            usuarioNombre = doc.getString("usuarioNombre") ?: "",
                            fecha = doc.getString("fecha") ?: ""
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    // ORDER PAGING
    fun getOrdenesPaged(): Pager<com.google.firebase.firestore.DocumentSnapshot, OrdenEntity> {
        val query: Query = ordenesRef.orderBy("fecha", Query.Direction.DESCENDING)
        return Pager(
            config = PagingConfig(pageSize = 30),
            pagingSourceFactory = {
                FirestorePagingSource(query) { doc ->
                    try {
                        val id = doc.id
                        val numeroOrden = doc.getString("numeroOrden") ?: ""
                        val clienteId = doc.getString("clienteId") ?: ""
                        val clienteNombre = doc.getString("clienteNombre") ?: ""
                        val usuarioId = doc.getString("usuarioId") ?: ""
                        val fecha = doc.getString("fecha") ?: ""
                        val total = doc.getDouble("total") ?: doc.getLong("total")?.toDouble() ?: 0.0
                        val estado = doc.getString("estado") ?: "Pendiente"
                        val observaciones = doc.getString("observaciones") ?: ""
                    OrdenEntity(id, numeroOrden, clienteId, clienteNombre, usuarioId, doc.getString("usuarioNombre") ?: "", fecha, total, estado, observaciones)
                    } catch (e: Exception) { null }
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

    // MENSAJES (chat interno por pedido)
    suspend fun insertMensaje(mensaje: MensajeEntity): Result<String> = runCatching {
        val doc = mensajesRef.add(mensaje).await()
        doc.id
    }

    fun getMensajesByOrdenId(ordenId: String): Flow<List<MensajeEntity>> = callbackFlow {
        val reg = mensajesRef
            .whereEqualTo("ordenId", ordenId)
            .orderBy("fecha", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { Log.w(TAG, "mensajes error", error); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { doc ->
                    try {
                        MensajeEntity(
                            id = doc.id,
                            ordenId = doc.getString("ordenId") ?: "",
                            usuarioId = doc.getString("usuarioId") ?: "",
                            usuarioNombre = doc.getString("usuarioNombre") ?: "",
                            mensaje = doc.getString("mensaje") ?: "",
                            fecha = doc.getString("fecha") ?: ""
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun sendPushNotification(targetUserId: String, title: String, body: String, ordenId: String = "") {
        if (targetUserId.isBlank()) return
        try {
            val data = mapOf(
                "targetUserId" to targetUserId,
                "title" to title,
                "body" to body,
                "ordenId" to ordenId,
                "sent" to false,
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            pushQueueRef.add(data).await()
        } catch (e: Exception) {
            Log.w(TAG, "sendPushNotification error", e)
        }
    }

    suspend fun getFcmTokenByUserId(userId: String): String? {
        return try {
            val doc = usuariosRef.document(userId).get().await()
            doc.getString("fcmToken")
        } catch (e: Exception) { null }
    }
}
