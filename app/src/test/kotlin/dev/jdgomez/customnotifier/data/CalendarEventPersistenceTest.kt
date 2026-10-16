package dev.jdgomez.customnotifier.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import dev.jdgomez.customnotifier.domain.AlertEvent
import dev.jdgomez.customnotifier.domain.CalendarChannel
import dev.jdgomez.customnotifier.domain.CalendarEventRecord
import dev.jdgomez.customnotifier.domain.CalendarId
import dev.jdgomez.customnotifier.domain.DepletionDate
import dev.jdgomez.customnotifier.domain.EventId
import dev.jdgomez.customnotifier.domain.LanguageTag
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.domain.ProductName
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CalendarEventPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var database: CustomNotifierDatabase? = null

    @After
    fun close() {
        database?.close()
    }

    private fun open(name: String?): CustomNotifierDatabase {
        val builder =
            if (name == null) {
                Room.inMemoryDatabaseBuilder<CustomNotifierDatabase>(context)
            } else {
                Room.databaseBuilder<CustomNotifierDatabase>(context, name)
            }
        return builder
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
            .also { database = it }
    }

    private val id = ProductId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val content =
        AlertEvent(ProductName("Coffee"), DepletionDate(LocalDate.parse("2026-03-11")), LocalDate.parse("2026-03-01"), LanguageTag("en"))

    @Test
    fun `stores, replaces, marks as gone and removes a record`() =
        runBlocking {
            val records = RoomCalendarEventRecords(open(null).calendarEventDao())
            assertTrue(records.all().isEmpty())

            val live = CalendarEventRecord(id, CalendarId(3), EventId(42), content)
            records.put(live)
            assertEquals(listOf(live), records.all())

            val moved = live.copy(content = content.copy(date = LocalDate.parse("2026-03-31")))
            records.put(moved)
            assertEquals(listOf(moved), records.all())

            val gone = moved.copy(eventId = null)
            records.put(gone)
            assertEquals(listOf(gone), records.all())

            records.remove(id)
            assertTrue(records.all().isEmpty())
        }

    @Test
    fun `a record outlives its product`() =
        runBlocking {
            val db = open(null)
            val records = RoomCalendarEventRecords(db.calendarEventDao())
            records.put(CalendarEventRecord(id, CalendarId(3), EventId(42), content))
            RoomProductRepository(db.productDao()).delete(id)
            assertEquals(1, records.all().size)
        }

    @Test
    fun `upgrade from schema 3 keeps the channel and starts with no records`() =
        runBlocking {
            val path = context.getDatabasePath("upgrade3.db")
            path.parentFile?.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                old.execSQL(SCHEMA_3_PRODUCTS)
                old.execSQL(SCHEMA_3_DELIVERIES)
                old.execSQL(
                    "CREATE TABLE IF NOT EXISTS `calendar_channel` (`id` INTEGER NOT NULL, `calendar_id` INTEGER, PRIMARY KEY(`id`))",
                )
                old.execSQL("INSERT INTO calendar_channel VALUES (0, 7)")
                old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                old.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, 'd1d24f688cc6eb4e7c023ae538053a05')")
                old.version = 3
            }

            val db = open("upgrade3.db")

            assertEquals(CalendarChannel.On(CalendarId(7)), RoomCalendarChannelSettings(db.calendarChannelDao()).observe().first())
            assertTrue(RoomCalendarEventRecords(db.calendarEventDao()).all().isEmpty())
        }

    @Test
    fun `upgrade from schema 4 keeps the records and marks them as written in no language`() =
        runBlocking {
            val path = context.getDatabasePath("upgrade4.db")
            path.parentFile?.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                old.execSQL(SCHEMA_3_PRODUCTS)
                old.execSQL(SCHEMA_3_DELIVERIES)
                old.execSQL(
                    "CREATE TABLE IF NOT EXISTS `calendar_channel` (`id` INTEGER NOT NULL, `calendar_id` INTEGER, PRIMARY KEY(`id`))",
                )
                old.execSQL(
                    "CREATE TABLE IF NOT EXISTS `calendar_event` (`product_id` TEXT NOT NULL, `calendar_id` INTEGER NOT NULL, " +
                        "`event_id` INTEGER, `title_name` TEXT NOT NULL, `depletion_date` TEXT NOT NULL, `event_date` TEXT NOT NULL, " +
                        "PRIMARY KEY(`product_id`))",
                )
                old.execSQL("INSERT INTO calendar_event VALUES ('${id.value}', 3, 42, 'Coffee', '2026-03-11', '2026-03-01')")
                old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                old.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, 'f31591ddb983ae8fe04c3855cca2d710')")
                old.version = 4
            }

            val records = RoomCalendarEventRecords(open("upgrade4.db").calendarEventDao()).all()

            assertEquals(listOf(CalendarEventRecord(id, CalendarId(3), EventId(42), content.copy(language = LanguageTag("")))), records)
        }

    private companion object {
        /** The tables exactly as exported in `app/schemas/.../3.json`. */
        const val SCHEMA_3_PRODUCTS =
            "CREATE TABLE IF NOT EXISTS `products` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `unit_label` TEXT NOT NULL, " +
                "`package_size` INTEGER NOT NULL, `rate_units` INTEGER NOT NULL, `rate_days` INTEGER NOT NULL, " +
                "`stock_numerator` TEXT NOT NULL, `stock_denominator` TEXT NOT NULL, `recorded_at_epoch_second` INTEGER NOT NULL, " +
                "`recorded_at_nano` INTEGER NOT NULL, `lead_time_days` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        const val SCHEMA_3_DELIVERIES =
            "CREATE TABLE IF NOT EXISTS `alert_deliveries` (`product_id` TEXT NOT NULL, PRIMARY KEY(`product_id`), " +
                "FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
    }
}
