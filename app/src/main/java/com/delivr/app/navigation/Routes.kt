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
     * Mode Livraison (Phase 6). Sans argument : la tournée est toujours
     * relue depuis Room, qu'on arrive depuis l'écran de validation
     * (« Démarrer la tournée ») ou depuis l'accueil (« Reprendre la tournée
     * en cours »). Remplace la route `VALIDATION_RESUME` de la Phase 5.4 :
     * « Reprendre » ramène désormais directement au cottage courant, pas à
     * la liste à valider.
     */
    const val DELIVERY = "delivery"
}
