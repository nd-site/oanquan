package com.example.data

import kotlinx.coroutines.flow.Flow

class MatchHistoryRepository(private val dao: MatchHistoryDao) {
    val allMatches: Flow<List<MatchHistory>> = dao.getAllMatches()

    suspend fun insertMatch(match: MatchHistory) {
        dao.insertMatch(match)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }
}
