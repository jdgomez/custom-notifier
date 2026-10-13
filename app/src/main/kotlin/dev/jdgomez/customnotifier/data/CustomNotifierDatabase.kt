package dev.jdgomez.customnotifier.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Schema version 2, exported to `app/schemas/`. A schema change needs a migration; data is never dropped. */
@Database(entities = [ProductEntity::class, AlertDeliveryEntity::class], version = 2, exportSchema = true)
abstract class CustomNotifierDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    abstract fun alertDeliveryDao(): AlertDeliveryDao
}

/** Version 1 -> 2: adds the alert delivery records; no alert is recorded as delivered. */
val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `alert_deliveries` (`product_id` TEXT NOT NULL, PRIMARY KEY(`product_id`), " +
                    "FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
        }
    }
