package com.gemnav.search

import android.content.Context
import com.gemnav.data.db.DatabaseProvider
import com.gemnav.data.db.entities.SearchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class SearchHistoryRepository(context: Context) {

    private val db = DatabaseProvider.getDatabase(context)

    suspend fun saveQuery(text: String) {
        withContext(Dispatchers.IO) {
            db.searchHistoryDao().insertQuery(
                SearchHistoryEntity(
                    query = text,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun recentQueries(limit: Int = 20): Flow<List<String>> = flow {
        val results = db.searchHistoryDao().getRecentSearches(limit)
        emit(results.map { it.query })
    }
}
