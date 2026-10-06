package dev.jdgomez.customnotifier.domain

import java.time.Instant

/** A consumable the user must restock. Immutable: every operation returns a new [Product]. */
data class Product(
    val name: ProductName,
    val unitLabel: UnitLabel,
    val packageSize: PackageSize,
    val consumptionRate: ConsumptionRate,
    val stock: Stock,
) {
    fun estimatedStockAt(now: Instant): EstimatedStock = stock.estimateAt(now, consumptionRate)

    /** Changes the stock by [units] (non-zero, may be negative), never going below zero. */
    fun adjust(
        units: Int,
        now: Instant,
    ): Product {
        require(units != 0) { "adjustment must not be zero" }
        return reanchor(now) { it + Quantity.of(units.toLong()) }
    }

    /** Adds [packages] (at least 1) times the package size to the stock. */
    fun restock(
        packages: Int,
        now: Instant,
    ): Product {
        require(packages >= 1) { "packages must be at least 1" }
        return reanchor(now) { it + Quantity.of(packages.toLong() * packageSize.units) }
    }

    /** Changes the rate; consumption up to [now] keeps the old rate. */
    fun changeConsumptionRate(
        rate: ConsumptionRate,
        now: Instant,
    ): Product = reanchor(now) { it }.copy(consumptionRate = rate)

    fun changePackageSize(size: PackageSize) = copy(packageSize = size)

    fun rename(name: ProductName) = copy(name = name)

    fun relabelUnit(unitLabel: UnitLabel) = copy(unitLabel = unitLabel)

    private fun reanchor(
        now: Instant,
        change: (Quantity) -> Quantity,
    ): Product {
        val updated = change(estimatedStockAt(now).exact).coerceAtLeast(Quantity.ZERO)
        return copy(stock = Stock(updated, maxOf(now, stock.recordedAt)))
    }

    companion object {
        /** Creates a product holding [initialUnits] whole units as of [now]. */
        fun create(
            name: ProductName,
            unitLabel: UnitLabel,
            packageSize: PackageSize,
            consumptionRate: ConsumptionRate,
            initialUnits: Int,
            now: Instant,
        ): Product {
            require(initialUnits >= 0) { "stock must not be negative" }
            return Product(name, unitLabel, packageSize, consumptionRate, Stock(Quantity.of(initialUnits.toLong()), now))
        }
    }
}
