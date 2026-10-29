package com.mutepoint.app.domain.repository

import com.mutepoint.app.domain.model.BatonState
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    val batonState: Flow<BatonState>

    suspend fun requestBaton()

    suspend fun grantBaton(timeSeconds: Int)

    suspend fun tick()

    suspend fun passBaton()

    suspend fun leaveQueue()
}
