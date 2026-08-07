package com.delivr.app.utils

import android.graphics.BitmapFactory
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [calculateInSampleSize] ne lit que les champs `outWidth`/`outHeight` d'un
 * [BitmapFactory.Options] déjà rempli par `inJustDecodeBounds` — aucun appel
 * réel de décodage n'est nécessaire pour la tester, donc pas besoin
 * d'instrumentation Android.
 */
class BitmapLoaderTest {
    private fun optionsOf(
        width: Int,
        height: Int,
    ): BitmapFactory.Options =
        BitmapFactory.Options().apply {
            outWidth = width
            outHeight = height
        }

    @Test
    fun `image deja plus petite que la cible n'est pas sous-echantillonnee`() {
        val options = optionsOf(width = 800, height = 600)

        val sampleSize = calculateInSampleSize(options, reqWidth = 1600, reqHeight = 1600)

        assertEquals(1, sampleSize)
    }

    @Test
    fun `image deux fois trop grande est reduite d'un facteur 2`() {
        // Feuille A4 scannée à haute résolution, aperçu cible ~1600x1600.
        val options = optionsOf(width = 3200, height = 3200)

        val sampleSize = calculateInSampleSize(options, reqWidth = 1600, reqHeight = 1600)

        assertEquals(2, sampleSize)
    }

    @Test
    fun `image tres grande est reduite par une puissance de 2 superieure`() {
        val options = optionsOf(width = 6400, height = 6400)

        val sampleSize = calculateInSampleSize(options, reqWidth = 1600, reqHeight = 1600)

        assertEquals(4, sampleSize)
    }
}
