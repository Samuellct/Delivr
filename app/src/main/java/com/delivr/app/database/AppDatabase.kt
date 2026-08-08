package com.delivr.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base Room de l'app — une seule tournée en cours à la fois (voir
 * [RoundEntity], [CottageEntity]).
 *
 * `exportSchema = false` : aucun schéma n'a jamais été publié, un seul
 * développeur sur ce projet. À rebasculer à `true` (avec
 * `ksp { arg("room.schemaLocation", ...) }` et un dossier `app/schemas/`
 * versionné) le jour où la version de schéma passe à 2 — voir `TODO_V1.md`,
 * Phase 5.
 */
@Database(
    entities = [RoundEntity::class, CottageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roundDao(): RoundDao

    companion object {
        private const val DATABASE_NAME = "delivr.db"

        /**
         * Une seule instance doit exister par process (Room le rappelle : en
         * créer plusieurs multiplie les connexions SQLite). Le cycle de vie
         * est tenu par [com.delivr.app.DelivrApplication] (`by lazy`), pas
         * ici : pas de singleton statique à double vérification, dont on n'a
         * pas besoin puisque l'Application est déjà un singleton fourni par
         * le système Android.
         *
         * Volontairement **pas** de `fallbackToDestructiveMigration` :
         * `Presentation.md` § Versioning exige qu'une nouvelle APK s'installe
         * sans perte de données. À la version 2, il faudra une vraie
         * `Migration` (voir `TODO_V1.md`, Phase 5).
         */
        fun create(context: Context): AppDatabase =
            Room
                .databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .build()
    }
}
