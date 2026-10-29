package com.mutepoint.app.data.repository

import com.mutepoint.app.domain.model.BatonState
import com.mutepoint.app.domain.repository.RoomRepository
import com.mutepoint.app.domain.usecase.BatonManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class RoomRepositoryImpl @Inject constructor() : RoomRepository {

    private val manager = BatonManager()
    private val _batonState = MutableStateFlow(manager.state)

    override val batonState: Flow<BatonState> = _batonState.asStateFlow()

    override suspend fun requestBaton() {
        _batonState.value = manager.requestBaton()
    }

    override suspend fun grantBaton(timeSeconds: Int) {
        _batonState.value = manager.grantBaton(timeSeconds)
    }

    override suspend fun tick() {
        _batonState.value = manager.tick()
    }

    override suspend fun passBaton() {
        _batonState.value = manager.passBaton()
    }

    override suspend fun leaveQueue() {
        _batonState.value = manager.leaveQueue()
    }
}
