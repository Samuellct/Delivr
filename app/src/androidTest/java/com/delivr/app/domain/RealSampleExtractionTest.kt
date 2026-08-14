package com.delivr.app.domain

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.delivr.app.ocr.recognizeText
import com.delivr.app.utils.loadFullResolutionBitmap
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val TAG = "OCRAudit"

/**
 * Mesure du taux de détection réel du pipeline OCR (`recognizeText` +
 * `extractCottageNumbers`) sur les 4 vraies photos de feuilles de livraison
 * (`assets/real_samples/`, gitignorées) — audit AUD-003 de `audit_after_v1.md`.
 *
 * Contrairement à `CottageExtractionIntegrationTest` (fixture synthétique
 * parfaite), ce test ne fait aucune assertion stricte : c'est un test de
 * *mesure*, pas de non-régression. Les comptes de référence (nombre de
 * cottages valides par photo, hors lignes barrées) ont été fournis
 * manuellement par l'utilisateur en comparant à la feuille papier ; seul le
 * total est connu, pas la liste exacte numéro par numéro, donc ce test
 * calcule un rappel approximatif (détectés / attendus), pas une vraie
 * précision/rappel élément par élément.
 *
 * Copie chaque asset vers un fichier temporaire avant de le charger via
 * [loadFullResolutionBitmap] (qui prend un chemin de fichier, pas un flux
 * d'assets) — reproduit fidèlement le pipeline réel, y compris la correction
 * de rotation EXIF, cruciale pour `sheet-a-angle-checkmarks.jpg`.
 */
@RunWith(AndroidJUnit4::class)
class RealSampleExtractionTest {
    private val samples =
        listOf(
            "sheet-a-angle-checkmarks.jpg" to 23,
            "sheet-b-clean-straight.jpg" to 24,
            "sheet-c-scribbled-cancelled-row.jpg" to 20,
            "sheet-d-handwritten-added-row.jpg" to 23,
        )

    @Test
    fun mesure_le_taux_de_detection_sur_les_photos_reelles() =
        runBlocking {
            val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
            val testAssets = InstrumentationRegistry.getInstrumentation().context.assets

            for ((filename, expectedCount) in samples) {
                val tempFile = File(targetContext.cacheDir, filename)
                testAssets.open("real_samples/$filename").use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }

                val bitmap = loadFullResolutionBitmap(tempFile.absolutePath)
                if (bitmap == null) {
                    Log.i(TAG, "$filename : ECHEC DECODAGE BITMAP")
                    tempFile.delete()
                    continue
                }

                val elements = recognizeText(bitmap)
                val result = extractCottageNumbers(elements, imageWidthPx = bitmap.width)

                when (result) {
                    is ExtractionResult.Success -> {
                        val detected = result.cottageNumbers
                        val recall = detected.size * 100.0 / expectedCount
                        Log.i(
                            TAG,
                            "$filename attendu=$expectedCount detecte=${detected.size} " +
                                "rappel=${"%.1f".format(recall)}%% liste=$detected",
                        )
                    }
                    ExtractionResult.HeaderNotFound -> Log.i(TAG, "$filename : EN-TETE COTT NON TROUVE")
                    ExtractionResult.NoNumbersFound -> Log.i(TAG, "$filename : AUCUN NUMERO TROUVE")
                }

                tempFile.delete()
            }
        }
}
