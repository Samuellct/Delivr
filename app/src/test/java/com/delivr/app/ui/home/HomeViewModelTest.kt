package com.delivr.app.ui.home

import com.delivr.app.MainDispatcherRule
import com.delivr.app.database.FakeRoundDao
import com.delivr.app.domain.SortDirection
import com.delivr.app.repository.RoundRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `hasTourneeEnCours vaut false avant tout refresh`() {
        val vm = HomeViewModel(RoundRepository(FakeRoundDao()))

        assertFalse(vm.hasTourneeEnCours)
    }

    @Test
    fun `refresh sur une base vide laisse hasTourneeEnCours a false`() =
        runTest {
            val vm = HomeViewModel(RoundRepository(FakeRoundDao()))

            vm.refresh()

            assertFalse(vm.hasTourneeEnCours)
        }

    @Test
    fun `refresh apres une tournee sauvegardee passe hasTourneeEnCours a true`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3, 35), SortDirection.ASCENDING)
            val vm = HomeViewModel(repository)

            vm.refresh()

            assertTrue(vm.hasTourneeEnCours)
        }

    @Test
    fun `refresh apres clearRound repasse hasTourneeEnCours a false`() =
        runTest {
            val repository = RoundRepository(FakeRoundDao())
            repository.startRound(listOf(3), SortDirection.ASCENDING)
            val vm = HomeViewModel(repository)
            vm.refresh()

            repository.clearRound()
            vm.refresh()

            assertFalse(vm.hasTourneeEnCours)
        }
}
