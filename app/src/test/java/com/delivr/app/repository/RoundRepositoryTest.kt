package com.delivr.app.repository

import com.delivr.app.database.FakeRoundDao
import com.delivr.app.domain.CottageStatus
import com.delivr.app.domain.SortDirection
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [RoundRepository] testé avec [FakeRoundDao] : mapping domaine ↔ entités,
 * écrasement d'une tournée par la suivante, préservation des statuts, et
 * ordonnancement des écritures concurrentes (le `Mutex` interne). Tourne en
 * JVM pur, donc jouée par `ci.yml` à chaque push.
 */
class RoundRepositoryTest {
    @Test
    fun `startRound ecrit les numeros dans l'ordre recu avec des positions croissantes`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())

            repository.startRound(listOf(35, 3, 143), SortDirection.ASCENDING)

            val saved = repository.loadRound()
            assertEquals(listOf(35, 3, 143), saved?.cottageNumbers)
            assertEquals(SortDirection.ASCENDING, saved?.sortDirection)
        }

    @Test
    fun `loadRound relit exactement ce qui a ete ecrit, y compris en ordre decroissant`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())

            repository.startRound(listOf(999, 78, 3), SortDirection.DESCENDING)

            val saved = repository.loadRound()
            assertEquals(listOf(999, 78, 3), saved?.cottageNumbers)
            assertEquals(SortDirection.DESCENDING, saved?.sortDirection)
        }

    @Test
    fun `loadRound rend null sur une base vierge`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())

            assertNull(repository.loadRound())
        }

    @Test
    fun `loadRound rend null si la ligne round existe mais qu'aucun cottage ne subsiste`() =
        runTest {
            val dao = FakeRoundDao()
            val repository = RoundRepository(dao)
            repository.startRound(listOf(3), SortDirection.ASCENDING)

            dao.deleteAllCottages()

            assertNull(repository.loadRound())
        }

    @Test
    fun `hasRoundInProgress reflete l'etat de la base`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            assertFalse(repository.hasRoundInProgress())

            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)
            assertTrue(repository.hasRoundInProgress())

            repository.clearRound()
            assertFalse(repository.hasRoundInProgress())
        }

    @Test
    fun `startRound deux fois ne conserve aucune ligne de la premiere tournee`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 143, 999), SortDirection.ASCENDING)

            repository.startRound(listOf(6), SortDirection.DESCENDING)

            val saved = repository.loadRound()
            assertEquals(listOf(6), saved?.cottageNumbers)
            assertEquals(SortDirection.DESCENDING, saved?.sortDirection)
        }

    @Test
    fun `updateRound conserve un statut deja acquis et met le nouveau numero a A_FAIRE`() =
        runTest {
            val dao = FakeRoundDao()
            val repository = RoundRepository(dao)
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)
            dao.setStatusForTest(35, CottageStatus.LIVRE)

            repository.updateRound(listOf(3, 35, 78), SortDirection.ASCENDING)

            val cottages = dao.selectCottages()
            assertEquals(CottageStatus.LIVRE, cottages.first { it.number == 35 }.status)
            assertEquals(CottageStatus.A_FAIRE, cottages.first { it.number == 78 }.status)
        }

    @Test
    fun `updateRound conserve l'horodatage de la tournee d'origine`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3), SortDirection.ASCENDING, createdAtMillis = 1_000L)

            repository.updateRound(listOf(3, 35), SortDirection.ASCENDING)

            assertEquals(1_000L, repository.loadRound()?.createdAt)
        }

    @Test
    fun `updateRound avec un numero supprime le fait vraiment disparaitre`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 78), SortDirection.ASCENDING)

            repository.updateRound(listOf(3, 78), SortDirection.ASCENDING)

            assertEquals(listOf(3, 78), repository.loadRound()?.cottageNumbers)
        }

    @Test
    fun `deux ecritures concurrentes appliquent la derniere soumise en dernier`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3), SortDirection.ASCENDING)

            // Deux coroutines lancées coup sur coup : le mutex garantit que
            // la seconde soumise (35, 78) est bien celle qui l'emporte,
            // qu'importe l'ordre réel d'exécution des coroutines. On attend
            // les deux (join) avant d'observer le résultat : seul l'ordre
            // relatif des écritures est garanti par le mutex, pas que les
            // deux aient déjà tourné au moment de l'assertion.
            val first = launch { repository.updateRound(listOf(999), SortDirection.ASCENDING) }
            val second = launch { repository.updateRound(listOf(35, 78), SortDirection.ASCENDING) }
            first.join()
            second.join()

            assertEquals(listOf(35, 78), repository.loadRound()?.cottageNumbers)
        }

    @Test
    fun `un nouveau repository sur le meme dao retrouve la tournee ecrite precedemment`() =
        runTest {
            val dao = FakeRoundDao()
            RoundRepository(dao).startRound(listOf(3, 35), SortDirection.DESCENDING)

            // Équivalent JVM d'un redémarrage : un second repository, sur le
            // même DAO, doit relire exactement la même tournée.
            val recreated = RoundRepository(dao)

            val saved = recreated.loadRound()
            assertEquals(listOf(3, 35), saved?.cottageNumbers)
            assertEquals(SortDirection.DESCENDING, saved?.sortDirection)
        }

    @Test
    fun `loadRound reflete le statut par defaut A_FAIRE de chaque cottage`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)

            val saved = repository.loadRound()

            assertEquals(listOf(CottageStatus.A_FAIRE, CottageStatus.A_FAIRE), saved?.cottages?.map { it.status })
        }

    @Test
    fun `updateCottageStatus change uniquement le cottage vise`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 78), SortDirection.ASCENDING)

            val updated = repository.updateCottageStatus(35, CottageStatus.LIVRE)

            assertTrue(updated)
            val statuses = repository.loadRound()?.cottages?.associate { it.number to it.status }
            assertEquals(CottageStatus.A_FAIRE, statuses?.get(3))
            assertEquals(CottageStatus.LIVRE, statuses?.get(35))
            assertEquals(CottageStatus.A_FAIRE, statuses?.get(78))
        }

    @Test
    fun `updateCottageStatus sur un numero absent rend false et ne change rien`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)

            val updated = repository.updateCottageStatus(999, CottageStatus.LIVRE)

            assertFalse(updated)
            val statuses = repository.loadRound()?.cottages?.map { it.status }
            assertEquals(listOf(CottageStatus.A_FAIRE, CottageStatus.A_FAIRE), statuses)
        }

    @Test
    fun `updateCottageStatus ne touche ni l'ordre ni l'horodatage`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 78), SortDirection.ASCENDING, createdAtMillis = 1_000L)

            repository.updateCottageStatus(35, CottageStatus.ANNULE)

            val saved = repository.loadRound()
            assertEquals(listOf(3, 35, 78), saved?.cottageNumbers)
            assertEquals(1_000L, saved?.createdAt)
        }

    @Test
    fun `un statut acquis via updateCottageStatus survit a un updateRound qui suit`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)
            repository.updateCottageStatus(35, CottageStatus.LIVRE)

            repository.updateRound(listOf(3, 35, 78), SortDirection.ASCENDING)

            val statuses = repository.loadRound()?.cottages?.associate { it.number to it.status }
            assertEquals(CottageStatus.LIVRE, statuses?.get(35))
            assertEquals(CottageStatus.A_FAIRE, statuses?.get(78))
        }

    @Test
    fun `deux updateCottageStatus concurrents n'en perdent aucun`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)

            val first = launch { repository.updateCottageStatus(3, CottageStatus.LIVRE) }
            val second = launch { repository.updateCottageStatus(35, CottageStatus.ANNULE) }
            first.join()
            second.join()

            val statuses = repository.loadRound()?.cottages?.associate { it.number to it.status }
            assertEquals(CottageStatus.LIVRE, statuses?.get(3))
            assertEquals(CottageStatus.ANNULE, statuses?.get(35))
        }
}
