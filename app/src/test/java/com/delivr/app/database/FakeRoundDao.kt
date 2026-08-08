package com.delivr.app.database

import com.delivr.app.domain.CottageStatus

/**
 * Faux DAO en mémoire (deux listes), qui permet de tester [RoundRepository]
 * et les ViewModels en JUnit pur — donc dans `ci.yml`, à chaque push, sans
 * émulateur (contrairement aux vrais tests SQL, voir `RoundDaoTest` dans
 * `androidTest`). Pas de Robolectric, pas de mock : [RoundDao] est une
 * classe abstraite dont on n'implémente que les requêtes primitives. Les
 * méthodes `@Transaction` (`replaceRound`/`updateRound`/`clearRound`) sont
 * **héritées**, donc ces tests couvrent aussi leur logique — notamment la
 * préservation des statuts dans [RoundDao.updateRound].
 */
class FakeRoundDao : RoundDao() {
    private var round: RoundEntity? = null
    private val cottages = mutableListOf<CottageEntity>()

    /** Utilitaire de test : simule un statut déjà acquis (Phase 6). */
    fun setStatusForTest(
        number: Int,
        status: CottageStatus,
    ) {
        val index = cottages.indexOfFirst { it.number == number }
        if (index >= 0) cottages[index] = cottages[index].copy(status = status)
    }

    override suspend fun countCottages(): Int = cottages.size

    override suspend fun selectRound(): RoundEntity? = round

    override suspend fun selectCottages(): List<CottageEntity> = cottages.sortedBy { it.position }.toList()

    override suspend fun insertRound(round: RoundEntity) {
        this.round = round
    }

    override suspend fun insertCottages(cottages: List<CottageEntity>) {
        // OnConflictStrategy.REPLACE sur la PK `number` : une ligne existante
        // avec le même numéro est remplacée, jamais dupliquée.
        cottages.forEach { incoming ->
            val index = this.cottages.indexOfFirst { it.number == incoming.number }
            if (index >= 0) this.cottages[index] = incoming else this.cottages.add(incoming)
        }
    }

    override suspend fun deleteAllCottages() {
        cottages.clear()
    }

    override suspend fun deleteRound() {
        round = null
    }

    override suspend fun updateStatus(
        number: Int,
        status: CottageStatus,
    ): Int {
        val index = cottages.indexOfFirst { it.number == number }
        if (index < 0) return 0
        cottages[index] = cottages[index].copy(status = status)
        return 1
    }
}
