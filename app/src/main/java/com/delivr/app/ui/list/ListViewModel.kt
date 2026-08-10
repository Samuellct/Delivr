package com.delivr.app.ui.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delivr.app.domain.Cottage
import com.delivr.app.repository.RoundRepository
import kotlinx.coroutines.launch

/** Cause d'un échec de l'écran Liste (Phase 7). Distinct de `DeliveryError`/`ValidationError` : trois écrans, trois messages. */
sealed interface ListError {
    /** Cas défensif : on n'atteint normalement cet écran que depuis une tournée existante. */
    data object RoundUnavailable : ListError
}

/** État de l'écran Liste. */
sealed interface ListUiState {
    data object Loading : ListUiState

    /** [cottages] dans l'ordre de la tournée, chacun avec son statut courant. */
    data class Success(
        val cottages: List<Cottage>,
    ) : ListUiState

    data class Error(
        val error: ListError,
    ) : ListUiState
}

/**
 * Pilote l'écran Liste (Phase 7) : relit la tournée depuis Room pour
 * afficher tous les cottages avec leur statut.
 *
 * Pas de `SavedStateHandle`, comme `HomeViewModel`/`DeliveryViewModel` :
 * cet écran n'est qu'une fenêtre en lecture sur Room, qui reste la seule
 * source de vérité.
 */
class ListViewModel(
    private val repository: RoundRepository,
) : ViewModel() {
    var uiState: ListUiState by mutableStateOf(ListUiState.Loading)
        private set

    /**
     * Appelé à **chaque** arrivée sur l'écran (voir `ListRoute`), pas
     * seulement si l'état vaut encore [ListUiState.Loading] contrairement à
     * `DeliveryRoute`/`ValidationRoute` : on revient souvent ici après avoir
     * corrigé le statut d'un cottage précis en mode Livraison (navigation
     * rapide), et la liste doit refléter cet état à jour, pas un instantané
     * pris avant la correction. Le coût (une lecture Room) est négligeable.
     */
    fun load() {
        viewModelScope.launch {
            val saved = repository.loadRound()
            uiState =
                if (saved == null) {
                    ListUiState.Error(ListError.RoundUnavailable)
                } else {
                    ListUiState.Success(saved.cottages)
                }
        }
    }
}
