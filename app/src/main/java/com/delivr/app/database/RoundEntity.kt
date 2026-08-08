package com.delivr.app.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.delivr.app.domain.SortDirection

/**
 * Métadonnées de la tournée en cours — table à **une seule ligne** (voir
 * [SINGLE_ROUND_ID]). `Presentation.md` § Hors périmètre exclut tout
 * historique en V1 : il n'y a jamais qu'une tournée à la fois, donc pas
 * besoin d'un identifiant généré ni d'une table à plusieurs lignes.
 *
 * [sortDirection]/[createdAt] sont des propriétés de la tournée, pas d'un
 * cottage particulier — les séparer de [CottageEntity] rend impossible
 * l'incohérence qu'autoriserait leur duplication sur chaque ligne cottage
 * (deux lignes avec un sens différent, par exemple).
 */
@Entity(tableName = "round")
data class RoundEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLE_ROUND_ID,
    @ColumnInfo(name = "sort_direction")
    val sortDirection: SortDirection,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
) {
    companion object {
        /** Table à ligne unique : la V1 n'a pas d'historique de tournées. */
        const val SINGLE_ROUND_ID = 1
    }
}
