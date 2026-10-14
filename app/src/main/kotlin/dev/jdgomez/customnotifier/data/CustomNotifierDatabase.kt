package dev.jdgomez.customnotifier.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Schema version 3, exported to `app/schemas/`. A schema change needs a migration; data is never dropped. */
@Database(entities = [ProductEntity::class, AlertDeliveryEntity::class, CalendarChannelEntity::class], version = 3, exportSchema = true)
abstract class CustomNotifierDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    abstract fun alertDeliveryDao(): AlertDeliveryDao

    abstract fun calendarChannelDao(): CalendarChannelDao
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

/** Version 2 -> 3: adds the calendar channel setting; with no row the channel is off. */
val MIGRATION_2_3: Migration =
    object : Migration(2, 3) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `calendar_channel` (`id` INTEGER NOT NULL, `calendar_id` INTEGER, PRIMARY KEY(`id`))",
            )
        }
    }
