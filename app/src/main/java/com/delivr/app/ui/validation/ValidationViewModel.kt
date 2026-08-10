package com.delivr.app.ui.validation

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delivr.app.domain.ExtractionResult
import com.delivr.app.domain.SortDirection
import com.delivr.app.domain.addCottageNumber
import com.delivr.app.domain.extractCottageNumbers
import com.delivr.app.domain.removeCottageNumber
import com.delivr.app.domain.sortCottageNumbers
import com.delivr.app.domain.updateCottageNumber
import com.delivr.app.ocr.recognizeText
import com.delivr.app.repository.RoundRepository
import com.delivr.app.utils.loadFullResolutionBitmap
import kotlinx.coroutines.launch

/**
 * Tag de journalisation des durées du pipeline de scan (TODO_V1.md, Phase
 * 8.1). Volontairement non conditionné à `BuildConfig.DEBUG` : la mesure
 * doit porter sur l'APK release (celui qu'on publie), pas sur un build
 * debug non représentatif. Coût : une ligne de log par scan, sans donnée
 * personnelle (des durées en millisecondes et un nombre de cottages).
 */
private const val PERF_TAG = "DelivrPerf"

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

    /**
     * Le bouton « Reprendre » menait ici jusqu'en Phase 5.4. Depuis la
     * Phase 6, il mène directement au mode Livraison : plus aucune route de
     * navigation n'atteint ce cas. Conservé parce que [ValidationViewModel.resume]
     * l'est (voir son KDoc).
     */
    data object RoundUnavailable : ValidationError
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

    /**
     * [cottageNumbers] est trié selon [sortDirection] et sans doublon.
     * [sortDirection] par défaut à [SortDirection.ASCENDING] : c'est déjà
     * l'ordre produit par `extractCottageNumbers` (Phase 3) à l'arrivée sur
     * cet écran.
     */
    data class Success(
        val cottageNumbers: List<Int>,
        val sortDirection: SortDirection = SortDirection.ASCENDING,
    ) : ValidationUiState

    data class Error(
        val error: ValidationError,
    ) : ValidationUiState
}

private const val KEY_KIND = "validation_ui_state_kind"
private const val KEY_NUMBERS = "validation_ui_state_numbers"
private const val KEY_SORT_DIRECTION = "validation_ui_state_sort_direction"
private const val KEY_ERROR_KIND = "validation_ui_state_error_kind"

private const val KIND_SUCCESS = "success"
private const val KIND_ERROR = "error"

private const val ERROR_IMAGE_UNREADABLE = "image_unreadable"
private const val ERROR_HEADER_NOT_FOUND = "header_not_found"
private const val ERROR_NO_NUMBERS_FOUND = "no_numbers_found"
private const val ERROR_ROUND_UNAVAILABLE = "round_unavailable"

private const val SORT_ASCENDING = "asc"
private const val SORT_DESCENDING = "desc"

private fun SortDirection.toSavedValue(): String =
    when (this) {
        SortDirection.ASCENDING -> SORT_ASCENDING
        SortDirection.DESCENDING -> SORT_DESCENDING
    }

private fun restoreSortDirection(value: String?): SortDirection =
    if (value == SORT_DESCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING

private fun ValidationError.toSavedKind(): String =
    when (this) {
        ValidationError.ImageUnreadable -> ERROR_IMAGE_UNREADABLE
        ValidationError.HeaderNotFound -> ERROR_HEADER_NOT_FOUND
        ValidationError.NoNumbersFound -> ERROR_NO_NUMBERS_FOUND
        ValidationError.RoundUnavailable -> ERROR_ROUND_UNAVAILABLE
    }

private fun restoreValidationError(kind: String?): ValidationError =
    when (kind) {
        ERROR_HEADER_NOT_FOUND -> ValidationError.HeaderNotFound
        ERROR_NO_NUMBERS_FOUND -> ValidationError.NoNumbersFound
        ERROR_ROUND_UNAVAILABLE -> ValidationError.RoundUnavailable
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
                ValidationUiState.Success(
                    cottageNumbers = numbers.toList(),
                    sortDirection = restoreSortDirection(savedStateHandle.get<String>(KEY_SORT_DIRECTION)),
                )
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
            savedStateHandle[KEY_SORT_DIRECTION] = null
            savedStateHandle[KEY_ERROR_KIND] = null
        }
        is ValidationUiState.Success -> {
            savedStateHandle[KEY_KIND] = KIND_SUCCESS
            savedStateHandle[KEY_NUMBERS] = state.cottageNumbers.toIntArray()
            savedStateHandle[KEY_SORT_DIRECTION] = state.sortDirection.toSavedValue()
        }
        is ValidationUiState.Error -> {
            savedStateHandle[KEY_KIND] = KIND_ERROR
            savedStateHandle[KEY_ERROR_KIND] = state.error.toSavedKind()
        }
    }
}

/**
 * Pilote l'extraction des numéros de cottage depuis l'image scannée, et leur
 * édition manuelle (Phase 4) : charge le fichier en pleine résolution, fait
 * tourner l'OCR (`ocr/`), isole la colonne « Cott » (`domain/`), puis
 * applique les corrections de l'utilisateur (ajout/suppression/modification/
 * sens de tournée) via les fonctions pures de `domain/CottageList.kt`. Ne
 * connaît ni Compose ni ML Kit directement — orchestre seulement les
 * couches, comme `ScanViewModel` orchestre `camera/` sans connaître le SDK.
 *
 * [uiState] est répliqué dans [SavedStateHandle] pour survivre aux
 * changements de configuration et à la mort du process ; [repository]
 * réplique en plus la tournée dans Room (Phase 5) pour survivre à la
 * fermeture réelle de l'app — les deux mécanismes coexistent sans se
 * contredire, alimentés par le même instantané d'état.
 */
class ValidationViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: RoundRepository,
) : ViewModel() {
    var uiState: ValidationUiState by mutableStateOf(restoreUiState(savedStateHandle))
        private set

    fun extract(imagePath: String) {
        applyUiState(ValidationUiState.Extracting)
        viewModelScope.launch {
            val startAt = SystemClock.elapsedRealtime()

            val bitmap = loadFullResolutionBitmap(imagePath)
            val decodedAt = SystemClock.elapsedRealtime()
            if (bitmap == null) {
                applyUiState(ValidationUiState.Error(ValidationError.ImageUnreadable))
                return@launch
            }

            val elements = recognizeText(bitmap)
            val ocrAt = SystemClock.elapsedRealtime()
            val result = extractCottageNumbers(elements, imageWidthPx = bitmap.width)
            val extractedAt = SystemClock.elapsedRealtime()
            val newState =
                when (result) {
                    is ExtractionResult.Success -> ValidationUiState.Success(result.cottageNumbers)
                    ExtractionResult.HeaderNotFound -> ValidationUiState.Error(ValidationError.HeaderNotFound)
                    ExtractionResult.NoNumbersFound -> ValidationUiState.Error(ValidationError.NoNumbersFound)
                }
            val cottageCount = (result as? ExtractionResult.Success)?.cottageNumbers?.size ?: 0
            Log.i(
                PERF_TAG,
                "decode=${decodedAt - startAt}ms ocr=${ocrAt - decodedAt}ms " +
                    "extract=${extractedAt - ocrAt}ms total=${extractedAt - startAt}ms " +
                    "cottages=$cottageCount",
            )
            applyUiState(newState)
            // La tournée naît en base dès que l'OCR réussit (TODO_V1.md 5.3) :
            // à partir d'ici, « Reprendre la tournée en cours » fonctionne,
            // même si l'utilisateur tue l'app avant d'avoir touché à la
            // liste. Corollaire assumé : c'est aussi ici, et pas au tap sur
            // « Nouvelle tournée », que l'ancienne tournée est écrasée — un
            // scan annulé ou un OCR raté laisse donc la tournée précédente
            // intacte et reprenable.
            if (newState is ValidationUiState.Success) {
                repository.startRound(newState.cottageNumbers, newState.sortDirection)
            }
        }
    }

    /**
     * Recharge la liste depuis Room au lieu de refaire tourner l'OCR.
     *
     * **Plus branché sur aucune route de navigation depuis la Phase 6** : la
     * reprise d'une tournée mène désormais directement au mode Livraison
     * (`Routes.DELIVERY`). Volontairement conservé plutôt que supprimé —
     * c'est le seul chemin par lequel `ValidationViewModelTest` peut amener
     * ce ViewModel dans l'état `Success` en JVM pure (donc en CI), [extract]
     * exigeant un décodage bitmap et ML Kit, tous deux indisponibles hors
     * appareil. Supprimer cette méthode coûterait la couverture automatisée
     * des quatre méthodes de mutation de la Phase 4.
     */
    fun resume() {
        applyUiState(ValidationUiState.Extracting)
        viewModelScope.launch {
            val saved = repository.loadRound()
            applyUiState(
                if (saved == null) {
                    ValidationUiState.Error(ValidationError.RoundUnavailable)
                } else {
                    ValidationUiState.Success(saved.cottageNumbers, saved.sortDirection)
                },
            )
        }
    }

    fun onAddCottage(number: Int) {
        updateSuccessState { addCottageNumber(it.cottageNumbers, number, it.sortDirection) }
    }

    fun onRemoveCottage(number: Int) {
        updateSuccessState { removeCottageNumber(it.cottageNumbers, number) }
    }

    fun onUpdateCottage(
        oldNumber: Int,
        newNumber: Int,
    ) {
        updateSuccessState { updateCottageNumber(it.cottageNumbers, oldNumber, newNumber, it.sortDirection) }
    }

    fun onSortDirectionChange(direction: SortDirection) {
        val current = uiState as? ValidationUiState.Success ?: return
        applyAndSave(
            current.copy(
                cottageNumbers = sortCottageNumbers(current.cottageNumbers, direction),
                sortDirection = direction,
            ),
        )
    }

    private inline fun updateSuccessState(newNumbers: (ValidationUiState.Success) -> List<Int>) {
        val current = uiState as? ValidationUiState.Success ?: return
        applyAndSave(current.copy(cottageNumbers = newNumbers(current)))
    }

    /**
     * Sauvegarde automatique en continu (TODO_V1.md 5.3) : l'UI est mise à
     * jour immédiatement (synchrone, via [applyUiState]), l'écriture Room
     * part en tâche de fond. L'ordre des écritures concurrentes est garanti
     * par le mutex de [RoundRepository], pas par [viewModelScope].
     */
    private fun applyAndSave(state: ValidationUiState.Success) {
        applyUiState(state)
        viewModelScope.launch { repository.updateRound(state.cottageNumbers, state.sortDirection) }
    }

    private fun applyUiState(state: ValidationUiState) {
        uiState = state
        persistUiState(savedStateHandle, state)
    }
}
