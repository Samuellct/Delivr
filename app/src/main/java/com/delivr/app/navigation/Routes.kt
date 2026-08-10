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

    /** Argument de route optionnel ; utiliser [delivery] pour construire une destination. */
    const val DELIVERY_ARG_COTTAGE_NUMBER = "cottageNumber"

    /** Valeur sentinelle : `NavType.IntType` ne supporte pas `nullable = true` (seul `String` le permet). */
    const val DELIVERY_NO_COTTAGE = -1

    /**
     * Mode Livraison (Phase 6). La tournée est toujours relue depuis Room,
     * qu'on arrive depuis l'écran de validation (« Démarrer la tournée »),
     * depuis l'accueil (« Reprendre la tournée en cours »), ou depuis l'écran
     * Liste (Phase 7, [DELIVERY_ARG_COTTAGE_NUMBER] renseigné) pour afficher
     * un cottage précis plutôt que le courant déduit. Argument **optionnel**
     * en style requête (`?arg={arg}`) — un argument obligatoire style chemin
     * (comme [VALIDATION]) ne peut pas être omis, ce dont les deux premiers
     * cas d'arrivée ont besoin. Patron brut à donner à `composable(route =
     * ...)`/`popBackStack`/`popUpTo` ; utiliser [delivery] pour construire
     * une destination concrète à donner à `navigate(...)`.
     */
    const val DELIVERY = "delivery?$DELIVERY_ARG_COTTAGE_NUMBER={$DELIVERY_ARG_COTTAGE_NUMBER}"

    /** [cottageNumber] non nul uniquement pour la navigation rapide depuis la Liste (Phase 7). */
    fun delivery(cottageNumber: Int? = null): String =
        if (cottageNumber == null) "delivery" else "delivery?$DELIVERY_ARG_COTTAGE_NUMBER=$cottageNumber"

    /** Écran Liste (Phase 7) : vue d'ensemble de la tournée, sans argument. */
    const val LIST = "list"
}
