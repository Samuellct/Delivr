package com.delivr.app.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delivr.app.repository.RoundRepository
import kotlinx.coroutines.launch

/**
 * Recréé en Phase 5 après sa suppression en Phase 1 (« jamais instancié ») :
 * il a maintenant une raison d'exister, répondre à « existe-t-il une
 * tournée en cours ? » depuis Room.
 *
 * Pas de `SavedStateHandle` ici, contrairement à `ScanViewModel`/
 * `ValidationViewModel` : l'unique état de cet écran est un booléen dont la
 * source de vérité est la base, pas la mémoire. Après une mort de process,
 * le relire coûte une simple requête `COUNT(*)` — le persister serait à la
 * fois inutile et risqué (un booléen périmé activerait un bouton qui ne
 * mènerait à rien).
 */
class HomeViewModel(
    private val repository: RoundRepository,
) : ViewModel() {
    var hasTourneeEnCours: Boolean by mutableStateOf(false)
        private set

    /**
     * Appelé à chaque arrivée sur l'écran d'accueil (voir `HomeRoute` dans
     * `HomeScreen.kt`). La valeur initiale `false` est volontairement
     * pessimiste : elle grise le bouton pendant la poignée de millisecondes
     * de la requête, plutôt que de proposer une reprise qui échouerait.
     */
    fun refresh() {
        viewModelScope.launch { hasTourneeEnCours = repository.hasRoundInProgress() }
    }
}
