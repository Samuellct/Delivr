package com.delivr.app.ocr

import android.graphics.Bitmap
import com.delivr.app.domain.RecognizedElement
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Fait tourner ML Kit Text Recognition (hors ligne, modèle latin) sur
 * [bitmap] et aplatit son résultat en une liste plate de [RecognizedElement],
 * un par mot reconnu.
 *
 * Aplati au niveau **élément** — jamais au niveau ligne (`Text.Line`) —
 * conformément à la décision de la Phase 2.4 : ML Kit fusionne parfois
 * plusieurs cellules d'une même ligne du tableau (Cott + n° de réservation +
 * date) en un seul `Text.Line`, mais les `Text.Element` individuels
 * conservent une boîte englobante précise par cellule.
 *
 * Isolé de l'UI (aucun import Compose) : seul [Bitmap] (Android) et le SDK
 * ML Kit apparaissent ici, conformément à la séparation ocr/domain
 * documentée dans `CLAUDE.md`.
 */
suspend fun recognizeText(bitmap: Bitmap): List<RecognizedElement> {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()

    return result.textBlocks.flatMap { block ->
        block.lines.flatMap { line ->
            line.elements.mapNotNull { element ->
                val box = element.boundingBox ?: return@mapNotNull null
                RecognizedElement(text = element.text, left = box.left, top = box.top)
            }
        }
    }
}
