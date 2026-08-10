package com.delivr.app.ui.delivery

import com.delivr.app.MainDispatcherRule
import com.delivr.app.database.FakeRoundDao
import com.delivr.app.domain.CottageStatus
import com.delivr.app.domain.SortDirection
import com.delivr.app.repository.RoundRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * [DeliveryViewModel] testé avec [FakeRoundDao], sur le modèle de
 * `ValidationViewModelTest`/`HomeViewModelTest` : construction directe
 * (pas de `SavedStateHandle`, voir le KDoc de `DeliveryViewModel`), état lu
 * via `vm.uiState`. Jouée en JVM pur, donc par `ci.yml` à chaque push —
 * c'est ici, et pas dans `DeliveryScreenTest` (jamais joué en CI), que le
 * critère d'acceptation de la Phase 6 est vraiment protégé d'une
 * régression.
 */
class DeliveryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `l'etat initial avant tout load est Loading`() {
        val vm = DeliveryViewModel(RoundRepository(FakeRoundDao()))

        assertEquals(DeliveryUiState.Loading, vm.uiState)
    }

    @Test
    fun `load sur une tournee de 3 cottages rend le premier comme courant`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)

            vm.load()

            val state = vm.uiState
            assertTrue(state is DeliveryUiState.InProgress)
            state as DeliveryUiState.InProgress
            assertEquals(3, state.currentCottage?.number)
            assertEquals(1, state.displayPosition)
            assertEquals(3, state.total)
            assertFalse(state.isFinished)
            assertFalse(state.canGoBack)
        }

    @Test
    fun `load sur une base vide rend Error RoundUnavailable`() =
        runTest {
            val vm = DeliveryViewModel(RoundRepository(FakeRoundDao()))

            vm.load()

            val state = vm.uiState
            assertTrue(state is DeliveryUiState.Error)
            assertEquals(DeliveryError.RoundUnavailable, (state as DeliveryUiState.Error).error)
        }

    @Test
    fun `onDelivered avance immediatement et persiste en base`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)
            vm.load()

            vm.onDelivered()

            val state = vm.uiState as DeliveryUiState.InProgress
            assertEquals(35, state.currentCottage?.number)
            val statuses = repository.loadRound()?.cottages?.associate { it.number to it.status }
            assertEquals(CottageStatus.LIVRE, statuses?.get(3))
        }

    @Test
    fun `onCancelled avance immediatement et persiste en base`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)
            vm.load()

            vm.onCancelled()

            val state = vm.uiState as DeliveryUiState.InProgress
            assertEquals(35, state.currentCottage?.number)
            val statuses = repository.loadRound()?.cottages?.associate { it.number to it.status }
            assertEquals(CottageStatus.ANNULE, statuses?.get(3))
        }

    @Test
    fun `onPreviousCottage restaure le statut precedent en base`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)
            vm.load()
            vm.onDelivered()
            vm.onCancelled()

            vm.onPreviousCottage()

            val state = vm.uiState as DeliveryUiState.InProgress
            assertEquals(35, state.currentCottage?.number)
            val statuses = repository.loadRound()?.cottages?.associate { it.number to it.status }
            assertEquals(CottageStatus.LIVRE, statuses?.get(3))
            assertEquals(CottageStatus.A_FAIRE, statuses?.get(35))
        }

    @Test
    fun `onPreviousCottage sur le premier cottage ne change rien`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)
            vm.load()

            vm.onPreviousCottage()

            val state = vm.uiState as DeliveryUiState.InProgress
            assertEquals(3, state.currentCottage?.number)
            assertFalse(state.canGoBack)
        }

    @Test
    fun `tournee terminee neutralise les mutations et onPreviousCottage en repart`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)
            vm.load()
            vm.onDelivered()
            vm.onDelivered()

            var state = vm.uiState as DeliveryUiState.InProgress
            assertTrue(state.isFinished)
            assertEquals(2, state.displayPosition)

            // Une mutation supplémentaire sur une tournée terminée ne change rien.
            vm.onDelivered()
            state = vm.uiState as DeliveryUiState.InProgress
            assertTrue(state.isFinished)

            vm.onPreviousCottage()
            state = vm.uiState as DeliveryUiState.InProgress
            assertFalse(state.isFinished)
            assertEquals(35, state.currentCottage?.number)
        }

    @Test
    fun `un appel avant load ne plante pas`() {
        val vm = DeliveryViewModel(RoundRepository(FakeRoundDao()))

        vm.onDelivered()
        vm.onCancelled()
        vm.onPreviousCottage()

        assertEquals(DeliveryUiState.Loading, vm.uiState)
    }

    @Test
    fun `scenario complet fermeture et reouverture de l'app en cours de livraison`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 143, 999), SortDirection.ASCENDING)

            val firstVm = DeliveryViewModel(repository)
            firstVm.load()
            firstVm.onDelivered()
            firstVm.onCancelled()

            // Mort de process : un nouveau ViewModel, même repository (Room a survécu, lui).
            val secondVm = DeliveryViewModel(repository)
            secondVm.load()

            val state = secondVm.uiState as DeliveryUiState.InProgress
            assertEquals(143, state.currentCottage?.number)
            assertEquals(3, state.displayPosition)
            assertEquals(4, state.total)
            val statuses = repository.loadRound()?.cottages?.associate { it.number to it.status }
            assertEquals(CottageStatus.LIVRE, statuses?.get(3))
            assertEquals(CottageStatus.ANNULE, statuses?.get(35))
        }

    @Test
    fun `un statut acquis en livraison survit a une edition de la liste`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)
            vm.load()
            vm.onDelivered()

            repository.updateRound(listOf(3, 35, 78), SortDirection.ASCENDING)

            val newVm = DeliveryViewModel(repository)
            newVm.load()
            val statuses = repository.loadRound()?.cottages?.associate { it.number to it.status }
            assertEquals(CottageStatus.LIVRE, statuses?.get(3))
            assertEquals(CottageStatus.A_FAIRE, statuses?.get(78))
            val state = newVm.uiState as DeliveryUiState.InProgress
            assertEquals(35, state.currentCottage?.number)
        }

    @Test
    fun `load avec focusOnCottageNumber affiche ce cottage meme s'il n'est pas le courant`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)

            vm.load(focusOnCottageNumber = 143)

            val state = vm.uiState as DeliveryUiState.InProgress
            assertEquals(143, state.currentCottage?.number)
            assertEquals(3, state.displayPosition)
        }

    @Test
    fun `onDelivered en mode cible marque le cottage cible, pas le courant deduit`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)
            vm.load(focusOnCottageNumber = 143)

            vm.onDelivered()

            val statuses = repository.loadRound()?.cottages?.associate { it.number to it.status }
            assertEquals(CottageStatus.A_FAIRE, statuses?.get(3))
            assertEquals(CottageStatus.A_FAIRE, statuses?.get(35))
            assertEquals(CottageStatus.LIVRE, statuses?.get(143))
        }

    @Test
    fun `apres avoir marque un cottage cible, canAct passe a false meme si isFinished reste false`() =
        runTest {
            // Régression constatée manuellement : en mode ciblé, le cottage
            // affiché ne change jamais (voir applyAndSave), donc isFinished
            // (basé sur "y a-t-il un cottage affiché ?") restait faux même
            // après un tap réussi — les boutons Livré/Annulé restaient
            // actifs sans aucun retour visuel que le geste avait fonctionné.
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)
            vm.load(focusOnCottageNumber = 143)

            vm.onDelivered()

            val state = vm.uiState as DeliveryUiState.InProgress
            assertEquals(143, state.currentCottage?.number)
            assertFalse(state.isFinished)
            assertFalse(state.canAct)
        }

    @Test
    fun `onCancelled en mode cible annule le cottage cible, pas le courant deduit`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)
            vm.load(focusOnCottageNumber = 35)

            vm.onCancelled()

            val statuses = repository.loadRound()?.cottages?.associate { it.number to it.status }
            assertEquals(CottageStatus.A_FAIRE, statuses?.get(3))
            assertEquals(CottageStatus.ANNULE, statuses?.get(35))
        }

    @Test
    fun `onPreviousCottage en mode cible repasse le cottage juste avant a A_FAIRE`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)
            vm.load(focusOnCottageNumber = 143)

            vm.onPreviousCottage()

            val statuses = repository.loadRound()?.cottages?.associate { it.number to it.status }
            assertEquals(CottageStatus.A_FAIRE, statuses?.get(35))
        }

    @Test
    fun `le focus survit a une mutation, l'index affiche suit le meme numero`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)
            vm.load(focusOnCottageNumber = 143)

            vm.onDelivered()

            val state = vm.uiState as DeliveryUiState.InProgress
            assertEquals(143, state.focusedNumber)
            assertEquals(143, state.currentCottage?.number)
            assertEquals(CottageStatus.LIVRE, state.currentCottage?.status)
        }

    @Test
    fun `un focus sur un numero absent retombe sur le courant deduit`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)
            val vm = DeliveryViewModel(repository)

            vm.load(focusOnCottageNumber = 999)

            val state = vm.uiState as DeliveryUiState.InProgress
            assertEquals(3, state.currentCottage?.number)
        }
}
