package dev.jdgomez.customnotifier.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import dev.jdgomez.customnotifier.domain.CalendarChannel
import dev.jdgomez.customnotifier.domain.CalendarId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CalendarChannelPersistenceTest {
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
            .also { database = it }
    }

    @Test
    fun `the channel is off when nothing is stored`() =
        runBlocking {
            val settings = RoomCalendarChannelSettings(open(null).calendarChannelDao())
            assertEquals(CalendarChannel.Off, settings.observe().first())
        }

    @Test
    fun `stores the chosen calendar, replaces it and turns off`() =
        runBlocking {
            val settings = RoomCalendarChannelSettings(open(null).calendarChannelDao())

            settings.set(CalendarChannel.On(CalendarId(7)))
            assertEquals(CalendarChannel.On(CalendarId(7)), settings.observe().first())
            settings.set(CalendarChannel.On(CalendarId(9)))
            assertEquals(CalendarChannel.On(CalendarId(9)), settings.observe().first())
            settings.set(CalendarChannel.Off)
            assertEquals(CalendarChannel.Off, settings.observe().first())
        }

    @Test
    fun `the channel survives reopening the database`() =
        runBlocking {
            val first = open("channel.db")
            RoomCalendarChannelSettings(first.calendarChannelDao()).set(CalendarChannel.On(CalendarId(3)))
            first.close()

            val second = RoomCalendarChannelSettings(open("channel.db").calendarChannelDao())
            assertEquals(CalendarChannel.On(CalendarId(3)), second.observe().first())
        }

    @Test
    fun `upgrade from schema 2 keeps the products and starts with the channel off`() =
        runBlocking {
            val path = context.getDatabasePath("upgrade2.db")
            path.parentFile?.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                old.execSQL(SCHEMA_2_PRODUCTS)
                old.execSQL(SCHEMA_2_DELIVERIES)
                old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                old.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, '9038a19fab6e35a0045c8461189153ee')")
                old.execSQL(
                    "INSERT INTO products VALUES ('00000000-0000-0000-0000-000000000001', 'Soap', 'bar', 1, 1, 1, '3', '1', 1, 0, 2)",
                )
                old.version = 2
            }

            val db = open("upgrade2.db")

            assertEquals(listOf("Soap"), RoomProductRepository(db.productDao()).observeAll().first().map { it.name.value })
            assertEquals(CalendarChannel.Off, RoomCalendarChannelSettings(db.calendarChannelDao()).observe().first())
        }

    private companion object {
        /** The tables exactly as exported in `app/schemas/.../2.json`. */
        const val SCHEMA_2_PRODUCTS =
            "CREATE TABLE IF NOT EXISTS `products` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `unit_label` TEXT NOT NULL, " +
                "`package_size` INTEGER NOT NULL, `rate_units` INTEGER NOT NULL, `rate_days` INTEGER NOT NULL, " +
                "`stock_numerator` TEXT NOT NULL, `stock_denominator` TEXT NOT NULL, `recorded_at_epoch_second` INTEGER NOT NULL, " +
                "`recorded_at_nano` INTEGER NOT NULL, `lead_time_days` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        const val SCHEMA_2_DELIVERIES =
            "CREATE TABLE IF NOT EXISTS `alert_deliveries` (`product_id` TEXT NOT NULL, PRIMARY KEY(`product_id`), " +
                "FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
    }
}
