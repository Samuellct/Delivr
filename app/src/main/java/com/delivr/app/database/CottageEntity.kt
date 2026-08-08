package com.delivr.app.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.delivr.app.domain.CottageStatus

/**
 * Un cottage de la tournée en cours. [number] sert de clé primaire : sans
 * historique en V1 (`Presentation.md` § Hors périmètre), un numéro identifie
 * un cottage sans ambiguïté — `domain/CottageList.kt` garantit déjà l'absence
 * de doublon en amont — donc pas d'identifiant auto-généré inutile.
 *
 * [position] est stockée explicitement plutôt que déduite d'un `ORDER BY
 * number`, car le sens de tournée peut être décroissant (voir
 * [com.delivr.app.domain.SortDirection]), et parce que le mode Livraison
 * (Phase 6) affichera une position (« 6 / 24 »).
 */
@Entity(tableName = "cottage")
data class CottageEntity(
    @PrimaryKey
    @ColumnInfo(name = "number")
    val number: Int,
    @ColumnInfo(name = "position")
    val position: Int,
    @ColumnInfo(name = "status")
    val status: CottageStatus = CottageStatus.A_FAIRE,
)
