package com.delivr.app.ui.scan

import androidx.lifecycle.SavedStateHandle
import com.delivr.app.camera.ScanError
import com.delivr.app.camera.ScanOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ScanViewModel.onScanOutcome] est un mapper pur à 5 cas, sans dépendance
 * Android — aucun mock nécessaire. Ces tests couvrent aussi la persistance
 * dans [SavedStateHandle] (voir `ScanViewModel.kt`), en reconstruisant un
 * second ViewModel à partir du même handle pour simuler une recréation de
 * process.
 */
class ScanViewModelTest {
    @Test
    fun `etat initial est Idle`() {
        val viewModel = ScanViewModel(SavedStateHandle())

        assertEquals(ScanUiState.Idle, viewModel.uiState)
    }

    @Test
    fun `onScanStarted passe en Scanning`() {
        val viewModel = ScanViewModel(SavedStateHandle())

        viewModel.onScanStarted()

        assertEquals(ScanUiState.Scanning, viewModel.uiState)
    }

    @Test
    fun `onScanOutcome Success mappe vers ScanUiState Success avec le chemin du fichier`() {
        val viewModel = ScanViewModel(SavedStateHandle())

        viewModel.onScanOutcome(ScanOutcome.Success("/data/user/0/com.delivr.app/files/scans/current_scan.jpg"))

        val state = viewModel.uiState
        assertTrue(state is ScanUiState.Success)
        assertEquals(
            "/data/user/0/com.delivr.app/files/scans/current_scan.jpg",
            (state as ScanUiState.Success).imagePath,
        )
    }

    @Test
    fun `onScanOutcome Cancelled mappe vers ScanUiState Cancelled`() {
        val viewModel = ScanViewModel(SavedStateHandle())

        viewModel.onScanOutcome(ScanOutcome.Cancelled)

        assertEquals(ScanUiState.Cancelled, viewModel.uiState)
    }

    @Test
    fun `onScanOutcome Error mappe vers ScanUiState Error avec le meme type d'erreur`() {
        val viewModel = ScanViewModel(SavedStateHandle())

        viewModel.onScanOutcome(ScanOutcome.Error(ScanError.NoImageReturned))

        val state = viewModel.uiState
        assertTrue(state is ScanUiState.Error)
        assertEquals(ScanError.NoImageReturned, (state as ScanUiState.Error).error)
    }

    @Test
    fun `onScanOutcome LaunchFailed conserve le message technique sans le perdre`() {
        val viewModel = ScanViewModel(SavedStateHandle())

        viewModel.onScanOutcome(ScanOutcome.Error(ScanError.LaunchFailed("boom")))

        val error = (viewModel.uiState as ScanUiState.Error).error
        assertEquals(ScanError.LaunchFailed("boom"), error)
    }

    @Test
    fun `un scan reussi survit a une recreation du ViewModel avec le meme SavedStateHandle`() {
        // Simule ce qui se passe après la mort du process : le SavedStateHandle
        // est restauré par le système, et un nouveau ScanViewModel est construit
        // à partir de son contenu persisté.
        val savedStateHandle = SavedStateHandle()
        val firstViewModel = ScanViewModel(savedStateHandle)
        firstViewModel.onScanOutcome(ScanOutcome.Success("/tmp/scan.jpg"))

        val recreatedViewModel = ScanViewModel(savedStateHandle)

        assertEquals(ScanUiState.Success("/tmp/scan.jpg"), recreatedViewModel.uiState)
    }

    @Test
    fun `un scan interrompu en Scanning redevient Idle apres recreation`() {
        // Décision de conception documentée dans ScanViewModel.kt : un flux ML
        // Kit interrompu par une vraie mort de process ne peut pas reprendre.
        val savedStateHandle = SavedStateHandle()
        val firstViewModel = ScanViewModel(savedStateHandle)
        firstViewModel.onScanStarted()

        val recreatedViewModel = ScanViewModel(savedStateHandle)

        assertEquals(ScanUiState.Idle, recreatedViewModel.uiState)
    }
}
