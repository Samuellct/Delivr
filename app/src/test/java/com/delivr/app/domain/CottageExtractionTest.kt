package com.delivr.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [extractCottageNumbers] est une fonction pure : les [RecognizedElement] sont
 * construits à la main ici pour reproduire des sorties ML Kit réelles
 * observées en Phase 2.4, sans dépendre d'Android ni d'un appareil.
 */
class CottageExtractionTest {
    // Feuille de 1000px de large : tolérance = max(1000 * 0.05, 20) = 50px.
    private val header = RecognizedElement(text = "Cott", left = 500, top = 100)

    @Test
    fun `exemple exact de Presentation md, colonne propre`() {
        // 607, 143, 35, 960, 78, 558 -> 035, 078, 143, 558, 607, 960
        val elements =
            listOf(
                header,
                RecognizedElement("607.", left = 510, top = 200),
                RecognizedElement("143-", left = 495, top = 250),
                RecognizedElement("35", left = 505, top = 300),
                RecognizedElement("960", left = 500, top = 350),
                RecognizedElement("78,", left = 520, top = 400),
                RecognizedElement("558", left = 490, top = 450),
            )

        val result = extractCottageNumbers(elements, imageWidthPx = 1000)

        assertEquals(ExtractionResult.Success(listOf(35, 78, 143, 558, 607, 960)), result)
        assertEquals(
            listOf("035", "078", "143", "558", "607", "960"),
            (result as ExtractionResult.Success).cottageNumbers.map(::formatCottageNumber),
        )
    }

    @Test
    fun `en-tete absent renvoie HeaderNotFound`() {
        val elements = listOf(RecognizedElement("607", left = 510, top = 200))

        val result = extractCottageNumbers(elements, imageWidthPx = 1000)

        assertEquals(ExtractionResult.HeaderNotFound, result)
    }

    @Test
    fun `en-tete present mais aucune valeur exploitable renvoie NoNumbersFound`() {
        val elements = listOf(header, RecognizedElement("Nom", left = 500, top = 150))

        val result = extractCottageNumbers(elements, imageWidthPx = 1000)

        assertEquals(ExtractionResult.NoNumbersFound, result)
    }

    @Test
    fun `element hors marge de tolerance est ignore meme si le nombre est valide`() {
        val elements =
            listOf(
                header,
                // No. Res. voisin, largement hors des 50px de tolérance.
                RecognizedElement("700", left = 700, top = 200),
            )

        val result = extractCottageNumbers(elements, imageWidthPx = 1000)

        assertEquals(ExtractionResult.NoNumbersFound, result)
    }

    @Test
    fun `element pile a la limite de la marge est conserve`() {
        val elements =
            listOf(
                header,
                RecognizedElement("42", left = header.left + 50, top = 200),
            )

        val result = extractCottageNumbers(elements, imageWidthPx = 1000)

        assertEquals(ExtractionResult.Success(listOf(42)), result)
    }

    @Test
    fun `element juste hors limite de la marge est rejete`() {
        val elements =
            listOf(
                header,
                RecognizedElement("42", left = header.left + 51, top = 200),
            )

        val result = extractCottageNumbers(elements, imageWidthPx = 1000)

        assertEquals(ExtractionResult.NoNumbersFound, result)
    }

    @Test
    fun `valeurs hors de la plage 1 a 999 sont exclues`() {
        val elements =
            listOf(
                header,
                RecognizedElement("0", left = 500, top = 200),
                RecognizedElement("1000", left = 500, top = 250),
                RecognizedElement("500", left = 500, top = 300),
            )

        val result = extractCottageNumbers(elements, imageWidthPx = 1000)

        assertEquals(ExtractionResult.Success(listOf(500)), result)
    }

    @Test
    fun `doublons de detection sont deduplique`() {
        val elements =
            listOf(
                header,
                RecognizedElement("42", left = 500, top = 200),
                RecognizedElement("42", left = 500, top = 205),
            )

        val result = extractCottageNumbers(elements, imageWidthPx = 1000)

        assertEquals(ExtractionResult.Success(listOf(42)), result)
    }

    @Test
    fun `en-tete fusionne avec la cellule voisine reste detecte`() {
        // Observé en Phase 2.4 sur une photo réelle : "CottNo. Res. Début arr..."
        val fusedHeader = RecognizedElement(text = "CottNo.", left = 500, top = 100)
        val elements =
            listOf(
                fusedHeader,
                RecognizedElement("181", left = 505, top = 200),
            )

        val result = extractCottageNumbers(elements, imageWidthPx = 1000)

        assertEquals(ExtractionResult.Success(listOf(181)), result)
    }

    @Test
    fun `element au-dessus de l'en-tete est ignore`() {
        val elements =
            listOf(
                header,
                RecognizedElement("42", left = 500, top = 50),
            )

        val result = extractCottageNumbers(elements, imageWidthPx = 1000)

        assertEquals(ExtractionResult.NoNumbersFound, result)
    }

    @Test
    fun `formatCottageNumber affiche toujours 3 chiffres`() {
        assertEquals("005", formatCottageNumber(5))
        assertEquals("078", formatCottageNumber(78))
        assertEquals("999", formatCottageNumber(999))
    }

    @Test
    fun `resultat est trie meme si les elements arrivent dans le desordre`() {
        val elements =
            listOf(
                header,
                RecognizedElement("960", left = 500, top = 200),
                RecognizedElement("35", left = 500, top = 250),
                RecognizedElement("558", left = 500, top = 300),
            )

        val result = extractCottageNumbers(elements, imageWidthPx = 1000) as ExtractionResult.Success

        assertTrue(result.cottageNumbers == result.cottageNumbers.sorted())
    }
}
