package dev.jdgomez.customnotifier.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarEventSyncTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val home = CalendarId(1)
    private val family = CalendarId(2)

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(madrid).toInstant()

    private class Gateway : CalendarGateway {
        var access = true
        val calendars = mutableSetOf(CalendarId(1), CalendarId(2))
        val events = mutableMapOf<Pair<CalendarId, EventId>, AlertEvent>()
        var writes = 0
        var accessChecks = 0
        var failure: Exception? = null
        private var next = 100L

        override suspend fun writableCalendars() = emptyList<DeviceCalendar>()

        override suspend fun find(id: CalendarId) = if (access && id in calendars) DeviceCalendar(id, "c", "a") else null

        override suspend fun canWrite(): Boolean {
            failure?.let { if (it !is CalendarUnavailableException) throw it }
            accessChecks++
            return access
        }

        override suspend fun create(
            calendar: CalendarId,
            event: AlertEvent,
        ): EventId? {
            failure?.let { throw it }
            if (!access) return null
            writes++
            return EventId(next++).also { events[calendar to it] = event }
        }

        override suspend fun exists(
            calendar: CalendarId,
            id: EventId,
        ): Boolean {
            failure?.let { throw it }
            return (calendar to id) in events
        }

        override suspend fun update(
            calendar: CalendarId,
            id: EventId,
            event: AlertEvent,
        ): Boolean {
            failure?.let { throw it }
            if ((calendar to id) !in events) return false
            writes++
            events[calendar to id] = event
            return true
        }

        override suspend fun delete(
            calendar: CalendarId,
            id: EventId,
        ) {
            failure?.let { throw it }
            if (events.remove(calendar to id) != null) writes++
        }

        fun titles(calendar: CalendarId) =
            events
                .filterKeys { it.first == calendar }
                .values
                .map {
                    it.productName.value to
                        it.date.toString()
                }.sortedBy { it.first }
    }

    private class Records : CalendarEventRecords {
        val map = mutableMapOf<ProductId, CalendarEventRecord>()

        override suspend fun all() = map.values.toList()

        override suspend fun put(record: CalendarEventRecord) {
            map[record.productId] = record
        }

        override suspend fun remove(productId: ProductId) {
            map -= productId
        }
    }

    private class Repo : ProductRepository {
        val state = MutableStateFlow(emptyList<Product>())

        override fun observeAll(): Flow<List<Product>> = state

        override suspend fun find(id: ProductId) = state.value.firstOrNull { it.id == id }

        override suspend fun save(product: Product) {
            state.value = state.value.filter { it.id != product.id } + product
        }

        override suspend fun delete(id: ProductId) {
            state.value = state.value.filter { it.id != id }
        }
    }

    private val gateway = Gateway()
    private val records = Records()
    private val changes = ProductChanges()
    private val store = Repo()
    private val repo = TrackingProductRepository(store, changes)
    private val sync = CalendarEventSync(gateway, records, changes)
    private var now = at("2026-02-20T12:00")
    private var channel: CalendarChannel = CalendarChannel.On(home)

    /** Runs out [units] days after 2026-02-20 12:00 Madrid, alert 10 days before. */
    private fun product(
        name: String,
        units: Long = 19,
    ) = Product(
        ProductId(UUID.randomUUID()),
        ProductName(name),
        UnitLabel("u"),
        PackageSize(30),
        ConsumptionRate(1, 1),
        Stock(Quantity.of(units), at("2026-02-20T12:00")),
        Rule(LeadTime(10)),
    )

    private var language = LanguageTag("en")

    private suspend fun run() = sync.sync(store.state.value, channel, now, madrid, language)

    @Test
    fun `event on the alert date with the product texts`() =
        runTest {
            val coffee = product("Coffee")
            repo.save(coffee)
            run()
            assertEquals(listOf("Coffee" to "2026-03-01"), gateway.titles(home))
            assertEquals(
                DepletionDate(LocalDate.parse("2026-03-11")),
                gateway.events.values
                    .single()
                    .depletionDate,
            )
        }

    @Test
    fun `event kept after delivery and deleted after the depletion date`() =
        runTest {
            repo.save(product("Coffee"))
            run()
            now = at("2026-03-05T12:00")
            run()
            assertEquals(listOf("Coffee" to "2026-03-01"), gateway.titles(home))
            now = at("2026-03-12T12:00")
            run()
            assertTrue(gateway.events.isEmpty())
            assertTrue(records.map.isEmpty())
        }

    @Test
    fun `restock moves the event and rename updates it`() =
        runTest {
            val coffee = product("Coffee")
            repo.save(coffee)
            run()
            repo.save(coffee.restock(1, now).rename(ProductName("Espresso")))
            run()
            assertEquals(listOf("Espresso" to "2026-03-31"), gateway.titles(home))
        }

    @Test
    fun `a language change rewrites the events written in the other language`() =
        runTest {
            repo.save(product("Coffee"))
            run()
            val writes = gateway.writes
            language = LanguageTag("es")
            run()
            assertEquals(writes + 1, gateway.writes)
            assertEquals(listOf(LanguageTag("es")), gateway.events.values.map { it.language })
            assertEquals(
                LanguageTag("es"),
                records.map.values
                    .single()
                    .content.language,
            )
            run()
            assertEquals(writes + 1, gateway.writes)
        }

    @Test
    fun `a language change does not recreate an event deleted outside`() =
        runTest {
            repo.save(product("Coffee"))
            run()
            gateway.events.clear()
            language = LanguageTag("es")
            run()
            assertTrue(gateway.events.isEmpty())
            assertNull(
                records.map.values
                    .single()
                    .eventId,
            )
        }

    @Test
    fun `deleting the product deletes the event`() =
        runTest {
            val coffee = product("Coffee")
            repo.save(coffee)
            run()
            repo.delete(coffee.id)
            run()
            assertTrue(gateway.events.isEmpty())
        }

    @Test
    fun `turning on creates all, changing calendar moves, turning off deletes`() =
        runTest {
            channel = CalendarChannel.Off
            repo.save(product("Coffee"))
            repo.save(product("Rice"))
            run()
            assertTrue(gateway.events.isEmpty())
            channel = CalendarChannel.On(home)
            run()
            assertEquals(2, gateway.titles(home).size)
            channel = CalendarChannel.On(family)
            run()
            assertTrue(gateway.titles(home).isEmpty())
            assertEquals(2, gateway.titles(family).size)
            channel = CalendarChannel.Off
            run()
            assertTrue(gateway.events.isEmpty())
            assertTrue(records.map.isEmpty())
        }

    @Test
    fun `an old calendar that is gone is left alone when changing calendar`() =
        runTest {
            repo.save(product("Coffee"))
            run()
            gateway.calendars -= home
            gateway.events.clear()
            channel = CalendarChannel.On(family)
            run()
            assertEquals(1, gateway.titles(family).size)
        }

    @Test
    fun `a deleted event stays deleted in the background and returns after a product change`() =
        runTest {
            val coffee = product("Coffee")
            val rice = product("Rice")
            repo.save(coffee)
            repo.save(rice)
            run()
            gateway.events.keys
                .first { gateway.events[it]!!.productName.value == "Coffee" }
                .let { gateway.events -= it }
            run()
            run()
            assertEquals(listOf("Rice" to "2026-03-01"), gateway.titles(home))
            repo.save(rice.restock(1, now))
            run()
            assertEquals(listOf("Rice" to "2026-03-31"), gateway.titles(home))
            repo.save(coffee.restock(1, now))
            run()
            assertEquals(listOf("Coffee" to "2026-03-31", "Rice" to "2026-03-31"), gateway.titles(home))
        }

    @Test
    fun `an event deleted outside while the content changes is recreated for the changed product only`() =
        runTest {
            val coffee = product("Coffee")
            repo.save(coffee)
            run()
            gateway.events.clear()
            repo.save(coffee.restock(1, now))
            run()
            assertEquals(listOf("Coffee" to "2026-03-31"), gateway.titles(home))
        }

    @Test
    fun `without access nothing is written and records are kept, then the sync catches up`() =
        runTest {
            val coffee = product("Coffee")
            repo.save(coffee)
            run()
            gateway.access = false
            val rice = product("Rice")
            repo.save(rice)
            repo.delete(coffee.id)
            run()
            assertEquals(1, records.map.size)
            assertEquals(1, gateway.events.size)
            gateway.access = true
            run()
            assertEquals(listOf("Rice" to "2026-03-01"), gateway.titles(home))
        }

    @Test
    fun `a calendar that is gone writes nothing and keeps the records`() =
        runTest {
            repo.save(product("Coffee"))
            run()
            gateway.calendars -= home
            repo.save(product("Rice"))
            run()
            assertEquals(1, records.map.size)
            assertEquals(1, gateway.events.size)
        }

    @Test
    fun `channel off with nothing recorded never asks for calendar access`() =
        runTest {
            channel = CalendarChannel.Off
            repo.save(product("Coffee"))
            run()
            assertEquals(0, gateway.accessChecks)
        }

    @Test
    fun `a second sync writes nothing`() =
        runTest {
            repo.save(product("Coffee"))
            repo.save(product("Rice"))
            run()
            val writes = gateway.writes
            run()
            assertEquals(writes, gateway.writes)
            assertNull(records.map.values.firstOrNull { it.eventId == null })
        }

    @Test
    fun `access lost in the middle of a sync keeps the records and the event comes back`() =
        runTest {
            val coffee = product("Coffee")
            repo.save(coffee)
            run()
            repo.save(coffee.restock(1, now))
            gateway.failure = CalendarUnavailableException()
            run()
            assertEquals(1, records.map.size)
            assertEquals(
                EventId(100),
                records.map.values
                    .single()
                    .eventId,
            )
            gateway.failure = null
            run()
            assertEquals(listOf("Coffee" to "2026-03-31"), gateway.titles(home))
        }

    @Test
    fun `an unavailable provider never makes the event look deleted`() =
        runTest {
            val coffee = product("Coffee")
            repo.save(coffee)
            run()
            gateway.failure = CalendarUnavailableException()
            run()
            run()
            gateway.failure = null
            repo.save(coffee)
            run()
            assertEquals(1, gateway.events.size)
            assertEquals(1, gateway.writes)
        }

    @Test
    fun `an unexpected failure of the calendar side never reaches the caller and keeps the records`() =
        runTest {
            repo.save(product("Coffee"))
            run()
            gateway.failure = IllegalStateException("provider exploded")
            run()
            assertEquals(1, records.map.size)
        }

    @Test
    fun `a change recorded while a sync was running is not lost`() {
        val id = ProductId(UUID.randomUUID())
        val seen =
            changes.run {
                record(id)
                pending()
            }
        changes.record(id)
        changes.clear(seen)
        assertEquals(setOf(id), changes.pending().keys)
        changes.clear(changes.pending())
        assertTrue(changes.pending().isEmpty())
    }

    @Test
    fun `a sync that started from the old list does not consume the change of a save in flight`() =
        runTest {
            val coffee = product("Coffee")
            repo.save(coffee)
            run()
            gateway.events.clear()
            val raced =
                TrackingProductRepository(
                    object : ProductRepository by store {
                        override suspend fun save(product: Product) {
                            // A sync that already read the old list runs while the write is stored.
                            sync.sync(listOf(coffee), channel, now, madrid, language)
                            store.save(product)
                        }
                    },
                    changes,
                )
            raced.save(coffee.restock(1, now))
            run()
            assertEquals(listOf("Coffee" to "2026-03-31"), gateway.titles(home))
        }
}
