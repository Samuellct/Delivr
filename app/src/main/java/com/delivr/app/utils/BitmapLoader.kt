package com.delivr.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Résultat du chargement d'une image, en remplacement d'un simple `Bitmap?`.
 * Un `Bitmap?` ne permet pas de distinguer un chargement encore en cours
 * d'un échec de décodage — les deux valaient `null`, d'où un indicateur de
 * chargement qui tournait indéfiniment en cas d'échec réel.
 */
sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data class Loaded(val bitmap: Bitmap) : ImageLoadState
    data object Failed : ImageLoadState
}

/**
 * Calcule le facteur de sous-échantillonnage à appliquer pour qu'une image
 * de dimensions `options.outWidth`Ã—`options.outHeight` tienne dans
 * `reqWidth`Ã—`reqHeight`, par puissances de 2 (seules valeurs garanties
 * efficaces par [BitmapFactory.decodeFile]). Pattern documenté par Google :
 * https://developer.android.com/topic/performance/graphics/load-bitmap
 */
internal fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        var halfHeight = height / 2
        var halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/**
 * Charge un [Bitmap] depuis un fichier sur le disque (typiquement la copie
 * interne du scan, voir `camera/DocumentScanner.kt`), sous-échantillonné
 * pour tenir dans `reqWidth`Ã—`reqHeight` et corrigé de son orientation EXIF.
 *
 * Fonction `suspend` pure (testable sans Compose ni Android instrumenté,
 * hors lecture EXIF/décodage qui restent de vraies I/O disque) : la version
 * précédente faisait tout ce travail directement dans un composable.
 *
 * **Important** : ce sous-échantillonnage ne modifie que le [Bitmap] tenu en
 * mémoire pour l'aperçu ; le fichier source sur le disque n'est jamais
 * modifié ni réécrit, il reste en pleine résolution pour l'OCR (Phase 3).
 */
suspend fun loadDownsampledBitmap(path: String, reqWidth: Int, reqHeight: Int): ImageLoadState =
    withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return@withContext ImageLoadState.Failed
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
            }
            val bitmap = BitmapFactory.decodeFile(path, options)
                ?: return@withContext ImageLoadState.Failed

            val rotationDegrees = runCatching { ExifInterface(path).rotationDegrees }.getOrDefault(0)
            val oriented = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
            ImageLoadState.Loaded(oriented)
        }.getOrDefault(ImageLoadState.Failed)
    }

/**
 * Version composable de [loadDownsampledBitmap], pour affichage direct dans
 * un aperçu Compose. `reqWidth`/`reqHeight` par défaut couvrent largement un
 * écran de téléphone : l'aperçu n'a pas besoin de la pleine résolution du
 * scan.
 */
@Composable
fun rememberBitmapFromFile(
    path: String,
    reqWidth: Int = 1600,
    reqHeight: Int = 1600
): ImageLoadState {
    val state = produceState<ImageLoadState>(initialValue = ImageLoadState.Loading, path) {
        value = loadDownsampledBitmap(path, reqWidth, reqHeight)
    }
    return state.value
}
