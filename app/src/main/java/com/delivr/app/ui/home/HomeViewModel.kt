package com.delivr.app.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * ViewModel de l'écran d'accueil.
 *
 * TODO: brancher [hasTourneeEnCours] sur le repository Room dès que la
 * sauvegarde automatique de la tournée sera en place.
 */
class HomeViewModel : ViewModel() {
    var hasTourneeEnCours by mutableStateOf(false)
        private set
}
