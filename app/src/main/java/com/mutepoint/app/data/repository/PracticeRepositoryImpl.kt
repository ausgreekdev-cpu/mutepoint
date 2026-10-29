package com.mutepoint.app.data.repository

import com.mutepoint.app.data.local.dao.CardsDao
import com.mutepoint.app.data.local.dao.JournalDao
import com.mutepoint.app.data.local.entities.AssertionCardEntity
import com.mutepoint.app.data.mapper.toDomain
import com.mutepoint.app.data.mapper.toEntity
import com.mutepoint.app.domain.model.JournalEntry
import com.mutepoint.app.domain.model.PracticeCard
import com.mutepoint.app.domain.model.PracticeCardCategory
import com.mutepoint.app.domain.repository.PracticeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PracticeRepositoryImpl @Inject constructor(
    private val journalDao: JournalDao,
    private val cardsDao: CardsDao,
) : PracticeRepository {

    override fun observeCards(): Flow<List<PracticeCard>> =
        cardsDao.observeAll().map { entities ->
            entities.map { entity ->
                PracticeCard(
                    id = entity.id,
                    category = runCatching { PracticeCardCategory.valueOf(entity.category) }
                        .getOrDefault(PracticeCardCategory.WORKPLACE),
                    title = entity.title,
                    script = entity.script,
                )
            }
        }

    override fun observeJournal(): Flow<List<JournalEntry>> =
        journalDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertJournalEntry(entry: JournalEntry): Long =
        journalDao.insert(entry.toEntity())

    override suspend fun deleteJournalEntry(id: Long) = journalDao.deleteById(id)

    suspend fun ensureSeedCards() {
        if (cardsDao.count() > 0) return
        cardsDao.upsertAll(seedCards())
    }

    private fun seedCards(): List<AssertionCardEntity> = listOf(
        AssertionCardEntity(
            category = PracticeCardCategory.WORKPLACE.name,
            title = "The Gentle Blocker",
            script = "I'm not finished yet — let me complete my point, then I'd love your input.",
        ),
        AssertionCardEntity(
            category = PracticeCardCategory.WORKPLACE.name,
            title = "The Redirect",
            script = "I'll get to your question in a moment; first let me wrap up what I was saying.",
        ),
        AssertionCardEntity(
            category = PracticeCardCategory.WORKPLACE.name,
            title = "The Recap Hold",
            script = "To make sure my point lands: the part I still need to finish is…",
        ),
        AssertionCardEntity(
            category = PracticeCardCategory.SOCIAL.name,
            title = "The Kind Boundary",
            script = "Hold on — I do want to hear you, but I still have the floor for a second.",
        ),
        AssertionCardEntity(
            category = PracticeCardCategory.SOCIAL.name,
            title = "The Equalizer",
            script = "Let's make sure we both get to finish. I'll be quick, then it's your turn.",
        ),
        AssertionCardEntity(
            category = PracticeCardCategory.HIGH_CONFLICT.name,
            title = "The Anchor",
            script = "I hear you. And I still need to finish what I started saying.",
        ),
        AssertionCardEntity(
            category = PracticeCardCategory.HIGH_CONFLICT.name,
            title = "The Pause",
            script = "I'm not going to talk over you — and I need the same courtesy. Give me ten seconds.",
        ),
    )
}
