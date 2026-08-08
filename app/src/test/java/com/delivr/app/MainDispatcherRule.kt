package com.delivr.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Remplace `Dispatchers.Main` (absent sur la JVM de test) par un dispatcher
 * de test le temps du test — nécessaire pour tout ViewModel qui utilise
 * `viewModelScope` (ce que `ScanViewModel` ne fait pas, mais que
 * `ValidationViewModel`/`HomeViewModel` font depuis la Phase 5, pour
 * persister dans Room).
 *
 * [UnconfinedTestDispatcher] : les coroutines lancées via `launch`
 * s'exécutent immédiatement, donc les assertions peuvent suivre l'appel
 * sans `advanceUntilIdle()` — le plus simple à lire pour ces tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
