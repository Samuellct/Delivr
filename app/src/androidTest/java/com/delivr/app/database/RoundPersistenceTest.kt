package com.delivr.app.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.delivr.app.domain.CottageStatus
import com.delivr.app.domain.SortDirection
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB_NAME = "delivr_persistence_test.db"

/**
 * Automatise, au niveau DAO, le critère d'acceptation de la Phase 5
 * (« fermer l'app en plein milieu d'une tournée [...] puis la rouvrir [...]
 * restaure exactement l'état où elle a été quittée ») : contrairement à
 * [RoundDaoTest], qui utilise une base **en mémoire** (perdue à la
 * fermeture, donc inadaptée ici), cette base vit sur **fichier** — fermer
 * puis rouvrir une nouvelle instance sur le même fichier est l'équivalent
 * fidèle d'une mort de process réelle.
 */
@RunWith(AndroidJUnit4::class)
class RoundPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB_NAME)
    }

    private fun openDatabase(): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB_NAME).build()

    @Test
    fun une_tournee_ecrite_puis_l_app_fermee_est_relue_a_l_identique_a_la_reouverture() =
        runBlocking {
            val firstOpen = openDatabase()
            firstOpen.roundDao().replaceRound(
                round = RoundEntity(sortDirection = SortDirection.DESCENDING, createdAt = 42_000L),
                cottages =
                    listOf(
                        CottageEntity(number = 3, position = 0, status = CottageStatus.LIVRE),
                        CottageEntity(number = 143, position = 1),
                        CottageEntity(number = 999, position = 2),
                    ),
            )
            // close() : équivaut à la mort du process — la base n'existe plus
            // qu'en tant que fichier sur le disque, plus aucune instance en
            // mémoire ne détient l'état de la tournée.
            firstOpen.close()

            // Nouvelle instance sur le même fichier : ce que fait
            // AppDatabase.create() au prochain lancement de l'app.
            val secondOpen = openDatabase()
            val round = secondOpen.roundDao().selectRound()
            val cottages = secondOpen.roundDao().selectCottages()
            secondOpen.close()

            assertEquals(SortDirection.DESCENDING, round?.sortDirection)
            assertEquals(42_000L, round?.createdAt)
            assertEquals(listOf(3, 143, 999), cottages.map { it.number })
            assertEquals(CottageStatus.LIVRE, cottages.first { it.number == 3 }.status)
        }
}
