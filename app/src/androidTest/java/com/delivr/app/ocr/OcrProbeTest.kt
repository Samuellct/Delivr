package com.delivr.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Sonde exploratoire de la Phase 2 (TODO_V1.md, étape 2.3) : fait tourner
 * ML Kit Text Recognition sur les échantillons réels
 * (`assets/real_samples/`, local uniquement, jamais commité) et la fixture
 * synthétique (`assets/fixtures/`), et écrit pour chacun un rapport texte
 * listant chaque bloc/ligne/élément reconnu avec sa position
 * ([android.graphics.Rect] `boundingBox`). C'est cette sortie réelle qui
 * permet de choisir la stratégie d'isolation de la colonne « Cott » en
 * Phase 2.4, plutôt que de la deviner.
 *
 * Test volontairement temporaire : supprimé une fois la décision 2.4
 * rédigée (voir TODO_V1.md). `Assume.assumeTrue` l'ignore silencieusement
 * si `real_samples/` est vide (aucune photo locale disponible), pour ne
 * jamais faire échouer une exécution sur un autre poste.
 */
@RunWith(AndroidJUnit4::class)
class OcrProbeTest {
    @Test
    fun sonde_toutes_les_images_de_test() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assets = instrumentation.context.assets
        val outputDir = ApplicationProvider.getApplicationContext<Context>().getExternalFilesDir(null)!!
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val images =
            assets.list("real_samples").orEmpty().map { "real_samples/$it" } +
                assets.list("fixtures").orEmpty().map { "fixtures/$it" }
        Assume.assumeTrue("Aucun échantillon local trouvé", images.isNotEmpty())

        for (assetPath in images) {
            val rotation = assets.open(assetPath).use { ExifInterface(it).rotationDegrees }
            val rawBitmap = assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
            val oriented =
                if (rotation != 0) {
                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                } else {
                    rawBitmap
                }

            val result =
                Tasks.await(
                    recognizer.process(InputImage.fromBitmap(oriented, 0)),
                    30,
                    TimeUnit.SECONDS,
                )

            val report =
                buildString {
                    appendLine("=== $assetPath (${oriented.width}x${oriented.height}) ===")
                    for (block in result.textBlocks) {
                        for (line in block.lines) {
                            appendLine("LINE  \"${line.text}\"  box=${line.boundingBox}")
                            for (element in line.elements) {
                                appendLine("  ELEM \"${element.text}\"  box=${element.boundingBox}")
                            }
                        }
                    }
                }
            File(outputDir, "${assetPath.replace("/", "_")}.txt").writeText(report)
        }
    }
}
