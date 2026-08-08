package com.delivr.app.navigation

import android.net.Uri

/**
 * Identifiants des écrans de l'application.
 */
object Routes {
    const val HOME = "home"
    const val SCAN = "scan"

    /** Argument de route brut ; utiliser [validation] pour construire une destination. */
    const val VALIDATION = "validation/{imagePath}"
    const val VALIDATION_ARG_IMAGE_PATH = "imagePath"

    /**
     * Le chemin de fichier de l'image scannée est passé en argument de
     * navigation ; encodé car il contient des `/` (illégaux dans un segment
     * de route).
     */
    fun validation(imagePath: String) = "validation/${Uri.encode(imagePath)}"

    /**
     * Reprise de la tournée sauvegardée (Phase 5.4) : même écran que
     * [VALIDATION], mais sans argument `imagePath` — il n'y a rien à
     * extraire, la liste vient de Room. Route distincte plutôt qu'un
     * argument optionnel : chacune a sa propre entrée de pile (donc son
     * propre ViewModel/SavedStateHandle), et le graphe dit explicitement
     * lequel des deux chemins a été pris.
     */
    const val VALIDATION_RESUME = "validation_resume"
}
