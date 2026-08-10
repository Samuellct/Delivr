package com.delivr.app.ui.delivery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delivr.app.domain.Cottage
import com.delivr.app.domain.CottageStatus
import com.delivr.app.domain.currentCottageIndex
import com.delivr.app.domain.markCottageAtIndex
import com.delivr.app.domain.resetCottageBeforeIndex
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
 * [InProgress] porte la liste des cottages avec leurs statuts, plus
 * [pagerIndex] (voir son KDoc) : tout le reste (position courante, total,
 * fin de tournée) en est dérivé.
 */
sealed interface DeliveryUiState {
    data object Loading : DeliveryUiState

    data class InProgress(
        val cottages: List<Cottage>,
        /**
         * Curseur de défilement libre, utilisé uniquement en navigation
         * rapide depuis l'écran Liste (Phase 7) : `null` tant qu'on n'a pas
         * encore agi depuis un cottage ciblé. Contrairement au courant
         * déduit par [currentCottageIndex] (comportement Phase 6, utilisé
         * par « Reprendre »/« Démarrer la tournée »), ce curseur **avance ou
         * recule d'une position à chaque geste, quel que soit le statut du
         * cottage voisin** — c'est ce qui permet, après avoir sauté plusieurs
         * cottages loupés depuis la Liste, de les retraiter un par un « au
         * fil de l'eau » sans revenir systématiquement au premier « à faire »
         * de toute la tournée. Vit uniquement en mémoire (pas de colonne
         * Room) : fermer l'app et rouvrir avec « Reprendre » repart toujours
         * du courant déduit, jamais de ce curseur.
         */
        val pagerIndex: Int? = null,
    ) : DeliveryUiState {
        /**
         * Index (base 0) du cottage affiché : [pagerIndex] s'il est engagé,
         * sinon le cottage courant déduit par [currentCottageIndex]
         * (comportement Phase 6 inchangé). [total] si la tournée est finie
         * (ou si le défilement libre a dépassé la fin de la liste).
         */
        val currentIndex: Int get() = pagerIndex ?: currentCottageIndex(cottages)

        val total: Int get() = cottages.size

        /** `null` quand il n'y a plus de cottage à cet index (tournée terminée, ou fin du défilement libre). */
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
     *
     * [focusOnCottageNumber] non nul quand on arrive depuis l'écran Liste
     * (Phase 7) sur un cottage précis : engage
     * [DeliveryUiState.InProgress.pagerIndex] sur sa position dans la
     * tournée. Un numéro absent (ne devrait pas arriver) retombe
     * silencieusement sur le courant déduit.
     */
    fun load(focusOnCottageNumber: Int? = null) {
        viewModelScope.launch {
            val saved = repository.loadRound()
            uiState =
                if (saved == null) {
                    DeliveryUiState.Error(DeliveryError.RoundUnavailable)
                } else {
                    val pagerIndex =
                        focusOnCottageNumber?.let { number ->
                            saved.cottages.indexOfFirst { it.number == number }.takeIf { it >= 0 }
                        }
                    DeliveryUiState.InProgress(saved.cottages, pagerIndex = pagerIndex)
                }
        }
    }

    /** Tap simple sur la coche verte (cas courant, `TODO_V1.md` 6.3). */
    fun onDelivered() {
        mark(CottageStatus.LIVRE)
    }

    /** Appui long sur la croix rouge — le garde-fou du geste est dans l'UI, pas ici. */
    fun onCancelled() {
        mark(CottageStatus.ANNULE)
    }

    /**
     * En défilement libre ([DeliveryUiState.InProgress.pagerIndex] engagé) :
     * recule d'une position, sans toucher au statut de qui que ce soit — pure
     * navigation, pour pouvoir revoir un cottage déjà traité sans en effacer
     * le statut. En mode séquentiel (comportement Phase 6 inchangé) : repasse
     * le cottage précédent à « à faire », qui redevient donc le courant.
     */
    fun onPreviousCottage() {
        val current = uiState as? DeliveryUiState.InProgress ?: return

        if (current.pagerIndex != null) {
            val newIndex = current.pagerIndex - 1
            if (newIndex < 0) return
            uiState = current.copy(pagerIndex = newIndex)
            return
        }

        val target = current.cottages.getOrNull(current.currentIndex - 1) ?: return
        applyAndSave(
            current.copy(cottages = resetCottageBeforeIndex(current.cottages, current.currentIndex)),
            number = target.number,
            status = CottageStatus.A_FAIRE,
        )
    }

    /**
     * Marque le cottage affiché. En défilement libre, avance [pagerIndex]
     * d'une position après coup — vers le cottage suivant de la tournée,
     * quel que soit son statut (« au fil de l'eau »), pas vers le prochain
     * `A_FAIRE` : c'est justement ce qui permet de retraiter dans l'ordre une
     * plage de cottages loupés, y compris ceux déjà marqués par erreur.
     */
    private fun mark(status: CottageStatus) {
        val current = uiState as? DeliveryUiState.InProgress ?: return
        val target = current.currentCottage ?: return
        val updatedCottages = markCottageAtIndex(current.cottages, current.currentIndex, status)
        val newState =
            current.copy(
                cottages = updatedCottages,
                pagerIndex = current.pagerIndex?.plus(1),
            )
        applyAndSave(newState, number = target.number, status = status)
    }

    /**
     * Même idiome que `ValidationViewModel.applyAndSave` : l'UI est mise à
     * jour **synchrone** (le cottage suivant apparaît dans l'instant, ce que
     * demande le modèle « coup d'œil » de `Presentation.md` § Mode
     * Livraison), l'écriture Room part en tâche de fond. Plus simple ici que
     * côté validation : une seule ligne change, donc un `UPDATE` ciblé au
     * lieu d'une réécriture complète de la liste.
     */
    private fun applyAndSave(
        newState: DeliveryUiState.InProgress,
        number: Int,
        status: CottageStatus,
    ) {
        uiState = newState
        viewModelScope.launch { repository.updateCottageStatus(number = number, status = status) }
    }
}
