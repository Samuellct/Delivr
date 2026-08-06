package com.delivr.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Charge un [Bitmap] depuis une [Uri] (typiquement le content:// renvoyé par
 * le scanner de documents) de façon asynchrone, pour affichage dans un
 * aperçu Compose. Pas de dépendance à une librairie de chargement d'image
 * (Coil, Glide, ...) : le volume d'images à afficher dans l'app reste très
 * faible (une feuille scannée à la fois), donc une lecture directe via
 * [android.content.ContentResolver] suffit et garde l'app légère.
 *
 * Retourne `null` tant que le chargement n'est pas terminé, ou en cas
 * d'échec de décodage.
 */
@Composable
fun rememberBitmapFromUri(uri: Uri): Bitmap? {
    val context = LocalContext.current
    val state = produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        }
    }
    return state.value
}
