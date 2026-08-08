package com.delivr.app

import android.app.Application
import com.delivr.app.database.AppDatabase
import com.delivr.app.repository.RoundRepository

/**
 * Porte les deux singletons de l'app : la base Room et le repository qui
 * l'expose. C'est **le** conteneur d'injection de dépendances du projet, et
 * il tient en quelques lignes — introduire Hilt/Koin/Dagger pour deux
 * ViewModels dans un module unique coûterait un plugin, du code généré et
 * un apprentissage, pour remplacer exactement ce que fait cette classe.
 * Décision assumée (Phase 5, voir `TODO_V1.md`).
 *
 * `by lazy` : la base n'est ouverte qu'au premier accès réel, donc pas au
 * démarrage à froid de l'app — l'écran d'accueil s'affiche sans attendre
 * l'ouverture de SQLite.
 */
class DelivrApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }

    val roundRepository: RoundRepository by lazy { RoundRepository(database.roundDao()) }
}
