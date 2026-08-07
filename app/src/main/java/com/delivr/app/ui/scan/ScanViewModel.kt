package com.delivr.app.ui.scan

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.delivr.app.camera.ScanError
import com.delivr.app.camera.ScanOutcome

/**
 * État de l'écran de scan.
 */
sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Scanning : ScanUiState
    data class Success(val imageUri: Uri) : ScanUiState
    data object Cancelled : ScanUiState
    data class Error(val error: ScanError) : ScanUiState
}

/**
 * Gère l'état du scan en cours. Ne connaît rien de l'UI ni du SDK ML Kit :
 * elle ne fait que traduire un [ScanOutcome] en [ScanUiState] affichable.
 */
class ScanViewModel : ViewModel() {
    var uiState: ScanUiState by mutableStateOf(ScanUiState.Idle)
        private set

    fun onScanStarted() {
        uiState = ScanUiState.Scanning
    }

    fun onScanOutcome(outcome: ScanOutcome) {
        uiState = when (outcome) {
            is ScanOutcome.Success -> ScanUiState.Success(outcome.imageUri)
            is ScanOutcome.Cancelled -> ScanUiState.Cancelled
            is ScanOutcome.Error -> ScanUiState.Error(outcome.error)
        }
    }
}
