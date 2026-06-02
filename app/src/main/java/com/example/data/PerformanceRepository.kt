package com.example.data

import kotlinx.coroutines.flow.Flow

class PerformanceRepository(private val dao: PerformanceLogDao) {
    val allLogs: Flow<List<PerformanceLog>> = dao.getAllLogs()

    suspend fun insertLog(log: PerformanceLog): Long {
        return dao.insertLog(log)
    }

    suspend fun deleteLogById(id: Long) {
        dao.deleteLogById(id)
    }

    suspend fun clearAllLogs() {
        dao.clearAllLogs()
    }

    suspend fun getLatestLog(): PerformanceLog? {
        return dao.getLatestLog()
    }
}
