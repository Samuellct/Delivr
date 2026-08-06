package com.delivr.app.camera

import android.app.Activity.RESULT_OK
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Résultat d'une tentative de scan de document.
 *
 * La détection automatique des bords de la feuille, le redressement de
 * perspective et l'amélioration du contraste sont entièrement pris en charge
 * par le SDK ML Kit Document Scanner (Google Play services) — voir
 * https://developers.google.com/ml-kit/vision/doc-scanner/android.
 * Ce module ne fait qu'orchestrer l'appel et exposer un résultat exploitable
 * par l'écran de scan.
 */
sealed interface ScanOutcome {
    data class Success(val imageUri: Uri) : ScanOutcome
    data object Cancelled : ScanOutcome
    data class Error(val message: String) : ScanOutcome
}

/**
 * Options du scanner : une seule page (une feuille de livraison = un scan),
 * uniquement un rendu JPEG (pas besoin du PDF pour l'OCR), et le mode complet
 * qui active les filtres (niveaux de gris, amélioration automatique du
 * contraste) proposés à l'utilisateur dans l'écran de prévisualisation de
 * Google. L'import depuis la galerie est autorisé en secours (ex. si la
 * caméra pose problème un matin donné) ; mettre à false si on préfère forcer
 * la capture caméra uniquement.
 */
private fun buildScannerOptions(): GmsDocumentScannerOptions =
    GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(1)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()

/**
 * Prépare le lancement du flux de scan ML Kit et retourne une fonction à
 * appeler pour le démarrer. [onResult] est notifié une fois le flux terminé
 * (succès, annulation ou erreur).
 */
@Composable
fun rememberDocumentScannerLauncher(onResult: (ScanOutcome) -> Unit): () -> Unit {
    val activity = LocalActivity.current
    val currentOnResult by rememberUpdatedState(onResult)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val resultData = activityResult.data
        if (activityResult.resultCode == RESULT_OK && resultData != null) {
            val imageUri = runCatching {
                GmsDocumentScanningResult.fromActivityResultIntent(resultData)
                    ?.pages
                    ?.firstOrNull()
                    ?.imageUri
            }.getOrNull()

            if (imageUri != null) {
                currentOnResult(ScanOutcome.Success(imageUri))
            } else {
                currentOnResult(ScanOutcome.Error("Le scanner n'a retourné aucune image."))
            }
        } else if (activityResult.resultCode == RESULT_OK) {
            // RESULT_OK mais pas de données exploitables : on le traite comme une erreur
            // plutôt qu'une annulation silencieuse, pour ne pas masquer un vrai problème.
            currentOnResult(ScanOutcome.Error("Le scanner n'a retourné aucune donnée."))
        } else {
            currentOnResult(ScanOutcome.Cancelled)
        }
    }

    return remember(activity, launcher) {
        {
            if (activity == null) {
                currentOnResult(ScanOutcome.Error("Impossible de démarrer le scanner (contexte invalide)."))
            } else {
                val scanner: GmsDocumentScanner = GmsDocumentScanning.getClient(buildScannerOptions())
                scanner.getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener { exception ->
                        currentOnResult(
                            ScanOutcome.Error(
                                exception.message ?: "Échec du démarrage du scanner."
                            )
                        )
                    }
            }
        }
    }
}
