package org.bike4city.ciclofficinabot.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class, DiagnosisReportEntity::class],
    version = 3,
    exportSchema = false
)
abstract class Bike4CityDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS diagnosis_reports (
                        sessionId TEXT NOT NULL,
                        bikeSummary TEXT NOT NULL,
                        initialProblem TEXT NOT NULL,
                        checksPerformed TEXT NOT NULL,
                        possibleCauses TEXT NOT NULL,
                        safetyLevel TEXT NOT NULL,
                        outcome TEXT NOT NULL,
                        usageRecommendation TEXT NOT NULL,
                        generatedAt INTEGER NOT NULL,
                        PRIMARY KEY(sessionId),
                        FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE chat_sessions ADD COLUMN remoteSyncStatus TEXT NOT NULL DEFAULT 'LOCAL_ONLY'"
                )
                db.execSQL(
                    "ALTER TABLE chat_sessions ADD COLUMN remoteSyncedAt INTEGER DEFAULT NULL"
                )
            }
        }

        @Volatile private var instance: Bike4CityDatabase? = null
        fun get(context: Context): Bike4CityDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, Bike4CityDatabase::class.java, "bike4city.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
