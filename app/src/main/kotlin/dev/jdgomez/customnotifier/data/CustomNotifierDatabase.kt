package dev.jdgomez.customnotifier.data

import androidx.room3.Database
import androidx.room3.RoomDatabase

/** Schema version 1, exported to `app/schemas/`. A schema change needs a migration; data is never dropped. */
@Database(entities = [ProductEntity::class], version = 1, exportSchema = true)
abstract class CustomNotifierDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
