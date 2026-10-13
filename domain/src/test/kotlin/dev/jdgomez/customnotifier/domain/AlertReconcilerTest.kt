package dev.jdgomez.customnotifier.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlertReconcilerTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private var zone: ZoneId = madrid
    private var now: Instant = at("2026-03-01T12:00")

    private fun at(
        local: String,
        zone: ZoneId = madrid,
    ): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private class Repo(
        initial: List<Product> = emptyList(),
    ) : ProductRepository {
        val state = MutableStateFlow(initial)

        override fun observeAll(): Flow<List<Product>> = state

        override suspend fun find(id: ProductId) = state.value.firstOrNull { it.id == id }

        override suspend fun save(product: Product) {
            state.value = state.value.filter { it.id != product.id } + product
        }

        override suspend fun delete(id: ProductId) {
            state.value = state.value.filter { it.id != id }
        }
    }

    private class Deliveries : AlertDeliveries {
        val delivered = mutableSetOf<ProductId>()

        override suspend fun isDelivered(id: ProductId) = id in delivered

        override suspend fun record(id: ProductId) {
            delivered += id
        }

        override suspend fun clear(id: ProductId) {
            delivered -= id
        }
    }

    private class Scheduler : AlertScheduler {
        val alarms = mutableMapOf<ProductId, Instant>()

        override fun schedule(
            id: ProductId,
            at: Instant,
        ) {
            alarms[id] = at
        }

        override fun cancel(id: ProductId) {
            alarms -= id
        }
    }

    private class Notifier : AlertNotifier {
        var enabled = true
        val posted = mutableListOf<Triple<Product, DepletionDate, LocalDate>>()

        override fun notify(
            product: Product,
            depletionDate: DepletionDate,
            today: LocalDate,
        ): Boolean {
            if (enabled) posted += Triple(product, depletionDate, today)
            return enabled
        }
    }

    private val repo = Repo()
    private val deliveries = Deliveries()
    private val scheduler = Scheduler()
    private val notifier = Notifier()
    private val reconciler =
        AlertReconciler(
            repo,
            deliveries,
            scheduler,
            notifier,
            object : Clock() {
                override fun getZone() = ZoneOffset.UTC

                override fun withZone(zone: ZoneId?) = this

                override fun instant() = now
            },
            { zone },
        )

    /** Runs out 17 days after 2026-03-01 12:00 Madrid (2026-03-18), alert 10 days before: 2026-03-08 09:00. */
    private fun product(
        units: Long = 17,
        leadDays: Int = 10,
    ) = Product(
        ProductId(UUID.randomUUID()),
        ProductName("Vitamin D"),
        UnitLabel("pill"),
        PackageSize(30),
        ConsumptionRate(1, 1),
        Stock(Quantity.of(units), at("2026-03-01T12:00")),
        Rule(LeadTime(leadDays)),
    )

    @Test
    fun `decision table`() {
        val at = LocalDateTime.parse("2026-03-08T09:00").atZone(madrid)
        assertEquals(AlertAction.Schedule(at), AlertAction.decide(NextAlert.Scheduled(at), delivered = true))
        assertEquals(AlertAction.Schedule(at), AlertAction.decide(NextAlert.Scheduled(at), delivered = false))
        assertEquals(AlertAction.Deliver, AlertAction.decide(NextAlert.Due, delivered = false))
        assertEquals(AlertAction.Skip, AlertAction.decide(NextAlert.Due, delivered = true))
        assertEquals(AlertAction.Skip, AlertAction.decide(NextAlert.None, delivered = false))
    }

    @Test
    fun `schedules the alarm at the alert moment and posts nothing`() =
        runTest {
            val p = product()
            repo.save(p)
            reconciler.reconcile()
            assertEquals(at("2026-03-08T09:00"), scheduler.alarms[p.id])
            assertTrue(notifier.posted.isEmpty())
        }

    @Test
    fun `delivers a due alert exactly once`() =
        runTest {
            val p = product()
            repo.save(p)
            now = at("2026-03-10T10:00")
            reconciler.reconcile()
            reconciler.reconcile()
            assertEquals(1, notifier.posted.size)
            assertEquals(DepletionDate(LocalDate.parse("2026-03-18")), notifier.posted.single().second)
            assertEquals(LocalDate.parse("2026-03-10"), notifier.posted.single().third)
            assertNull(scheduler.alarms[p.id])
            assertTrue(p.id in deliveries.delivered)
        }

    @Test
    fun `a restock re-arms the alert`() =
        runTest {
            val p = product()
            repo.save(p)
            now = at("2026-03-10T10:00")
            reconciler.reconcile()
            repo.save(p.restock(30, now))
            reconciler.reconcile()
            assertTrue(p.id !in deliveries.delivered)
            assertTrue(scheduler.alarms.containsKey(p.id))
        }

    @Test
    fun `a small correction after delivery does not alert again`() =
        runTest {
            val p = product()
            repo.save(p)
            now = at("2026-03-10T10:00")
            reconciler.reconcile()
            repo.save(p.adjust(-1, now))
            reconciler.reconcile()
            assertEquals(1, notifier.posted.size)
        }

    @Test
    fun `past the depletion date nothing is scheduled or posted`() =
        runTest {
            val p = product()
            repo.save(p)
            now = at("2026-03-19T10:00")
            reconciler.reconcile()
            assertTrue(scheduler.alarms.isEmpty())
            assertTrue(notifier.posted.isEmpty())
        }

    @Test
    fun `a missed alert is delivered late`() =
        runTest {
            repo.save(product())
            now = at("2026-03-17T10:00")
            reconciler.reconcile()
            assertEquals(1, notifier.posted.size)
        }

    @Test
    fun `disabled notifications are retried and not recorded`() =
        runTest {
            val p = product()
            repo.save(p)
            now = at("2026-03-10T10:00")
            notifier.enabled = false
            reconciler.reconcile()
            assertTrue(p.id !in deliveries.delivered)
            notifier.enabled = true
            reconciler.reconcile()
            assertEquals(1, notifier.posted.size)
            assertTrue(p.id in deliveries.delivered)
        }

    @Test
    fun `a time zone change moves the alarm to the same local time`() =
        runTest {
            val p = product()
            repo.save(p)
            reconciler.reconcile()
            val newYork = ZoneId.of("America/New_York")
            zone = newYork
            reconciler.reconcile()
            assertEquals(at("2026-03-08T09:00", newYork), scheduler.alarms[p.id])
        }

    @Test
    fun `a longer lead time moves the alarm earlier`() =
        runTest {
            val p = product()
            repo.save(p)
            reconciler.reconcile()
            repo.save(p.changeRule(Rule(LeadTime(12))))
            reconciler.reconcile()
            assertEquals(at("2026-03-06T09:00"), scheduler.alarms[p.id])
        }

    @Test
    fun `deleting a product cancels its alarm`() =
        runTest {
            val p = product()
            repo.save(p)
            reconciler.reconcile()
            repo.delete(p.id)
            reconciler.reconcile()
            assertTrue(scheduler.alarms.isEmpty())
        }
}
