package com.delivr.app.domain

/** Sens de la tournée (`Presentation.md` § Choix du sens). */
enum class SortDirection {
    ASCENDING,
    DESCENDING,
}

/** Trie [numbers] selon [direction]. */
fun sortCottageNumbers(
    numbers: List<Int>,
    direction: SortDirection,
): List<Int> =
    when (direction) {
        SortDirection.ASCENDING -> numbers.sorted()
        SortDirection.DESCENDING -> numbers.sortedDescending()
    }

/**
 * Ajoute [newNumber] à [numbers] et retrie selon [direction] — « réinséré
 * automatiquement au bon rang » (`Presentation.md` § Validation), jamais en
 * fin de liste. Si [newNumber] est déjà présent, la liste est retournée
 * inchangée : l'écran de Validation bloque déjà la soumission d'un doublon
 * avant d'appeler cette fonction (voir `ValidationScreen.kt`), mais elle
 * reste sûre indépendamment de son appelant — cohérent avec le garde-fou
 * déjà en place dans `extractCottageNumbers` (Phase 3).
 */
fun addCottageNumber(
    numbers: List<Int>,
    newNumber: Int,
    direction: SortDirection,
): List<Int> = sortCottageNumbers((numbers + newNumber).distinct(), direction)

/** Supprime [numberToRemove] de [numbers]. Sans effet si absent. */
fun removeCottageNumber(
    numbers: List<Int>,
    numberToRemove: Int,
): List<Int> = numbers.filterNot { it == numberToRemove }

/**
 * Remplace [oldNumber] par [newNumber] et retrie selon [direction]. Si
 * [newNumber] existe déjà ailleurs dans la liste (et diffère de
 * [oldNumber] — modifier un numéro sans le changer n'est pas un doublon),
 * retourne [numbers] inchangée plutôt que de fusionner silencieusement les
 * deux entrées (même garde-fou que [addCottageNumber]).
 */
fun updateCottageNumber(
    numbers: List<Int>,
    oldNumber: Int,
    newNumber: Int,
    direction: SortDirection,
): List<Int> {
    if (newNumber != oldNumber && newNumber in numbers) return numbers
    return sortCottageNumbers(numbers.map { if (it == oldNumber) newNumber else it }, direction)
}
