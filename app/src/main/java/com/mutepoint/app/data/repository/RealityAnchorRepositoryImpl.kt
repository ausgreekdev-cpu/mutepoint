package com.mutepoint.app.data.repository

import com.mutepoint.app.data.local.dao.RealityAnchorDao
import com.mutepoint.app.data.mapper.toDomain
import com.mutepoint.app.data.mapper.toEntity
import com.mutepoint.app.domain.model.RealityAnchorEntry
import com.mutepoint.app.domain.repository.RealityAnchorRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RealityAnchorRepositoryImpl @Inject constructor(
    private val realityAnchorDao: RealityAnchorDao,
) : RealityAnchorRepository {

    override fun observeEntries(): Flow<List<RealityAnchorEntry>> =
        realityAnchorDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertEntry(entry: RealityAnchorEntry): Long =
        realityAnchorDao.insert(entry.toEntity())

    override suspend fun deleteEntry(id: Long) = realityAnchorDao.deleteById(id)
}
