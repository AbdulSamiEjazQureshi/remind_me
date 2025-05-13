package com.softrock.gesturesandwidgets.ui

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timeInMillis: Long,
    val title: String,
    val audioUri: String,
    val isReminded: Boolean = false
)

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("select * from reminders order by timeInMillis asc")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("select * from reminders where isReminded=:isReminded order by timeInMillis asc")
    fun getAllRemindersByStatus(isReminded: Boolean): Flow<List<ReminderEntity>>

    @Query("select * from reminders where id = :id")
    suspend fun getReminder(id: Int): ReminderEntity?

    @Query("update reminders set isReminded=:isReminded where id = :id")
    suspend fun updateReminderStatus(id: Int, isReminded: Boolean)
}

// Migrations
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE reminders ADD COLUMN isReminded INTEGER NOT NULL DEFAULT 0")
        database.execSQL("UPDATE reminders SET isReminded = 1 WHERE timeInMillis < strftime('%s','now') * 1000")
    }
}

@Database(
    entities = [ReminderEntity::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getAppDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}