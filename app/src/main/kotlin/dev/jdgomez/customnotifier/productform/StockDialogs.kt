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

/** This dialog's input applied to [product] at [now]: the same input with the stock after recomputed. */
internal fun StockDialog.refreshedFor(
    product: Product,
    now: Instant,
): StockDialog =
    when (this) {
        is StockDialog.Restock -> product.restockDialog(packages, now)
        is StockDialog.Adjust -> product.adjustDialog(remove, units, now)
    }

/** The digits of [this] that fit the adjust dialog's units field. */
internal fun String.toUnitsInput(): String = filter(Char::isDigit).take(StockLimits.UNITS_MAX_LENGTH)

/** What an action does to the stock dialogs. */
internal sealed interface StockOutcome {
    data class Show(
        val dialog: StockDialog,
    ) : StockOutcome

    data object Close : StockOutcome

    /** Store [dialog]'s result. */
    data class Confirm(
        val dialog: StockDialog,
    ) : StockOutcome

    /** The action does not apply to the dialog that is open (or to no dialog), for example a tap that arrives after it closed. */
    data object Ignore : StockOutcome
}

/** The outcome of [action] for this product while [dialog] is open (null: none) at [now]. */
internal fun Product.stockOutcome(
    dialog: StockDialog?,
    action: StockAction,
    now: Instant,
): StockOutcome =
    when (dialog) {
        null -> noDialogOutcome(action, now)
        is StockDialog.Restock -> restockOutcome(dialog, action, now)
        is StockDialog.Adjust -> adjustOutcome(dialog, action, now)
    }

private fun Product.noDialogOutcome(
    action: StockAction,
    now: Instant,
): StockOutcome =
    when (action) {
        StockAction.OpenRestock -> StockOutcome.Show(restockDialog(1, now))
        StockAction.OpenAdjust -> StockOutcome.Show(adjustDialog(remove = true, units = "", now))
        StockAction.Dismiss, StockAction.Confirm, is StockAction.Step, is StockAction.SetMode, is StockAction.SetUnits ->
            StockOutcome.Ignore
    }

private fun Product.restockOutcome(
    dialog: StockDialog.Restock,
    action: StockAction,
    now: Instant,
): StockOutcome =
    when (action) {
        is StockAction.Step -> StockOutcome.Show(restockDialog((dialog.packages + action.delta).coerceIn(StockLimits.packages), now))
        StockAction.Dismiss -> StockOutcome.Close
        StockAction.Confirm -> StockOutcome.Confirm(dialog)
        StockAction.OpenRestock, StockAction.OpenAdjust, is StockAction.SetMode, is StockAction.SetUnits -> StockOutcome.Ignore
    }

private fun Product.adjustOutcome(
    dialog: StockDialog.Adjust,
    action: StockAction,
    now: Instant,
): StockOutcome =
    when (action) {
        is StockAction.SetMode -> StockOutcome.Show(adjustDialog(action.remove, dialog.units, now))
        is StockAction.SetUnits -> StockOutcome.Show(adjustDialog(dialog.remove, action.text.toUnitsInput(), now))
        StockAction.Dismiss -> StockOutcome.Close
        StockAction.Confirm -> StockOutcome.Confirm(dialog)
        StockAction.OpenRestock, StockAction.OpenAdjust, is StockAction.Step -> StockOutcome.Ignore
    }
