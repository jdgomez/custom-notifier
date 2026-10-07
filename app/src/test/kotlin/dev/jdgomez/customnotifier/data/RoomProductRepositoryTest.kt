package dev.jdgomez.customnotifier.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import dev.jdgomez.customnotifier.domain.ConsumptionRate
import dev.jdgomez.customnotifier.domain.LeadTime
import dev.jdgomez.customnotifier.domain.PackageSize
import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.domain.ProductName
import dev.jdgomez.customnotifier.domain.Quantity
import dev.jdgomez.customnotifier.domain.Rule
import dev.jdgomez.customnotifier.domain.Stock
import dev.jdgomez.customnotifier.domain.UnitLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigInteger
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val OBSERVE_TIMEOUT_MS = 10_000L

@RunWith(RobolectricTestRunner::class)
class RoomProductRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val recordedAt = Instant.parse("2026-03-01T12:00:00.123456789Z")
    private lateinit var database: CustomNotifierDatabase
    private lateinit var repository: RoomProductRepository

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder<CustomNotifierDatabase>(context)
                .setDriver(AndroidSQLiteDriver())
                .build()
        repository = RoomProductRepository(database.productDao())
    }

    @After
    fun close() = database.close()

    private fun product(
        name: String = "Vitamin D",
        units: Quantity = Quantity.of(BigInteger.valueOf(17), BigInteger.TWO),
    ) = Product(
        ProductId(UUID.randomUUID()),
        ProductName(name),
        UnitLabel("pill"),
        PackageSize(90),
        ConsumptionRate(3, 2),
        Stock(units, recordedAt),
        Rule(LeadTime(10)),
    )

    @Test
    fun `round trip of every value`() =
        runTest {
            val saved = product()
            repository.save(saved)
            assertEquals(saved, repository.find(saved.id))
        }

    @Test
    fun `text is stored literally`() =
        runTest {
            val other = product("Detergent")
            val hostile = product("Robert'); DROP TABLE products;--")
            repository.save(other)
            repository.save(hostile)
            assertEquals("Robert'); DROP TABLE products;--", repository.find(hostile.id)?.name?.value)
            assertEquals(other, repository.find(other.id))
        }

    @Test
    fun `products survive a restart`() =
        runTest {
            val name = "restart-test.db"
            val saved = product()

            fun open() =
                Room
                    .databaseBuilder<CustomNotifierDatabase>(context, name)
                    .setDriver(AndroidSQLiteDriver())
                    .build()
            val first = open()
            RoomProductRepository(first.productDao()).save(saved)
            first.close()
            val second = open()
            try {
                assertEquals(saved, RoomProductRepository(second.productDao()).find(saved.id))
            } finally {
                second.close()
                context.deleteDatabase(name)
            }
        }

    @Test
    fun `saving again replaces`() =
        runTest {
            val original = product(units = Quantity.of(30))
            repository.save(original)
            val restocked = original.restock(1, recordedAt)
            repository.save(restocked)
            assertEquals(Quantity.of(120), repository.find(original.id)?.stock?.units)
            assertEquals(listOf(restocked), repository.observeAll().first())
        }

    @Test
    fun `delete removes only that product`() =
        runTest {
            val kept = product("Detergent")
            val removed = product("Vitamin D")
            repository.save(kept)
            repository.save(removed)
            repository.delete(removed.id)
            assertNull(repository.find(removed.id))
            assertEquals(kept, repository.find(kept.id))
        }

    @Test
    fun `unknown identity is not found and deleting it does nothing`() =
        runTest {
            val unknown = ProductId(UUID.randomUUID())
            assertNull(repository.find(unknown))
            repository.delete(unknown)
            assertNull(repository.find(unknown))
        }

    @Test
    fun `updates are delivered`() =
        runBlocking {
            val received = Channel<List<Product>>(Channel.UNLIMITED)
            val job = launch(Dispatchers.Default) { repository.observeAll().collect { received.send(it) } }

            // Each step waits for its emission, so Room cannot conflate two changes into one.
            suspend fun next() = withTimeout(OBSERVE_TIMEOUT_MS) { received.receive() }
            val first = product()
            val edited = first.rename(ProductName("Vitamin C"))
            val seen = mutableListOf(next())
            repository.save(first)
            seen += next()
            repository.save(edited)
            seen += next()
            repository.delete(first.id)
            seen += next()
            job.cancel()
            assertEquals(listOf(emptyList(), listOf(first), listOf(edited), emptyList()), seen)
        }

    @Test
    fun `order by name ignoring case`() =
        runTest {
            listOf("diapers", "Vitamin D", "Detergent").forEach { repository.save(product(it)) }
            assertEquals(listOf("Detergent", "diapers", "Vitamin D"), repository.observeAll().first().map { it.name.value })
        }
}
