package dev.jdgomez.customnotifier.productlist

import androidx.compose.runtime.Immutable
import dev.jdgomez.customnotifier.domain.EstimatedStock
import dev.jdgomez.customnotifier.domain.NextAlert
import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.domain.ProductId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the product list screen shows. */
sealed interface ProductListState {
    /** The stored products have not been read yet. */
    data object Loading : ProductListState

    /** No product is stored. */
    data object Empty : ProductListState

    @Immutable
    data class Products(
        val rows: List<ProductRow>,
    ) : ProductListState
}

/** One product as the list shows it: values already computed for a moment and a time zone. */
data class ProductRow(
    val id: ProductId,
    val name: String,
    val unitLabel: String,
    val stock: EstimatedStock,
    val status: RowStatus,
)

sealed interface RowStatus {
    /** The product has not run out: [alertDate] is the next alert's local date, or null when the alert is due. */
    data class Upcoming(
        val depletionDate: LocalDate,
        val alertDate: LocalDate?,
    ) : RowStatus

    data class RanOut(
        val depletionDate: LocalDate,
    ) : RowStatus
}

/** Maps [this] stored list, in its order, to the screen state at [now] in [zone], using only domain results. */
fun List<Product>.toListState(
    now: Instant,
    zone: ZoneId,
): ProductListState = if (isEmpty()) ProductListState.Empty else ProductListState.Products(map { it.toRow(now, zone) })

private fun Product.toRow(
    now: Instant,
    zone: ZoneId,
): ProductRow = ProductRow(id, name.value, unitLabel.value, estimatedStockAt(now), statusAt(now, zone))

/** The depletion date and next alert of [this] at [now] in [zone], as the list and the product form show them. */
fun Product.statusAt(
    now: Instant,
    zone: ZoneId,
): RowStatus {
    val depletionDate = depletionDateIn(zone).date
    return when (val alert = nextAlertAt(now, zone)) {
        is NextAlert.Scheduled -> RowStatus.Upcoming(depletionDate, alert.at.toLocalDate())
        NextAlert.Due -> RowStatus.Upcoming(depletionDate, null)
        NextAlert.None -> RowStatus.RanOut(depletionDate)
    }
}
