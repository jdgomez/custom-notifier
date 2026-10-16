package dev.jdgomez.customnotifier.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Schema version 5, exported to `app/schemas/`. A schema change needs a migration; data is never dropped. */
@Database(
    entities = [ProductEntity::class, AlertDeliveryEntity::class, CalendarChannelEntity::class, CalendarEventEntity::class],
    version = 5,
    exportSchema = true,
)
abstract class CustomNotifierDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    abstract fun alertDeliveryDao(): AlertDeliveryDao

    abstract fun calendarChannelDao(): CalendarChannelDao

    abstract fun calendarEventDao(): CalendarEventDao
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

/** Version 3 -> 4: adds the records of the alert events the app created; no event is known yet. */
val MIGRATION_3_4: Migration =
    object : Migration(3, 4) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `calendar_event` (`product_id` TEXT NOT NULL, `calendar_id` INTEGER NOT NULL, " +
                    "`event_id` INTEGER, `title_name` TEXT NOT NULL, `depletion_date` TEXT NOT NULL, `event_date` TEXT NOT NULL, " +
                    "PRIMARY KEY(`product_id`))",
            )
        }
    }

/** Version 4 -> 5: records the language an alert event was written in; '' differs from any tag, so each event is rewritten once. */
val MIGRATION_4_5: Migration =
    object : Migration(4, 5) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `calendar_event` ADD COLUMN `language` TEXT NOT NULL DEFAULT ''")
        }
    }
