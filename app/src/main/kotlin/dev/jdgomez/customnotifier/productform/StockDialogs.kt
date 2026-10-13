package dev.jdgomez.customnotifier.productform

import dev.jdgomez.customnotifier.domain.Product
import java.time.Instant

internal fun Product.restockDialog(
    packages: Int,
    now: Instant,
) = StockDialog.Restock(packages, packages * packageSize.units, restock(packages, now).estimatedStockAt(now))

internal fun Product.adjustDialog(
    remove: Boolean,
    units: String,
    now: Instant,
): StockDialog.Adjust {
    val signed = signedUnits(remove, units)
    return StockDialog.Adjust(remove, units, signed?.let { adjust(it, now).estimatedStockAt(now) })
}

/** The units to adjust by, or null while [units] is empty or outside the limits. */
private fun signedUnits(
    remove: Boolean,
    units: String,
): Int? = units.toIntOrNull()?.takeIf { it in StockLimits.units }?.let { if (remove) -it else it }

/** [product] with this dialog's restock or adjustment applied at [now]. */
internal fun StockDialog.applyTo(
    product: Product,
    now: Instant,
): Product =
    when (this) {
        is StockDialog.Restock -> product.restock(packages, now)
        is StockDialog.Adjust -> product.adjust(checkNotNull(signedUnits(remove, units)), now)
    }

internal fun Product.stockSummary(now: Instant) = StockSummary(estimatedStockAt(now), unitLabel.value)
