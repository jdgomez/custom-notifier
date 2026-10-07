package dev.jdgomez.customnotifier.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DepletionAndAlertTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun at(
        local: String,
        zone: ZoneId = madrid,
    ): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun product(
        units: Long,
        recordedAt: Instant,
        rate: ConsumptionRate = ConsumptionRate(1, 1),
        leadDays: Int = 10,
    ) = Product(
        ProductId(java.util.UUID.randomUUID()),
        ProductName("Vitamin D"),
        UnitLabel("pill"),
        PackageSize(30),
        rate,
        Stock(Quantity.of(units), recordedAt),
        Rule(LeadTime(leadDays)),
    )

    private fun Product.depletionIn(zone: ZoneId) = depletionDateIn(zone).date

    private fun scheduled(
        local: String,
        zone: ZoneId = madrid,
    ) = NextAlert.Scheduled(LocalDateTime.parse(local).atZone(zone))

    // --- Depletion date

    @Test
    fun `whole days`() {
        assertEquals(LocalDate.parse("2026-03-11"), product(10, at("2026-03-01T12:00")).depletionIn(madrid))
    }

    @Test
    fun `fractional rate depletes at an exact moment`() {
        val product = product(10, at("2026-03-01T12:00"), ConsumptionRate(3, 2))
        assertEquals(at("2026-03-08T04:00"), product.depletionMoment)
        assertEquals(LocalDate.parse("2026-03-08"), product.depletionIn(madrid))
    }

    @Test
    fun `estimated stock at the depletion moment is exactly zero for a fractional rate`() {
        val product = product(10, at("2026-03-01T12:00"), ConsumptionRate(3, 7))
        val moment = product.depletionMoment
        assertEquals(Quantity.ZERO, product.estimatedStockAt(moment).exact)
        assertTrue(product.estimatedStockAt(moment.minusMillis(1)).exact > Quantity.ZERO)
    }

    @Test
    fun `time zone decides the depletion date`() {
        val product = product(1, Instant.parse("2026-06-01T20:00:00Z"))
        assertEquals(LocalDate.parse("2026-06-02"), product.depletionIn(madrid))
        assertEquals(LocalDate.parse("2026-06-03"), product.depletionIn(ZoneId.of("Asia/Tokyo")))
    }

    @Test
    fun `depletion at midnight is on the new date`() {
        assertEquals(LocalDate.parse("2026-03-11"), product(1, at("2026-03-10T00:00")).depletionIn(madrid))
    }

    @Test
    fun `already out of stock depletes at the recording`() {
        val product = product(0, at("2026-03-10T18:00"))
        assertEquals(at("2026-03-10T18:00"), product.depletionMoment)
        assertEquals(LocalDate.parse("2026-03-10"), product.depletionIn(madrid))
    }

    @Test
    fun `adjustment moves the depletion date`() {
        val recorded = at("2026-03-01T12:00")
        val product = product(10, recorded)
        assertEquals(LocalDate.parse("2026-03-11"), product.depletionIn(madrid))
        assertEquals(LocalDate.parse("2026-04-10"), product.restock(1, recorded).depletionIn(madrid))
    }

    // --- Next alert

    private val tenDays = product(10, at("2026-03-01T12:00"))

    @Test
    fun `scheduled before the alert moment`() {
        assertEquals(scheduled("2026-03-01T09:00"), tenDays.nextAlertAt(at("2026-02-20T10:00"), madrid))
    }

    @Test
    fun `reaching the alert moment makes it due`() {
        assertEquals(NextAlert.Due, tenDays.nextAlertAt(at("2026-03-01T09:00"), madrid))
        assertEquals(scheduled("2026-03-01T09:00"), tenDays.nextAlertAt(at("2026-03-01T09:00").minusMillis(1), madrid))
    }

    @Test
    fun `missed alert is due until the end of the depletion date`() {
        assertEquals(NextAlert.Due, tenDays.nextAlertAt(at("2026-03-05T10:00"), madrid))
        assertEquals(NextAlert.Due, tenDays.nextAlertAt(at("2026-03-11T23:59"), madrid))
    }

    @Test
    fun `none after the depletion date`() {
        assertEquals(NextAlert.None, tenDays.nextAlertAt(at("2026-03-12T00:00"), madrid))
    }

    @Test
    fun `lead time of zero alerts on the depletion date`() {
        val product = product(1, at("2026-03-10T03:00"), leadDays = 0)
        assertEquals(scheduled("2026-03-11T09:00"), product.nextAlertAt(at("2026-03-11T08:00"), madrid))
        assertEquals(NextAlert.Due, product.nextAlertAt(at("2026-03-11T10:00"), madrid))
    }

    @Test
    fun `lead time longer than the remaining stock is due at once`() {
        val product = product(5, at("2026-03-01T12:00"))
        assertEquals(NextAlert.Due, product.nextAlertAt(at("2026-03-01T12:00"), madrid))
    }

    @Test
    fun `product created out of stock is due for the rest of its date and none from the next date`() {
        val created = at("2026-03-10T18:00")
        val product =
            Product.create(
                ProductName("Vitamin D"),
                UnitLabel("pill"),
                PackageSize(30),
                ConsumptionRate(1, 1),
                Rule(LeadTime(10)),
                0,
                created,
            )
        assertEquals(NextAlert.Due, product.nextAlertAt(created, madrid))
        assertEquals(NextAlert.Due, product.nextAlertAt(at("2026-03-10T23:59"), madrid))
        assertEquals(NextAlert.None, product.nextAlertAt(at("2026-03-11T00:00"), madrid))
    }

    @Test
    fun `time zone decides the alert moment`() {
        val product = product(1, Instant.parse("2026-06-10T14:00:00Z"))
        val now = Instant.parse("2026-05-01T00:00:00Z")
        val newYork = ZoneId.of("America/New_York")
        val madridAlert = product.nextAlertAt(now, madrid) as NextAlert.Scheduled
        val newYorkAlert = product.nextAlertAt(now, newYork) as NextAlert.Scheduled
        assertEquals(Instant.parse("2026-06-01T07:00:00Z"), madridAlert.at.toInstant())
        assertEquals(Instant.parse("2026-06-01T13:00:00Z"), newYorkAlert.at.toInstant())
        assertEquals(9, madridAlert.at.hour)
        assertEquals(9, newYorkAlert.at.hour)
    }

    @Test
    fun `changing the rule changes only the next alert`() {
        val now = at("2026-02-20T10:00")
        val product = product(30, at("2026-03-01T12:00"), leadDays = 10)
        val edited = product.changeRule(Rule(LeadTime(5)))
        assertEquals(product.depletionDateIn(madrid), edited.depletionDateIn(madrid))
        assertEquals(LocalDate.parse("2026-03-31"), edited.depletionIn(madrid))
        assertEquals(scheduled("2026-03-26T09:00"), edited.nextAlertAt(now, madrid))
    }

    // --- Alert time on clock changes

    @Test
    fun `alert stays at 09 00 local when daylight saving time starts`() {
        val product = product(0, at("2026-04-08T12:00"))
        val alert = product.nextAlertAt(at("2026-03-01T00:00"), madrid)
        assertEquals(scheduled("2026-03-29T09:00"), alert)
        assertEquals(Instant.parse("2026-03-29T07:00:00Z"), (alert as NextAlert.Scheduled).at.toInstant())
    }

    @Test
    fun `alert in a gap shifts later by the gap length`() {
        val zone = TestZones.gap
        val product = product(0, at("2026-04-20T12:00", zone), leadDays = 10)
        val alert = product.nextAlertAt(at("2026-04-01T00:00", zone), zone)
        assertEquals(scheduled("2026-04-10T10:00", zone), alert)
        assertEquals(Instant.parse("2026-04-10T09:00:00Z"), (alert as NextAlert.Scheduled).at.toInstant())
    }

    @Test
    fun `alert in an overlap uses the earlier 09 00`() {
        val zone = TestZones.overlap
        val product = product(0, at("2026-04-20T12:00", zone), leadDays = 10)
        val alert = product.nextAlertAt(at("2026-04-01T00:00", zone), zone) as NextAlert.Scheduled
        assertEquals(ZonedDateTime.of(LocalDateTime.parse("2026-04-10T09:00"), zone).withEarlierOffsetAtOverlap(), alert.at)
        assertEquals(Instant.parse("2026-04-10T08:00:00Z"), alert.at.toInstant())
    }
}
