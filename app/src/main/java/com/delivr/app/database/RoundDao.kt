package com.delivr.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.delivr.app.domain.CottageStatus

/**
 * Accès à la tournée en cours. Déclaré en `abstract class` plutôt qu'en
 * `interface` : [replaceRound]/[updateRound]/[clearRound] sont des méthodes
 * concrètes annotées [Transaction] qui composent les requêtes primitives
 * ci-dessous — Room génère un override qui les enveloppe dans une vraie
 * transaction SQLite. Bénéfice secondaire : le faux DAO des tests JVM
 * (`FakeRoundDao`) hérite cette logique transactionnelle sans la dupliquer,
 * donc les tests JVM la couvrent aussi (voir `RoundRepositoryTest`).
 */
@Dao
abstract class RoundDao {
    @Query("SELECT COUNT(*) FROM cottage")
    abstract suspend fun countCottages(): Int

    @Query("SELECT * FROM round WHERE id = ${RoundEntity.SINGLE_ROUND_ID}")
    abstract suspend fun selectRound(): RoundEntity?

    @Query("SELECT * FROM cottage ORDER BY position ASC")
    abstract suspend fun selectCottages(): List<CottageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRound(round: RoundEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCottages(cottages: List<CottageEntity>)

    @Query("DELETE FROM cottage")
    abstract suspend fun deleteAllCottages()

    @Query("DELETE FROM round")
    abstract suspend fun deleteRound()

    /** Utilisé à partir de la Phase 6 (mode Livraison) — pas encore appelé en Phase 5. */
    @Query("UPDATE cottage SET status = :status WHERE number = :number")
    abstract suspend fun updateStatus(
        number: Int,
        status: CottageStatus,
    ): Int

    /** Nouvelle tournée : tout ce qui précède est jeté, statuts remis à zéro. */
    @Transaction
    open suspend fun replaceRound(
        round: RoundEntity,
        cottages: List<CottageEntity>,
    ) {
        deleteAllCottages()
        insertRound(round)
        insertCottages(cottages)
    }

    /**
     * Édition de la tournée courante (ajout/suppression/modification/sens) :
     * réécrit intégralement la liste — au plus quelques dizaines de lignes,
     * donc inutile de calculer un delta — mais **conserve les statuts déjà
     * acquis**. Sans cette précaution, changer le sens de tournée en Phase 6
     * repasserait tous les cottages livrés à « à faire ». En Phase 5 tous les
     * statuts valent [CottageStatus.A_FAIRE], donc l'effet est invisible :
     * c'est volontairement écrit maintenant pour ne pas avoir à rétrofiter la
     * Phase 6 sur du code déjà livré (même raisonnement que le placement de
     * la Phase 5 avant la Phase 6, voir `TODO_V1.md`).
     */
    @Transaction
    open suspend fun updateRound(
        round: RoundEntity,
        cottages: List<CottageEntity>,
    ) {
        val previousStatuses = selectCottages().associate { it.number to it.status }
        deleteAllCottages()
        insertRound(round)
        insertCottages(cottages.map { it.copy(status = previousStatuses[it.number] ?: CottageStatus.A_FAIRE) })
    }

    @Transaction
    open suspend fun clearRound() {
        deleteAllCottages()
        deleteRound()
    }
}
