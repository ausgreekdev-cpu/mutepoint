package com.mutepoint.app.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.mutepoint.app.data.local.entities.AssertionCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardsDao {

    @Query("SELECT * FROM assertion_cards ORDER BY category, id")
    fun observeAll(): Flow<List<AssertionCardEntity>>

    @Query("SELECT COUNT(*) FROM assertion_cards")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(cards: List<AssertionCardEntity>)
}
