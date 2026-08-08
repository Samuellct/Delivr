package com.delivr.app.domain

import kotlin.math.abs
import kotlin.math.max

/**
 * Représentation pure d'un élément de texte reconnu par l'OCR — voir
 * `ocr/TextRecognizer.kt`, qui construit ces instances à partir des
 * `Text.Element` ML Kit. Volontairement découplée du SDK ML Kit (`Text.Element`
 * n'est pas instanciable directement dans un test, c'est une classe finale
 * fournie par la librairie) pour que ce fichier reste un module Kotlin pur,
 * testable sans Android ni Play Services.
 *
 * [left]/[top] sont les coordonnées du coin supérieur gauche de la boîte
 * englobante de l'élément, en pixels de l'image source — seules coordonnées
 * dont [extractCottageNumbers] a besoin (voir sa décision de conception).
 */
data class RecognizedElement(
    val text: String,
    val left: Int,
    val top: Int,
)

/**
 * Résultat de l'extraction des numéros de cottage à partir d'une feuille de
 * livraison reconnue par l'OCR.
 */
sealed interface ExtractionResult {
    /** [cottageNumbers] est trié par ordre croissant et sans doublon. */
    data class Success(
        val cottageNumbers: List<Int>,
    ) : ExtractionResult

    /** Aucun élément ne contient le texte de l'en-tête « Cott ». */
    data object HeaderNotFound : ExtractionResult

    /** L'en-tête a été trouvé, mais aucune valeur exploitable en dessous. */
    data object NoNumbersFound : ExtractionResult
}

private const val HEADER_TEXT = "cott"
private const val COLUMN_TOLERANCE_FRACTION = 0.05
private const val MIN_TOLERANCE_PX = 20
private const val MIN_COTTAGE_NUMBER = 1
private const val MAX_COTTAGE_NUMBER = 999

/**
 * Isole la colonne « Cott » et en extrait les numéros de cottage, triés et
 * dédoublonnés — stratégie actée en Phase 2.4 après observation d'une vraie
 * sortie ML Kit sur des feuilles réelles :
 *
 * 1. L'en-tête « Cott » est repéré par correspondance textuelle souple
 *    (`contains`, insensible à la casse) plutôt qu'une égalité stricte : sur
 *    une des 4 photos réelles testées, ML Kit avait fusionné l'en-tête avec
 *    la cellule voisine (`"CottNo."`) — une égalité stricte l'aurait manqué.
 *    S'il y a plusieurs correspondances (peu probable mais possible), on
 *    retient celle avec le `top` le plus petit : l'en-tête est en haut du
 *    tableau, pas une coïncidence plus bas.
 * 2. Le bord **gauche** de cet élément sert d'ancre de colonne — jamais sa
 *    largeur ni son bord droit, qui peuvent être faussés par la même fusion.
 * 3. La marge de tolérance est **proportionnelle à la largeur de l'image**
 *    ([imageWidthPx]), pas un nombre de pixels fixe : l'écart observé en
 *    Phase 2.4 entre l'en-tête et sa valeur était d'environ 24px sur une
 *    image large d'environ 2765px (~0,9 %). [COLUMN_TOLERANCE_FRACTION] à
 *    5 %, avec un plancher [MIN_TOLERANCE_PX], absorbe une marge d'erreur
 *    réaliste sans déborder sur la colonne voisine.
 * 4. On ne raisonne jamais au niveau d'une « ligne » ML Kit : sur la même
 *    photo, ML Kit avait fusionné plusieurs cellules d'une même ligne du
 *    tableau (Cott + n° de réservation + date) en un seul bloc de texte —
 *    seuls les éléments individuels gardent une boîte précise par cellule.
 * 5. Chaque texte candidat est nettoyé de la ponctuation qui peut s'y coller
 *    (ex. `"607."` → `"607"`, artefact observé en Phase 2.4) puis analysé en
 *    entier ; seules les valeurs `1..999` (borne de `Presentation.md`) sont
 *    retenues — un garde-fou supplémentaire si l'ancrage par position laisse
 *    passer un élément d'une colonne voisine (n° de réservation, durée...).
 */
fun extractCottageNumbers(
    elements: List<RecognizedElement>,
    imageWidthPx: Int,
): ExtractionResult {
    val header =
        elements
            .filter { it.text.contains(HEADER_TEXT, ignoreCase = true) }
            .minByOrNull { it.top }
            ?: return ExtractionResult.HeaderNotFound

    val tolerancePx = max((imageWidthPx * COLUMN_TOLERANCE_FRACTION).toInt(), MIN_TOLERANCE_PX)

    val cottageNumbers =
        elements
            .asSequence()
            .filter { it.top > header.top }
            .filter { abs(it.left - header.left) <= tolerancePx }
            .mapNotNull { it.text.filter(Char::isDigit).toIntOrNull() }
            .filter { it in MIN_COTTAGE_NUMBER..MAX_COTTAGE_NUMBER }
            .distinct()
            .sorted()
            .toList()

    return if (cottageNumbers.isEmpty()) {
        ExtractionResult.NoNumbersFound
    } else {
        ExtractionResult.Success(cottageNumbers)
    }
}

/** Affichage sur 3 chiffres attendu par `Presentation.md` (ex. `035`, `078`). */
fun formatCottageNumber(number: Int): String = "%03d".format(number)
