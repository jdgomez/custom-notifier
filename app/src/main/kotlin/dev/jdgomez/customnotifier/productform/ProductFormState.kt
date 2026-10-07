package dev.jdgomez.customnotifier.productform

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
) {
    /** Whether any field differs from the values the form opened with. */
    val isDirty: Boolean get() = texts != initialTexts

    /** The fields the form shows: the stock is only asked for when creating a product. */
    val fields: List<ProductFormField>
        get() = if (mode is ProductFormMode.New) ProductFormField.entries else ProductFormField.entries - ProductFormField.UnitsNow

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
