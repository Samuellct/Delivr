package com.delivr.app.camera

/**
 * Cause d'un échec de scan, sous forme de type plutôt que de texte déjà
 * formaté. [ScanOutcome.Error] et [com.delivr.app.ui.scan.ScanUiState.Error]
 * portent ce type ; c'est seulement au moment de l'affichage (dans un
 * composable, via `stringResource`) qu'un message localisé en est dérivé —
 * voir `ScanScreen.kt`. Ça évite deux problèmes du code précédent : des
 * messages en dur impossibles à localiser, et le texte brut d'exception
 * ML Kit ([LaunchFailed.technicalMessage]) affiché tel quel à l'utilisateur.
 */
sealed interface ScanError {
    /** Le scanner a rendu la main avec succès mais sans URI d'image exploitable. */
    data object NoImageReturned : ScanError

    /** Le scanner a rendu la main en succès (RESULT_OK) sans données du tout. */
    data object NoDataReturned : ScanError

    /** Pas d'Activity disponible pour démarrer l'intent du scanner. */
    data object InvalidContext : ScanError

    /**
     * Échec du démarrage du scanner côté ML Kit (ex. Play services
     * indisponible). [technicalMessage] est conservé pour le diagnostic
     * (log uniquement) mais jamais affiché directement à l'utilisateur.
     */
    data class LaunchFailed(val technicalMessage: String?) : ScanError
}
