package dev.jdgomez.customnotifier.productform

import dev.jdgomez.customnotifier.domain.EstimatedStock
import dev.jdgomez.customnotifier.productlist.RowStatus

/** The form's fields in screen order, with the input rules of each: the length cap and, for numbers, the allowed range. */
@Suppress("MagicNumber") // The limits are the form's input rules (see the product-form spec), written once here.
enum class ProductFormField(
    val maxLength: Int,
    val range: IntRange? = null,
) {
    Name(60),
    Unit(20),
    PackageSize(6, 1..100_000),
    ConsumptionUnits(6, 1..100_000),
    ConsumptionDays(3, 1..365),
    LeadTime(3, 0..365),
    UnitsNow(7, 0..1_000_000),
}

/** What the product form shows: the raw field texts, the fields to flag as invalid and the preview. */
data class ProductFormState(
    val texts: Map<ProductFormField, String> = INITIAL_TEXTS,
    /** Fields flagged after a failed save; a field leaves the set when it is edited. */
    val errors: Set<ProductFormField> = emptySet(),
    /** The depletion date and next alert of the product the fields describe, or null while any field is invalid. */
    val preview: RowStatus? = null,
    /** The texts the form opened with: a new product's defaults, or the stored product's values. */
    val initialTexts: Map<ProductFormField, String> = INITIAL_TEXTS,
    val mode: ProductFormMode = ProductFormMode.New,
    /** The stored product's estimated stock and unit label, shown in the edit form's "Stock" section. */
    val stock: StockSummary? = null,
    /** The restock or adjust dialog being shown, if any. */
    val stockDialog: StockDialog? = null,
) {
    /** Whether any field differs from the values the form opened with. */
    val isDirty: Boolean get() = texts != initialTexts

    /** The fields the form shows, and the keys of [texts] once it is ready: the stock is only asked for when creating a product. */
    val fields: List<ProductFormField>
        get() =
            when (mode) {
                ProductFormMode.New -> ProductFormField.entries
                ProductFormMode.Loading -> emptyList()
                is ProductFormMode.Edit -> ProductFormField.entries - ProductFormField.UnitsNow
            }

    operator fun get(field: ProductFormField): String = texts.getValue(field)

    companion object {
        val INITIAL_TEXTS: Map<ProductFormField, String> =
            ProductFormField.entries.associateWith {
                when (it) {
                    ProductFormField.ConsumptionDays -> "1"
                    ProductFormField.LeadTime -> "7"
                    else -> ""
                }
            }
    }
}

/** The estimated [stock] of a stored product, counted in [unitLabel]. */
data class StockSummary(
    val stock: EstimatedStock,
    val unitLabel: String,
)

/** The limits of the stock dialogs (see the stock-update spec). */
@Suppress("MagicNumber") // Input rules of the spec, written once here.
object StockLimits {
    val packages = 1..99
    val units = 1..1_000_000
    const val UNITS_MAX_LENGTH = 7
}

/** A stock dialog: [stockAfter] is the estimated stock it would result in, or null while its input is invalid. */
sealed interface StockDialog {
    val stockAfter: EstimatedStock?

    /** Restocking [packages] packages, which add [addedUnits] units. */
    data class Restock(
        val packages: Int,
        val addedUnits: Int,
        override val stockAfter: EstimatedStock,
    ) : StockDialog

    /** Adding or, when [remove], removing the whole units typed in [units]. */
    data class Adjust(
        val remove: Boolean,
        val units: String,
        override val stockAfter: EstimatedStock?,
    ) : StockDialog
}

/** Whether the form creates a product or edits a stored one. */
sealed interface ProductFormMode {
    data object New : ProductFormMode

    /** The stored product is being read. */
    data object Loading : ProductFormMode

    /** Editing the stored product, whose name when the form opened is [storedName]. */
    data class Edit(
        val storedName: String,
    ) : ProductFormMode
}

/** One-time effects of the form. */
sealed interface ProductFormEvent {
    /** The product was stored: the form should close. */
    data object Saved : ProductFormEvent

    /** There is nothing to edit any more (the product is missing or was deleted): the form should close. */
    data object Closed : ProductFormEvent

    /** A save was refused: [field] is the first invalid one and should take focus. */
    data class FocusField(
        val field: ProductFormField,
    ) : ProductFormEvent
}

/** What the user can do in the stock section and its dialogs. */
sealed interface StockAction {
    data object OpenRestock : StockAction

    data object OpenAdjust : StockAction

    /** Changes the restock package count by [delta]. */
    data class Step(
        val delta: Int,
    ) : StockAction

    /** Selects "Remove" ([remove]) or "Add" in the adjust dialog. */
    data class SetMode(
        val remove: Boolean,
    ) : StockAction

    /** Sets the units field of the adjust dialog; only its digits are kept. */
    data class SetUnits(
        val text: String,
    ) : StockAction

    data object Dismiss : StockAction

    /** Stores the open dialog's result. */
    data object Confirm : StockAction
}
