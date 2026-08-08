package com.delivr.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CottageListTest {
    @Test
    fun `sortCottageNumbers ascending trie du plus petit au plus grand`() {
        assertEquals(listOf(3, 35, 143), sortCottageNumbers(listOf(143, 3, 35), SortDirection.ASCENDING))
    }

    @Test
    fun `sortCottageNumbers descending trie du plus grand au plus petit`() {
        assertEquals(listOf(143, 35, 3), sortCottageNumbers(listOf(3, 143, 35), SortDirection.DESCENDING))
    }

    @Test
    fun `addCottageNumber reinsere au bon rang, pas en fin de liste`() {
        val numbers = listOf(3, 78, 143)

        val result = addCottageNumber(numbers, 35, SortDirection.ASCENDING)

        assertEquals(listOf(3, 35, 78, 143), result)
    }

    @Test
    fun `addCottageNumber reinsere au bon rang en ordre decroissant`() {
        val numbers = listOf(143, 78, 3)

        val result = addCottageNumber(numbers, 35, SortDirection.DESCENDING)

        assertEquals(listOf(143, 78, 35, 3), result)
    }

    @Test
    fun `addCottageNumber d'un doublon laisse la liste inchangee`() {
        val numbers = listOf(3, 35, 78)

        val result = addCottageNumber(numbers, 35, SortDirection.ASCENDING)

        assertEquals(listOf(3, 35, 78), result)
    }

    @Test
    fun `removeCottageNumber retire le numero existant`() {
        val numbers = listOf(3, 35, 78)

        val result = removeCottageNumber(numbers, 35)

        assertEquals(listOf(3, 78), result)
    }

    @Test
    fun `removeCottageNumber sur un numero absent ne change rien`() {
        val numbers = listOf(3, 35, 78)

        val result = removeCottageNumber(numbers, 999)

        assertEquals(listOf(3, 35, 78), result)
    }

    @Test
    fun `updateCottageNumber remplace et retrie`() {
        val numbers = listOf(3, 35, 143)

        val result = updateCottageNumber(numbers, oldNumber = 35, newNumber = 999, direction = SortDirection.ASCENDING)

        assertEquals(listOf(3, 143, 999), result)
    }

    @Test
    fun `updateCottageNumber vers une valeur deja presente ailleurs laisse la liste inchangee`() {
        val numbers = listOf(3, 35, 143)

        val result = updateCottageNumber(numbers, oldNumber = 35, newNumber = 143, direction = SortDirection.ASCENDING)

        assertEquals(listOf(3, 35, 143), result)
    }

    @Test
    fun `updateCottageNumber sans changer la valeur ne la fait pas disparaitre`() {
        val numbers = listOf(3, 35, 143)

        val result = updateCottageNumber(numbers, oldNumber = 35, newNumber = 35, direction = SortDirection.ASCENDING)

        assertEquals(listOf(3, 35, 143), result)
    }
}
