package com.delivr.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryProgressTest {
    private fun round(vararg statuses: CottageStatus): List<Cottage> =
        statuses.mapIndexed { i, status -> Cottage(number = (i + 1) * 10, status = status) }

    @Test
    fun `currentCottageIndex sur une tournee fraiche rend 0`() {
        assertEquals(0, currentCottageIndex(round(CottageStatus.A_FAIRE, CottageStatus.A_FAIRE, CottageStatus.A_FAIRE)))
    }

    @Test
    fun `currentCottageIndex apres un premier cottage livre rend 1`() {
        assertEquals(1, currentCottageIndex(round(CottageStatus.LIVRE, CottageStatus.A_FAIRE, CottageStatus.A_FAIRE)))
    }

    @Test
    fun `currentCottageIndex avec livre puis annule rend 2 (annule compte comme traite)`() {
        assertEquals(2, currentCottageIndex(round(CottageStatus.LIVRE, CottageStatus.ANNULE, CottageStatus.A_FAIRE)))
    }

    @Test
    fun `currentCottageIndex quand tout est traite rend la taille de la liste`() {
        assertEquals(3, currentCottageIndex(round(CottageStatus.LIVRE, CottageStatus.ANNULE, CottageStatus.LIVRE)))
    }

    @Test
    fun `currentCottageIndex sur une liste vide rend 0 sans planter`() {
        assertEquals(0, currentCottageIndex(emptyList()))
    }

    @Test
    fun `currentCottageIndex sur des statuts a trous rend le premier a faire`() {
        assertEquals(0, currentCottageIndex(round(CottageStatus.A_FAIRE, CottageStatus.LIVRE, CottageStatus.A_FAIRE)))
    }

    @Test
    fun `markCurrentCottage LIVRE change uniquement le cottage courant`() {
        val cottages = round(CottageStatus.A_FAIRE, CottageStatus.A_FAIRE, CottageStatus.A_FAIRE)

        val result = markCurrentCottage(cottages, CottageStatus.LIVRE)

        assertEquals(CottageStatus.LIVRE, result[0].status)
        assertEquals(CottageStatus.A_FAIRE, result[1].status)
        assertEquals(CottageStatus.A_FAIRE, result[2].status)
        assertEquals(1, currentCottageIndex(result))
    }

    @Test
    fun `markCurrentCottage ANNULE change uniquement le cottage courant`() {
        val cottages = round(CottageStatus.A_FAIRE, CottageStatus.A_FAIRE)

        val result = markCurrentCottage(cottages, CottageStatus.ANNULE)

        assertEquals(CottageStatus.ANNULE, result[0].status)
        assertEquals(1, currentCottageIndex(result))
    }

    @Test
    fun `markCurrentCottage sur une tournee deja terminee laisse la liste inchangee`() {
        val cottages = round(CottageStatus.LIVRE, CottageStatus.ANNULE)

        val result = markCurrentCottage(cottages, CottageStatus.LIVRE)

        assertEquals(cottages, result)
    }

    @Test
    fun `markCurrentCottage sur une liste vide rend une liste vide`() {
        assertEquals(emptyList<Cottage>(), markCurrentCottage(emptyList(), CottageStatus.LIVRE))
    }

    @Test
    fun `markCurrentCottage ne mute pas la liste d'entree`() {
        val cottages = round(CottageStatus.A_FAIRE)
        val original = cottages.toList()

        markCurrentCottage(cottages, CottageStatus.LIVRE)

        assertEquals(original, cottages)
    }

    @Test
    fun `goBackToPreviousCottage depuis l'index 2 repasse l'index 1 a A_FAIRE`() {
        val cottages = round(CottageStatus.LIVRE, CottageStatus.ANNULE, CottageStatus.A_FAIRE)

        val result = goBackToPreviousCottage(cottages)

        assertEquals(CottageStatus.LIVRE, result[0].status)
        assertEquals(CottageStatus.A_FAIRE, result[1].status)
        assertEquals(1, currentCottageIndex(result))
    }

    @Test
    fun `goBackToPreviousCottage sur le premier cottage ne change rien`() {
        val cottages = round(CottageStatus.A_FAIRE, CottageStatus.A_FAIRE)

        val result = goBackToPreviousCottage(cottages)

        assertEquals(cottages, result)
    }

    @Test
    fun `goBackToPreviousCottage depuis l'etat termine repasse le dernier cottage a A_FAIRE`() {
        val cottages = round(CottageStatus.LIVRE, CottageStatus.ANNULE, CottageStatus.LIVRE)

        val result = goBackToPreviousCottage(cottages)

        assertEquals(CottageStatus.A_FAIRE, result[2].status)
        assertEquals(2, currentCottageIndex(result))
    }

    @Test
    fun `goBackToPreviousCottage sur une liste vide ne change rien`() {
        assertEquals(emptyList<Cottage>(), goBackToPreviousCottage(emptyList()))
    }

    @Test
    fun `marquer livre puis reculer restitue la liste d'origine`() {
        val cottages = round(CottageStatus.A_FAIRE, CottageStatus.A_FAIRE)

        val result = goBackToPreviousCottage(markCurrentCottage(cottages, CottageStatus.LIVRE))

        assertEquals(cottages, result)
    }
}
