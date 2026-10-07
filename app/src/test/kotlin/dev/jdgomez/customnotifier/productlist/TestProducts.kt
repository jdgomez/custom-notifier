package dev.jdgomez.customnotifier.productlist

import dev.jdgomez.customnotifier.domain.ConsumptionRate
import dev.jdgomez.customnotifier.domain.LeadTime
import dev.jdgomez.customnotifier.domain.PackageSize
import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.domain.ProductName
import dev.jdgomez.customnotifier.domain.ProductRepository
import dev.jdgomez.customnotifier.domain.Quantity
import dev.jdgomez.customnotifier.domain.Rule
import dev.jdgomez.customnotifier.domain.Stock
import dev.jdgomez.customnotifier.domain.UnitLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/** A product holding [units] (a whole number, or [halves] / 2) as of [recordedAt], consuming 1 unit per day. */
fun testProduct(
    name: String,
    recordedAt: Instant,
    units: Long = 10,
    halves: Boolean = false,
    unitLabel: String = "pills",
    leadDays: Int = 10,
) = Product(
    id = ProductId(UUID.nameUUIDFromBytes(name.toByteArray())),
    name = ProductName(name),
    unitLabel = UnitLabel(unitLabel),
    packageSize = PackageSize(30),
    consumptionRate = ConsumptionRate(units = 1, days = 1),
    stock = Stock(if (halves) Quantity.of(BigInteger.valueOf(units), BigInteger.TWO) else Quantity.of(units), recordedAt),
    rule = Rule(LeadTime(leadDays)),
)

class FakeRepository : ProductRepository {
    val products = MutableStateFlow<List<Product>>(emptyList())

    override fun observeAll() = products.map { all -> all.sortedBy { it.name.value.lowercase() } }

    override suspend fun find(id: ProductId) = products.value.firstOrNull { it.id == id }

    override suspend fun save(product: Product) {
        products.value = products.value.filter { it.id != product.id } + product
    }

    override suspend fun delete(id: ProductId) {
        products.value = products.value.filter { it.id != id }
    }
}

class MutableClock(
    var now: Instant,
) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = now
}
