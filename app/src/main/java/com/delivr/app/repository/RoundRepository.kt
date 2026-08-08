package com.delivr.app.repository

import com.delivr.app.database.CottageEntity
import com.delivr.app.database.RoundDao
import com.delivr.app.database.RoundEntity
import com.delivr.app.domain.SortDirection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Tournée relue depuis Room, exprimée dans le vocabulaire du domaine. */
data class SavedRound(
    val cottageNumbers: List<Int>,
    val sortDirection: SortDirection,
    val createdAt: Long,
)

/**
 * Traduit entre le vocabulaire du domaine (`List<Int>` + [SortDirection],
 * manipulé par les fonctions pures de `domain/CottageList.kt`) et les
 * entités Room ([RoundEntity]/[CottageEntity]) — c'est la seule couche qui
 * connaît ces entités.
 */
class RoundRepository(
    private val roundDao: RoundDao,
) {
    /**
     * Toutes les écritures passent par ce mutex. Ce n'est pas de la
     * paranoïa : chaque geste de l'utilisateur (ajout, suppression, sens de
     * tournée) déclenche un `viewModelScope.launch` distinct, et rien ne
     * garantit l'ordre d'exécution de deux coroutines lancées coup sur coup.
     * Comme chaque écriture réécrit la liste *entière* (un instantané, pas
     * un delta), une inversion laisserait la base sur un état périmé. Le
     * `Mutex` de kotlinx.coroutines est équitable (FIFO) : les écritures
     * sont donc appliquées dans l'ordre où l'utilisateur les a faites.
     */
    private val writeMutex = Mutex()

    suspend fun hasRoundInProgress(): Boolean = roundDao.countCottages() > 0

    suspend fun loadRound(): SavedRound? {
        val round = roundDao.selectRound() ?: return null
        val cottages = roundDao.selectCottages()
        if (cottages.isEmpty()) return null
        return SavedRound(
            cottageNumbers = cottages.map { it.number },
            sortDirection = round.sortDirection,
            createdAt = round.createdAt,
        )
    }

    /** Nouvelle tournée : écrase intégralement la précédente (pas d'historique en V1). */
    suspend fun startRound(
        cottageNumbers: List<Int>,
        sortDirection: SortDirection,
        createdAtMillis: Long = System.currentTimeMillis(),
    ) = writeMutex.withLock {
        roundDao.replaceRound(
            round = RoundEntity(sortDirection = sortDirection, createdAt = createdAtMillis),
            cottages = cottageNumbers.toEntities(),
        )
    }

    /** Édition de la tournée courante : conserve l'horodatage et les statuts déjà acquis. */
    suspend fun updateRound(
        cottageNumbers: List<Int>,
        sortDirection: SortDirection,
    ) = writeMutex.withLock {
        val createdAt = roundDao.selectRound()?.createdAt ?: System.currentTimeMillis()
        roundDao.updateRound(
            round = RoundEntity(sortDirection = sortDirection, createdAt = createdAt),
            cottages = cottageNumbers.toEntities(),
        )
    }

    suspend fun clearRound() = writeMutex.withLock { roundDao.clearRound() }
}

/** La position est l'index dans la liste, déjà triée par `domain/CottageList.kt`. */
private fun List<Int>.toEntities(): List<CottageEntity> = mapIndexed { index, number -> CottageEntity(number = number, position = index) }
