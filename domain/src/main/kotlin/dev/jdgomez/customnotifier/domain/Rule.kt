package dev.jdgomez.customnotifier.domain

/** How many whole days before the depletion date the alert fires: 0 (on the depletion date itself) to 365. */
@JvmInline
value class LeadTime(
    val days: Int,
) {
    init {
        require(days in 0..MAX_DAYS) { "leadTime must be between 0 and $MAX_DAYS days" }
    }

    private companion object {
        const val MAX_DAYS = 365
    }
}

/** When to alert the user about a product. One per product. */
data class Rule(
    val leadTime: LeadTime,
)
