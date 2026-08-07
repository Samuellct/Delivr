package com.delivr.app.camera

import android.app.Activity.RESULT_OK
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    /**
     * [imagePath] est un chemin de fichier dans le stockage privé de l'app
     * (pas l'URI `content://` d'origine de ML Kit) : celle-ci n'a qu'une
     * autorisation de lecture transitoire, non garantie après recréation du
     * process. Voir [copyToInternalStorage].
     */
    data class Success(val imagePath: String) : ScanOutcome
    data object Cancelled : ScanOutcome
    data class Error(val error: ScanError) : ScanOutcome
}

private const val TAG = "DocumentScanner"

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
 * Copie l'image scannée depuis l'URI `content://` (temporaire, propriété de
 * ML Kit) vers un fichier privé de l'app. Le fichier précédent est écrasé :
 * une seule tournée à la fois est en cours, pas besoin d'historique ici.
 * Retourne `null` en cas d'échec de copie (source illisible, disque plein...).
 */
private fun copyToInternalStorage(context: Context, sourceUri: Uri): String? = runCatching {
    val internalFile = File(context.filesDir, "scans/current_scan.jpg")
    internalFile.parentFile?.mkdirs()
    val stream = context.contentResolver.openInputStream(sourceUri) ?: return null
    stream.use { input ->
        internalFile.outputStream().use { output -> input.copyTo(output) }
    }
    internalFile.absolutePath
}.getOrNull()

/**
 * Prépare le lancement du flux de scan ML Kit et retourne une fonction à
 * appeler pour le démarrer. [onResult] est notifié une fois le flux terminé
 * (succès, annulation ou erreur).
 */
@Composable
fun rememberDocumentScannerLauncher(onResult: (ScanOutcome) -> Unit): () -> Unit {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
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
                // Copie hors du thread principal : lecture/écriture disque pour
                // une image A4 pleine résolution, pas une opération instantanée.
                coroutineScope.launch {
                    val imagePath = withContext(Dispatchers.IO) {
                        copyToInternalStorage(context, imageUri)
                    }
                    if (imagePath != null) {
                        currentOnResult(ScanOutcome.Success(imagePath))
                    } else {
                        currentOnResult(ScanOutcome.Error(ScanError.NoImageReturned))
                    }
                }
            } else {
                currentOnResult(ScanOutcome.Error(ScanError.NoImageReturned))
            }
        } else if (activityResult.resultCode == RESULT_OK) {
            // RESULT_OK mais pas de données exploitables : on le traite comme une erreur
            // plutôt qu'une annulation silencieuse, pour ne pas masquer un vrai problème.
            currentOnResult(ScanOutcome.Error(ScanError.NoDataReturned))
        } else {
            currentOnResult(ScanOutcome.Cancelled)
        }
    }

    return remember(activity, launcher) {
        {
            if (activity == null) {
                currentOnResult(ScanOutcome.Error(ScanError.InvalidContext))
            } else {
                val scanner: GmsDocumentScanner = GmsDocumentScanning.getClient(buildScannerOptions())
                scanner.getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener { exception ->
                        // Le message brut de l'exception ML Kit est loggé pour le
                        // diagnostic, mais jamais affiché tel quel à l'utilisateur
                        // (voir ScanError.LaunchFailed et son résolveur dans ScanScreen.kt).
                        Log.w(TAG, "Échec du démarrage du scanner", exception)
                        currentOnResult(
                            ScanOutcome.Error(ScanError.LaunchFailed(exception.message))
                        )
                    }
            }
        }
    }
}
