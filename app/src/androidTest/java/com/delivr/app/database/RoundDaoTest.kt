package com.delivr.app.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.delivr.app.domain.CottageStatus
import com.delivr.app.domain.SortDirection
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests du vrai SQL de [RoundDao], sur une base **en mémoire** — nécessite
 * un appareil (comme `CottageExtractionIntegrationTest`, Phase 3.6) : pas
 * joué par `ci.yml` faute d'émulateur. La logique de plus haut niveau
 * (`RoundRepository`, préservation des statuts, etc.) est déjà couverte en
 * JVM par `FakeRoundDao`/`RoundRepositoryTest` ; ce test-ci vérifie
 * spécifiquement ce que seul le vrai moteur SQLite peut prouver : le
 * stockage des enums, le comportement `OnConflictStrategy.REPLACE`, et les
 * `@Transaction` exécutées pour de vrai.
 */
@RunWith(AndroidJUnit4::class)
class RoundDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: RoundDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // inMemoryDatabaseBuilder : base détruite à la fermeture, aucun
        // fichier laissé sur l'appareil, tests indépendants les uns des
        // autres. Pas de allowMainThreadQueries() : on n'appelle que des
        // fonctions suspend, que Room dispatche déjà sur son propre exécuteur.
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.roundDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aller_retour_replaceRound_puis_selectRound_et_selectCottages_dans_l_ordre_de_position() =
        runBlocking {
            dao.replaceRound(
                round = RoundEntity(sortDirection = SortDirection.ASCENDING, createdAt = 1_000L),
                cottages =
                    listOf(
                        CottageEntity(number = 143, position = 1),
                        CottageEntity(number = 3, position = 0),
                    ),
            )

            val round = dao.selectRound()
            val cottages = dao.selectCottages()

            assertEquals(SortDirection.ASCENDING, round?.sortDirection)
            assertEquals(1_000L, round?.createdAt)
            assertEquals(listOf(3, 143), cottages.map { it.number })
        }

    @Test
    fun les_enums_sont_bien_stockes_sous_forme_de_texte() =
        runBlocking {
            dao.replaceRound(
                round = RoundEntity(sortDirection = SortDirection.ASCENDING, createdAt = 0L),
                cottages = listOf(CottageEntity(number = 12, position = 0, status = CottageStatus.A_FAIRE)),
            )

            // Requête brute : seul le vrai moteur SQLite peut prouver que le
            // convertisseur d'enum intégré de Room stocke bien le *nom* de la
            // constante en TEXT, sans @TypeConverter écrit à la main.
            database.openHelper.readableDatabase.query("SELECT status FROM cottage WHERE number = 12").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("A_FAIRE", cursor.getString(0))
            }
            database.openHelper.readableDatabase.query("SELECT sort_direction FROM round").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("ASCENDING", cursor.getString(0))
            }
        }

    @Test
    fun insertCottages_avec_un_numero_deja_present_remplace_la_ligne_au_lieu_d_echouer() =
        runBlocking {
            dao.insertCottages(listOf(CottageEntity(number = 3, position = 0, status = CottageStatus.A_FAIRE)))

            dao.insertCottages(listOf(CottageEntity(number = 3, position = 5, status = CottageStatus.LIVRE)))

            val cottages = dao.selectCottages()
            assertEquals(1, cottages.size)
            assertEquals(CottageStatus.LIVRE, cottages.first().status)
        }

    @Test
    fun insertRound_deux_fois_ne_laisse_jamais_qu_une_seule_ligne() =
        runBlocking {
            dao.insertRound(RoundEntity(sortDirection = SortDirection.ASCENDING, createdAt = 1L))
            dao.insertRound(RoundEntity(sortDirection = SortDirection.DESCENDING, createdAt = 2L))

            database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM round").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            assertEquals(SortDirection.DESCENDING, dao.selectRound()?.sortDirection)
        }

    @Test
    fun replaceRound_ne_laisse_aucune_ligne_de_la_tournee_precedente() =
        runBlocking {
            dao.replaceRound(
                round = RoundEntity(sortDirection = SortDirection.ASCENDING, createdAt = 1L),
                cottages = listOf(CottageEntity(number = 3, position = 0), CottageEntity(number = 35, position = 1)),
            )

            dao.replaceRound(
                round = RoundEntity(sortDirection = SortDirection.DESCENDING, createdAt = 2L),
                cottages = listOf(CottageEntity(number = 6, position = 0)),
            )

            assertEquals(listOf(6), dao.selectCottages().map { it.number })
        }

    @Test
    fun updateRound_conserve_un_statut_deja_pose_par_updateStatus() =
        runBlocking {
            dao.replaceRound(
                round = RoundEntity(sortDirection = SortDirection.ASCENDING, createdAt = 1L),
                cottages = listOf(CottageEntity(number = 3, position = 0), CottageEntity(number = 35, position = 1)),
            )
            dao.updateStatus(35, CottageStatus.LIVRE)

            dao.updateRound(
                round = RoundEntity(sortDirection = SortDirection.ASCENDING, createdAt = 1L),
                cottages = listOf(CottageEntity(number = 3, position = 0), CottageEntity(number = 35, position = 1)),
            )

            assertEquals(CottageStatus.LIVRE, dao.selectCottages().first { it.number == 35 }.status)
        }

    @Test
    fun updateStatus_sur_un_numero_inexistant_ne_touche_aucune_ligne() =
        runBlocking {
            val affected = dao.updateStatus(999, CottageStatus.LIVRE)

            assertEquals(0, affected)
        }

    @Test
    fun clearRound_vide_les_deux_tables() =
        runBlocking {
            dao.replaceRound(
                round = RoundEntity(sortDirection = SortDirection.ASCENDING, createdAt = 1L),
                cottages = listOf(CottageEntity(number = 3, position = 0)),
            )

            dao.clearRound()

            assertNull(dao.selectRound())
            assertTrue(dao.selectCottages().isEmpty())
        }
}
