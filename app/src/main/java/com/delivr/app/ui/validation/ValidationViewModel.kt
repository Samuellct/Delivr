package com.delivr.app.ui.validation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delivr.app.domain.ExtractionResult
import com.delivr.app.domain.extractCottageNumbers
import com.delivr.app.ocr.recognizeText
import com.delivr.app.utils.loadFullResolutionBitmap
import kotlinx.coroutines.launch

/**
 * Cause d'un échec d'extraction. Découplé du domaine (`ExtractionResult`
 * couvre déjà `HeaderNotFound`/`NoNumbersFound`) uniquement pour ajouter le
 * cas propre à cet écran, [ImageUnreadable] : le fichier scanné n'a pas pu
 * être décodé, avant même que l'OCR n'entre en jeu.
 */
sealed interface ValidationError {
    data object ImageUnreadable : ValidationError

    data object HeaderNotFound : ValidationError

    data object NoNumbersFound : ValidationError
}

/**
 * État de l'écran de validation.
 *
 * Pas d'état `Idle` : l'extraction démarre automatiquement à l'arrivée sur
 * l'écran (voir `ValidationScreen.kt`), il n'y a rien à attendre de
 * l'utilisateur avant de commencer, contrairement au scan.
 */
sealed interface ValidationUiState {
    data object Extracting : ValidationUiState

    /** [cottageNumbers] est trié par ordre croissant et sans doublon. */
    data class Success(
        val cottageNumbers: List<Int>,
    ) : ValidationUiState

    data class Error(
        val error: ValidationError,
    ) : ValidationUiState
}

private const val KEY_KIND = "validation_ui_state_kind"
private const val KEY_NUMBERS = "validation_ui_state_numbers"
private const val KEY_ERROR_KIND = "validation_ui_state_error_kind"

private const val KIND_SUCCESS = "success"
private const val KIND_ERROR = "error"

private const val ERROR_IMAGE_UNREADABLE = "image_unreadable"
private const val ERROR_HEADER_NOT_FOUND = "header_not_found"
private const val ERROR_NO_NUMBERS_FOUND = "no_numbers_found"

private fun ValidationError.toSavedKind(): String =
    when (this) {
        ValidationError.ImageUnreadable -> ERROR_IMAGE_UNREADABLE
        ValidationError.HeaderNotFound -> ERROR_HEADER_NOT_FOUND
        ValidationError.NoNumbersFound -> ERROR_NO_NUMBERS_FOUND
    }

private fun restoreValidationError(kind: String?): ValidationError =
    when (kind) {
        ERROR_HEADER_NOT_FOUND -> ValidationError.HeaderNotFound
        ERROR_NO_NUMBERS_FOUND -> ValidationError.NoNumbersFound
        else -> ValidationError.ImageUnreadable
    }

/**
 * Reconstruit [ValidationUiState] depuis les primitives persistées dans
 * [SavedStateHandle] (même approche que `ScanViewModel`, Phase 1.2 : lecture/
 * écriture directes plutôt qu'un `Saver` Compose). En l'absence de kind
 * persisté (première apparition de l'écran, ou extraction interrompue par
 * une mort de process), on repart de [ValidationUiState.Extracting] — relancer
 * l'extraction est sans risque, contrairement à relancer un scan ML Kit
 * (voir `ScanViewModel.kt`).
 */
private fun restoreUiState(savedStateHandle: SavedStateHandle): ValidationUiState =
    when (savedStateHandle.get<String>(KEY_KIND)) {
        KIND_SUCCESS -> {
            val numbers = savedStateHandle.get<IntArray>(KEY_NUMBERS)
            if (numbers != null) {
                ValidationUiState.Success(numbers.toList())
            } else {
                ValidationUiState.Extracting
            }
        }
        KIND_ERROR -> ValidationUiState.Error(restoreValidationError(savedStateHandle.get<String>(KEY_ERROR_KIND)))
        else -> ValidationUiState.Extracting
    }

private fun persistUiState(
    savedStateHandle: SavedStateHandle,
    state: ValidationUiState,
) {
    when (state) {
        ValidationUiState.Extracting -> {
            savedStateHandle[KEY_KIND] = null
            savedStateHandle[KEY_NUMBERS] = null
            savedStateHandle[KEY_ERROR_KIND] = null
        }
        is ValidationUiState.Success -> {
            savedStateHandle[KEY_KIND] = KIND_SUCCESS
            savedStateHandle[KEY_NUMBERS] = state.cottageNumbers.toIntArray()
        }
        is ValidationUiState.Error -> {
            savedStateHandle[KEY_KIND] = KIND_ERROR
            savedStateHandle[KEY_ERROR_KIND] = state.error.toSavedKind()
        }
    }
}

/**
 * Pilote l'extraction des numéros de cottage depuis l'image scannée : charge
 * le fichier en pleine résolution, fait tourner l'OCR (`ocr/`), puis isole
 * la colonne « Cott » (`domain/`). Ne connaît ni Compose ni ML Kit
 * directement — orchestre seulement les deux couches, comme `ScanViewModel`
 * orchestre `camera/` sans connaître le SDK.
 *
 * [uiState] est répliqué dans [SavedStateHandle] pour survivre aux
 * changements de configuration et à la mort du process — l'extraction est
 * rapide et sans effet de bord, mais pas instantanée, et il n'y a pas de
 * raison de la refaire à chaque rotation.
 */
class ValidationViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    var uiState: ValidationUiState by mutableStateOf(restoreUiState(savedStateHandle))
        private set

    fun extract(imagePath: String) {
        applyUiState(ValidationUiState.Extracting)
        viewModelScope.launch {
            val bitmap = loadFullResolutionBitmap(imagePath)
            if (bitmap == null) {
                applyUiState(ValidationUiState.Error(ValidationError.ImageUnreadable))
                return@launch
            }

            val elements = recognizeText(bitmap)
            val result = extractCottageNumbers(elements, imageWidthPx = bitmap.width)
            applyUiState(
                when (result) {
                    is ExtractionResult.Success -> ValidationUiState.Success(result.cottageNumbers)
                    ExtractionResult.HeaderNotFound -> ValidationUiState.Error(ValidationError.HeaderNotFound)
                    ExtractionResult.NoNumbersFound -> ValidationUiState.Error(ValidationError.NoNumbersFound)
                },
            )
        }
    }

    private fun applyUiState(state: ValidationUiState) {
        uiState = state
        persistUiState(savedStateHandle, state)
    }
}
