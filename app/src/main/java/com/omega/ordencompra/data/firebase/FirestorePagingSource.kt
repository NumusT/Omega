package com.omega.ordencompra.data.firebase

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestorePagingSource<T : Any>(
    private val query: Query,
    private val mapper: (DocumentSnapshot) -> T?
) : PagingSource<DocumentSnapshot, T>() {

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, T>): DocumentSnapshot? {
        return null
    }

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, T> {
        return try {
            var currentQuery = query.limit(params.loadSize.toLong())
            if (params.key != null) {
                currentQuery = currentQuery.startAfter(params.key!!)
            }

            val querySnapshot = currentQuery.get().await()
            val documents = querySnapshot.documents
            val data = documents.mapNotNull { mapper(it) }

            val nextKey = if (documents.size < params.loadSize) {
                null
            } else {
                documents.last()
            }

            LoadResult.Page(
                data = data,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
