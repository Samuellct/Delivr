package com.delivr.app.ui.scan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.delivr.app.camera.ScanError
import com.delivr.app.camera.ScanOutcome

/**
 * État de l'écran de scan.
 *
 * [Success.imagePath] est un chemin de fichier dans le stockage privé de
 * l'app (voir [com.delivr.app.camera.ScanOutcome.Success]), pas une
 * [android.net.Uri] `content://` — celle-ci ne serait pas garantie lisible
 * après recréation du process.
 */
sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Scanning : ScanUiState
    data class Success(val imagePath: String) : ScanUiState
    data object Cancelled : ScanUiState
    data class Error(val error: ScanError) : ScanUiState
}

private const val KEY_KIND = "scan_ui_state_kind"
private const val KEY_IMAGE_PATH = "scan_ui_state_image_path"
private const val KEY_ERROR_KIND = "scan_ui_state_error_kind"
private const val KEY_ERROR_MESSAGE = "scan_ui_state_error_message"

private const val KIND_IDLE = "idle"
private const val KIND_SUCCESS = "success"
private const val KIND_CANCELLED = "cancelled"
private const val KIND_ERROR = "error"

private const val ERROR_NO_IMAGE = "no_image"
private const val ERROR_NO_DATA = "no_data"
private const val ERROR_INVALID_CONTEXT = "invalid_context"
private const val ERROR_LAUNCH_FAILED = "launch_failed"

private fun ScanError.toSavedParts(): Pair<String, String?> = when (this) {
    ScanError.NoImageReturned -> ERROR_NO_IMAGE to null
    ScanError.NoDataReturned -> ERROR_NO_DATA to null
    ScanError.InvalidContext -> ERROR_INVALID_CONTEXT to null
    is ScanError.LaunchFailed -> ERROR_LAUNCH_FAILED to technicalMessage
}

private fun restoreScanError(kind: String?, technicalMessage: String?): ScanError = when (kind) {
    ERROR_NO_IMAGE -> ScanError.NoImageReturned
    ERROR_NO_DATA -> ScanError.NoDataReturned
    ERROR_INVALID_CONTEXT -> ScanError.InvalidContext
    else -> ScanError.LaunchFailed(technicalMessage)
}

/**
 * Reconstruit [ScanUiState] depuis les primitives persistées dans
 * [SavedStateHandle] (String uniquement — pas de dépendance à un `Saver`
 * Compose, pour rester sur l'API stable de `SavedStateHandle`).
 *
 * Décision de conception : un état [ScanUiState.Scanning] n'est jamais
 * persisté tel quel (voir [persistUiState]) — un flux ML Kit interrompu par
 * une vraie mort de process ne peut pas reprendre là où il en était (son
 * activité de résultat n'existe plus). Seuls les états terminaux
 * ([ScanUiState.Success], [ScanUiState.Cancelled], [ScanUiState.Error])
 * sont restaurés ; sinon on repart proprement de [ScanUiState.Idle], qui
 * relance un nouveau scan (voir `ScanScreen.kt`).
 */
private fun restoreUiState(savedStateHandle: SavedStateHandle): ScanUiState =
    when (savedStateHandle.get<String>(KEY_KIND)) {
        KIND_SUCCESS -> savedStateHandle.get<String>(KEY_IMAGE_PATH)
            ?.let { ScanUiState.Success(it) }
            ?: ScanUiState.Idle
        KIND_CANCELLED -> ScanUiState.Cancelled
        KIND_ERROR -> ScanUiState.Error(
            restoreScanError(
                savedStateHandle.get<String>(KEY_ERROR_KIND),
                savedStateHandle.get<String>(KEY_ERROR_MESSAGE)
            )
        )
        else -> ScanUiState.Idle
    }

private fun persistUiState(savedStateHandle: SavedStateHandle, state: ScanUiState) {
    when (state) {
        ScanUiState.Idle, ScanUiState.Scanning -> {
            savedStateHandle[KEY_KIND] = KIND_IDLE
            savedStateHandle[KEY_IMAGE_PATH] = null
            savedStateHandle[KEY_ERROR_KIND] = null
            savedStateHandle[KEY_ERROR_MESSAGE] = null
        }
        is ScanUiState.Success -> {
            savedStateHandle[KEY_KIND] = KIND_SUCCESS
            savedStateHandle[KEY_IMAGE_PATH] = state.imagePath
        }
        ScanUiState.Cancelled -> {
            savedStateHandle[KEY_KIND] = KIND_CANCELLED
        }
        is ScanUiState.Error -> {
            savedStateHandle[KEY_KIND] = KIND_ERROR
            val (kind, message) = state.error.toSavedParts()
            savedStateHandle[KEY_ERROR_KIND] = kind
            savedStateHandle[KEY_ERROR_MESSAGE] = message
        }
    }
}

/**
 * Gère l'état du scan en cours. Ne connaît rien de l'UI ni du SDK ML Kit :
 * elle ne fait que traduire un [ScanOutcome] en [ScanUiState] affichable.
 *
 * [uiState] est répliqué dans [SavedStateHandle] à chaque changement pour
 * survivre à la fois aux changements de configuration (rotation) et à la
 * mort du process en arrière-plan — sans ça, un scan terminé était perdu
 * dans les deux cas.
 */
class ScanViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    var uiState: ScanUiState by mutableStateOf(restoreUiState(savedStateHandle))
        private set

    fun onScanStarted() {
        applyUiState(ScanUiState.Scanning)
    }

    fun onScanOutcome(outcome: ScanOutcome) {
        applyUiState(
            when (outcome) {
                is ScanOutcome.Success -> ScanUiState.Success(outcome.imagePath)
                is ScanOutcome.Cancelled -> ScanUiState.Cancelled
                is ScanOutcome.Error -> ScanUiState.Error(outcome.error)
            }
        )
    }

    private fun applyUiState(state: ScanUiState) {
        uiState = state
        persistUiState(savedStateHandle, state)
    }
}
