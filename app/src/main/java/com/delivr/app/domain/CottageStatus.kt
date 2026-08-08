package com.delivr.app.domain

/**
 * Statut d'un cottage dans la tournée. Trois valeurs seulement — décision
 * actée le 2026-08-07 (`TODO_V1.md`, Phase 5.2) : « Reporté », prévu par
 * `Presentation.md` § Liste, est écarté de la V1 faute de geste dédié dans le
 * mode Livraison (Phase 6, qui ne gère qu'un tap simple pour Livré et un
 * appui long pour Annulé).
 *
 * Persisté par Room sous forme de `TEXT` contenant le *nom* de la constante
 * (`"A_FAIRE"`, …) via son convertisseur d'enum intégré — aucun
 * `@TypeConverter` à écrire (voir `database/RoundEntity.kt`,
 * `database/CottageEntity.kt`). Corollaire à ne pas oublier : renommer une
 * constante casserait la relecture des tournées déjà sauvegardées, ce serait
 * une migration.
 */
enum class CottageStatus {
    A_FAIRE,
    LIVRE,
    ANNULE,
}
