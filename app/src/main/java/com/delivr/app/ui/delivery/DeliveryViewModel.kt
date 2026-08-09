package com.delivr.app.ui.delivery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delivr.app.domain.Cottage
import com.delivr.app.domain.CottageStatus
import com.delivr.app.domain.currentCottageIndex
import com.delivr.app.domain.goBackToPreviousCottage
import com.delivr.app.domain.markCurrentCottage
import com.delivr.app.repository.RoundRepository
import kotlinx.coroutines.launch

/**
 * Cause d'un échec du mode Livraison. Volontairement **distinct** de
 * `ValidationError` malgré le cas homonyme : ce sont deux écrans, deux
 * messages, et les fusionner créerait une dépendance entre deux
 * fonctionnalités qui n'en ont aucune.
 */
sealed interface DeliveryError {
    /** La base ne contient aucune tournée (cas défensif : on n'arrive ici que depuis une tournée existante). */
    data object RoundUnavailable : DeliveryError
}

/**
 * État de l'écran de livraison.
 *
 * [InProgress] ne porte qu'**une** donnée, la liste des cottages avec leurs
 * statuts : tout le reste (position courante, total, fin de tournée) en est
 * dérivé par `domain/DeliveryProgress.kt`. Une seule source de vérité, donc
 * aucun risque de compteur désynchronisé — et les tests Compose peuvent
 * piloter l'écran avec la seule liste (voir `DeliveryScreenTest`).
 */
sealed interface DeliveryUiState {
    data object Loading : DeliveryUiState

    data class InProgress(
        val cottages: List<Cottage>,
    ) : DeliveryUiState {
        /** Index (base 0) du cottage courant, ou [total] si la tournée est finie. */
        val currentIndex: Int get() = currentCottageIndex(cottages)

        val total: Int get() = cottages.size

        /** `null` quand la tournée est terminée. */
        val currentCottage: Cottage? get() = cottages.getOrNull(currentIndex)

        val isFinished: Boolean get() = currentCottage == null

        /** Le tout premier cottage n'a pas de précédent : « Retour » y est inerte. */
        val canGoBack: Boolean get() = currentIndex > 0

        /** Position affichée, en base 1 (« 6 / 24 », `Presentation.md` § Mode Livraison). */
        val displayPosition: Int get() = (currentIndex + 1).coerceAtMost(total)
    }

    data class Error(
        val error: DeliveryError,
    ) : DeliveryUiState
}

/**
 * Pilote le mode Livraison (Phase 6) : relit la tournée depuis Room, puis
 * applique les gestes de l'utilisateur (livré / annulé / retour) via les
 * fonctions pures de `domain/DeliveryProgress.kt`.
 *
 * Pas de `SavedStateHandle`, comme `HomeViewModel` et contrairement à
 * `ValidationViewModel` : chaque geste est écrit dans Room dans la foulée,
 * donc la base **est** l'état de cet écran. Après une rotation ou une mort
 * de process, [load] suffit à retrouver exactement la même position — la
 * répliquer en mémoire n'ajouterait rien et pourrait diverger.
 */
class DeliveryViewModel(
    private val repository: RoundRepository,
) : ViewModel() {
    var uiState: DeliveryUiState by mutableStateOf(DeliveryUiState.Loading)
        private set

    /**
     * Appelé une fois à l'arrivée sur l'écran (voir `DeliveryRoute` dans
     * `DeliveryScreen.kt`, qui le conditionne à l'état [DeliveryUiState.Loading]
     * pour ne pas rejouer la lecture après un changement de configuration).
     */
    fun load() {
        viewModelScope.launch {
            val saved = repository.loadRound()
            uiState =
                if (saved == null) {
                    DeliveryUiState.Error(DeliveryError.RoundUnavailable)
                } else {
                    DeliveryUiState.InProgress(saved.cottages)
                }
        }
    }

    /** Tap simple sur la coche verte (cas courant, `TODO_V1.md` 6.3). */
    fun onDelivered() {
        markCurrent(CottageStatus.LIVRE)
    }

    /** Appui long sur la croix rouge — le garde-fou du geste est dans l'UI, pas ici. */
    fun onCancelled() {
        markCurrent(CottageStatus.ANNULE)
    }

    /** Repasse le cottage précédent à « à faire », qui redevient donc le courant. */
    fun onPreviousCottage() {
        val current = uiState as? DeliveryUiState.InProgress ?: return
        val target = current.cottages.getOrNull(current.currentIndex - 1) ?: return
        applyAndSave(
            cottages = goBackToPreviousCottage(current.cottages),
            number = target.number,
            status = CottageStatus.A_FAIRE,
        )
    }

    private fun markCurrent(status: CottageStatus) {
        val current = uiState as? DeliveryUiState.InProgress ?: return
        val target = current.currentCottage ?: return
        applyAndSave(
            cottages = markCurrentCottage(current.cottages, status),
            number = target.number,
            status = status,
        )
    }

    /**
     * Même idiome que `ValidationViewModel.applyAndSave` : l'UI est mise à
     * jour **synchrone** (le numéro suivant apparaît dans l'instant, ce que
     * demande le modèle « coup d'œil » de `Presentation.md` § Mode
     * Livraison), l'écriture Room part en tâche de fond. Plus simple ici que
     * côté validation : une seule ligne change, donc un `UPDATE` ciblé au
     * lieu d'une réécriture complète de la liste.
     */
    private fun applyAndSave(
        cottages: List<Cottage>,
        number: Int,
        status: CottageStatus,
    ) {
        uiState = DeliveryUiState.InProgress(cottages)
        viewModelScope.launch { repository.updateCottageStatus(number = number, status = status) }
    }
}
