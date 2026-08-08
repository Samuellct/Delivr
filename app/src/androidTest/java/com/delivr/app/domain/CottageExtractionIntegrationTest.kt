package com.delivr.app.domain

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.delivr.app.ocr.recognizeText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test bout en bout (Phase 3.6) : fait tourner le vrai pipeline
 * `ocr.recognizeText` + `domain.extractCottageNumbers` sur la fixture
 * synthétique versionnée (`assets/fixtures/feuille_synthetique.png`, Phase
 * 2.2), via de vrais appels ML Kit — nécessite un appareil connecté (Play
 * Services), comme la sonde de la Phase 2. Ne tourne pas dans `ci.yml`
 * (aucun appareil en CI), cohérent avec `ScanScreenTest`.
 */
@RunWith(AndroidJUnit4::class)
class CottageExtractionIntegrationTest {
    @Test
    fun extraction_sur_la_fixture_synthetique_retourne_les_20_numeros_attendus() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val bitmap =
            assets.open("fixtures/feuille_synthetique.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }

        val elements = runBlocking { recognizeText(bitmap) }
        val result = extractCottageNumbers(elements, imageWidthPx = bitmap.width)

        // Numéros posés dans la fixture en Phase 2.2 (voir make_fixture.py),
        // triés par ordre croissant.
        val expected =
            listOf(3, 6, 35, 48, 63, 78, 92, 143, 150, 234, 267, 421, 512, 558, 607, 700, 815, 881, 960, 999)

        assertEquals(ExtractionResult.Success(expected), result)
    }
}
