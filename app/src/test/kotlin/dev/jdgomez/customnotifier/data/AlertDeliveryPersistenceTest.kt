package dev.jdgomez.customnotifier.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import dev.jdgomez.customnotifier.productlist.testProduct
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AlertDeliveryPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val recordedAt = Instant.parse("2026-03-01T12:00:00Z")
    private var database: CustomNotifierDatabase? = null

    @After
    fun close() {
        database?.close()
    }

    private fun open(name: String? = null): CustomNotifierDatabase {
        val builder =
            if (name == null) {
                Room.inMemoryDatabaseBuilder<CustomNotifierDatabase>(context)
            } else {
                Room.databaseBuilder<CustomNotifierDatabase>(context, name)
            }
        return builder
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
            .also { database = it }
    }

    @Test
    fun `records and clears a delivery`() =
        runBlocking {
            val db = open()
            val product = testProduct("Vitamin D", recordedAt)
            RoomProductRepository(db.productDao()).save(product)
            val deliveries = RoomAlertDeliveries(db.alertDeliveryDao())

            assertFalse(deliveries.isDelivered(product.id))
            deliveries.record(product.id)
            deliveries.record(product.id)
            assertTrue(deliveries.isDelivered(product.id))
            deliveries.clear(product.id)
            assertFalse(deliveries.isDelivered(product.id))
        }

    @Test
    fun `deleting a product removes its delivery record`() =
        runBlocking {
            val db = open()
            val repository = RoomProductRepository(db.productDao())
            val deliveries = RoomAlertDeliveries(db.alertDeliveryDao())
            val product = testProduct("Vitamin D", recordedAt)
            repository.save(product)
            deliveries.record(product.id)

            repository.delete(product.id)
            // Saving the same identity again must not bring the delivery record back.
            repository.save(product)

            assertFalse(deliveries.isDelivered(product.id))
        }

    @Test
    fun `recording for a product that no longer exists is ignored`() =
        runBlocking {
            val db = open()
            val deliveries = RoomAlertDeliveries(db.alertDeliveryDao())
            val product = testProduct("Vitamin D", recordedAt)

            deliveries.record(product.id)

            assertFalse(deliveries.isDelivered(product.id))
        }

    @Test
    fun `record survives reopening the database`() =
        runBlocking {
            val product = testProduct("Vitamin D", recordedAt)
            val first = open("survives.db")
            RoomProductRepository(first.productDao()).save(product)
            RoomAlertDeliveries(first.alertDeliveryDao()).record(product.id)
            first.close()

            val second = open("survives.db")
            assertTrue(RoomAlertDeliveries(second.alertDeliveryDao()).isDelivered(product.id))
        }

    @Test
    fun `upgrade from schema 1 keeps the products and records nothing`() =
        runBlocking {
            val path = context.getDatabasePath("upgrade.db")
            path.parentFile?.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                old.execSQL(SCHEMA_1_PRODUCTS)
                old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                old.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, '4b50606ff701cd82ea847cceec555ef5')")
                val rows = mapOf("00000000-0000-0000-0000-000000000001" to "Vitamin D", "00000000-0000-0000-0000-000000000002" to "Soap")
                for ((id, name) in rows) {
                    old.execSQL("INSERT INTO products VALUES ('$id', '$name', 'pill', 30, 1, 1, '17', '2', 1772366400, 0, 10)")
                }
                old.version = 1
            }

            val db = open("upgrade.db")
            val products = RoomProductRepository(db.productDao()).observeAll().first()

            assertEquals(listOf("Soap", "Vitamin D"), products.map { it.name.value })
            assertEquals(listOf(10, 10), products.map { it.rule.leadTime.days })
            val deliveries = RoomAlertDeliveries(db.alertDeliveryDao())
            assertTrue(products.none { deliveries.isDelivered(it.id) })
        }

    private companion object {
        /** The `products` table exactly as exported in `app/schemas/.../1.json`. */
        const val SCHEMA_1_PRODUCTS =
            "CREATE TABLE IF NOT EXISTS `products` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `unit_label` TEXT NOT NULL, " +
                "`package_size` INTEGER NOT NULL, `rate_units` INTEGER NOT NULL, `rate_days` INTEGER NOT NULL, " +
                "`stock_numerator` TEXT NOT NULL, `stock_denominator` TEXT NOT NULL, `recorded_at_epoch_second` INTEGER NOT NULL, " +
                "`recorded_at_nano` INTEGER NOT NULL, `lead_time_days` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    }
}
