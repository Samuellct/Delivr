package com.delivr.app.ui.list

import com.delivr.app.MainDispatcherRule
import com.delivr.app.database.FakeRoundDao
import com.delivr.app.domain.CottageStatus
import com.delivr.app.domain.SortDirection
import com.delivr.app.repository.RoundRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * [ListViewModel] testé avec [FakeRoundDao], sur le modèle de
 * `HomeViewModelTest`/`DeliveryViewModelTest`. Jouée en JVM pur, donc par
 * `ci.yml` à chaque push.
 */
class ListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `l'etat initial avant tout load est Loading`() {
        val vm = ListViewModel(RoundRepository(FakeRoundDao()))

        assertEquals(ListUiState.Loading, vm.uiState)
    }

    @Test
    fun `load reflete les statuts melanges de chaque cottage`() =
        runTest {
            val dao = FakeRoundDao()
            val repository = RoundRepository(dao)
            repository.startRound(listOf(3, 35, 143), SortDirection.ASCENDING)
            dao.setStatusForTest(3, CottageStatus.LIVRE)
            dao.setStatusForTest(35, CottageStatus.ANNULE)
            val vm = ListViewModel(repository)

            vm.load()

            val state = vm.uiState
            assertTrue(state is ListUiState.Success)
            val cottages = (state as ListUiState.Success).cottages
            assertEquals(CottageStatus.LIVRE, cottages.first { it.number == 3 }.status)
            assertEquals(CottageStatus.ANNULE, cottages.first { it.number == 35 }.status)
            assertEquals(CottageStatus.A_FAIRE, cottages.first { it.number == 143 }.status)
        }

    @Test
    fun `load conserve l'ordre de la tournee`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(999, 3, 35), SortDirection.ASCENDING)
            val vm = ListViewModel(repository)

            vm.load()

            val state = vm.uiState as ListUiState.Success
            assertEquals(listOf(999, 3, 35), state.cottages.map { it.number })
        }

    @Test
    fun `load sur une base vide rend Error RoundUnavailable`() =
        runTest {
            val vm = ListViewModel(RoundRepository(FakeRoundDao()))

            vm.load()

            val state = vm.uiState
            assertTrue(state is ListUiState.Error)
            assertEquals(ListError.RoundUnavailable, (state as ListUiState.Error).error)
        }
}
