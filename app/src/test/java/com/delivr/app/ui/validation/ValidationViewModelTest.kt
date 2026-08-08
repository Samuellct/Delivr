package com.delivr.app.ui.validation

import androidx.lifecycle.SavedStateHandle
import com.delivr.app.MainDispatcherRule
import com.delivr.app.database.FakeRoundDao
import com.delivr.app.domain.SortDirection
import com.delivr.app.repository.RoundRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * [ValidationViewModel.extract] n'est pas testable en JVM (bitmap + ML Kit,
 * voir `ScanViewModelTest` pour la même limite côté scan) ; [resume] l'est,
 * et il donne accès à l'état `Success`, donc aux 4 méthodes de mutation —
 * toutes désormais persistées dans Room (Phase 5.3). Utilise [FakeRoundDao]
 * (JVM pur), donc jouée par `ci.yml` à chaque push.
 */
class ValidationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(repository: RoundRepository = RoundRepository(FakeRoundDao())) =
        ValidationViewModel(SavedStateHandle(), repository)

    @Test
    fun `resume sur une base pre-remplie rend Success avec les memes numeros et le meme sens`() =
        runTest {
            val dao = FakeRoundDao()
            val repository = RoundRepository(dao)
            repository.startRound(listOf(3, 35, 143), SortDirection.DESCENDING)

            val vm = viewModel(repository)
            vm.resume()

            val state = vm.uiState
            assertTrue(state is ValidationUiState.Success)
            assertEquals(listOf(3, 35, 143), (state as ValidationUiState.Success).cottageNumbers)
            assertEquals(SortDirection.DESCENDING, state.sortDirection)
        }

    @Test
    fun `resume sur une base vide rend Error RoundUnavailable`() =
        runTest {
            val vm = viewModel()

            vm.resume()

            val state = vm.uiState
            assertTrue(state is ValidationUiState.Error)
            assertEquals(ValidationError.RoundUnavailable, (state as ValidationUiState.Error).error)
        }

    @Test
    fun `onAddCottage persiste le nouveau numero au bon rang`() =
        runTest {
            val dao = FakeRoundDao()
            val repository = RoundRepository(dao)
            repository.startRound(listOf(3, 143), SortDirection.ASCENDING)
            val vm = viewModel(repository)
            vm.resume()

            vm.onAddCottage(35)

            assertEquals(listOf(3, 35, 143), repository.loadRound()?.cottageNumbers)
        }

    @Test
    fun `onRemoveCottage fait disparaitre le numero de la base`() =
        runTest {
            val dao = FakeRoundDao()
            val repository = RoundRepository(dao)
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)
            val vm = viewModel(repository)
            vm.resume()

            vm.onRemoveCottage(35)

            assertEquals(listOf(3, 143), repository.loadRound()?.cottageNumbers)
        }

    @Test
    fun `onUpdateCottage remplace l'ancien numero par le nouveau, bien positionne`() =
        runTest {
            val dao = FakeRoundDao()
            val repository = RoundRepository(dao)
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)
            val vm = viewModel(repository)
            vm.resume()

            vm.onUpdateCottage(oldNumber = 35, newNumber = 999)

            assertEquals(listOf(3, 143, 999), repository.loadRound()?.cottageNumbers)
        }

    @Test
    fun `onSortDirectionChange persiste le nouveau sens et l'ordre inverse`() =
        runTest {
            val dao = FakeRoundDao()
            val repository = RoundRepository(dao)
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)
            val vm = viewModel(repository)
            vm.resume()

            vm.onSortDirectionChange(SortDirection.DESCENDING)

            val saved = repository.loadRound()
            assertEquals(SortDirection.DESCENDING, saved?.sortDirection)
            assertEquals(listOf(143, 35, 3), saved?.cottageNumbers)
        }

    @Test
    fun `une mutation refusee par le domaine laisse la base inchangee`() =
        runTest {
            val dao = FakeRoundDao()
            val repository = RoundRepository(dao)
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)
            val vm = viewModel(repository)
            vm.resume()

            // 35 existe déjà : addCottageNumber (domain/CottageList.kt) rend
            // la liste inchangée, donc rien de nouveau ne doit être persisté.
            vm.onAddCottage(35)

            assertEquals(listOf(3, 35), repository.loadRound()?.cottageNumbers)
        }

    @Test
    fun `mort de process apres resume plus mutation restitue l'etat sans relire la base`() =
        runTest {
            val dao = FakeRoundDao()
            val repository = RoundRepository(dao)
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)
            val savedStateHandle = SavedStateHandle()
            val firstVm = ValidationViewModel(savedStateHandle, repository)
            firstVm.resume()
            firstVm.onAddCottage(78)

            // Même SavedStateHandle, nouveau ViewModel : simule une rotation
            // ou une mort de process (voir ScanViewModelTest, même motif).
            val recreatedVm = ValidationViewModel(savedStateHandle, repository)

            val state = recreatedVm.uiState
            assertTrue(state is ValidationUiState.Success)
            assertEquals(listOf(3, 35, 78), (state as ValidationUiState.Success).cottageNumbers)
        }

    @Test
    fun `scenario complet fermeture et reouverture de l'app en cours de tournee`() =
        runTest {
            val dao = FakeRoundDao()
            val repository = RoundRepository(dao)
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)

            // VM n°1 : reprend la tournée, puis la modifie (comme le ferait
            // l'écran de validation avant que l'app ne soit tuée).
            val firstVm = ValidationViewModel(SavedStateHandle(), repository)
            firstVm.resume()
            firstVm.onRemoveCottage(35)
            firstVm.onAddCottage(999)
            firstVm.onSortDirectionChange(SortDirection.DESCENDING)

            // VM n°2 : SavedStateHandle neuf (mort de process réelle, pas
            // juste une rotation) + le même repository (Room a survécu, lui).
            val secondVm = ValidationViewModel(SavedStateHandle(), repository)
            secondVm.resume()

            val state = secondVm.uiState
            assertTrue(state is ValidationUiState.Success)
            assertEquals(listOf(999, 143, 3), (state as ValidationUiState.Success).cottageNumbers)
            assertEquals(SortDirection.DESCENDING, state.sortDirection)
        }
}
