package dev.jdgomez.customnotifier.productlist

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
import java.math.BigInteger
import java.time.Instant
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
