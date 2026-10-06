package dev.jdgomez.customnotifier.domain

/** The product name: free text, trimmed, not blank. */
@JvmInline
value class ProductName private constructor(
    val value: String,
) {
    companion object {
        operator fun invoke(raw: String): ProductName {
            require(raw.isNotBlank()) { "name must not be blank" }
            return ProductName(raw.trim())
        }
    }
}

/** The label of the unit the product is counted in (for example "pill"). Not named Unit: that is a Kotlin type. */
@JvmInline
value class UnitLabel private constructor(
    val value: String,
) {
    companion object {
        operator fun invoke(raw: String): UnitLabel {
            require(raw.isNotBlank()) { "unitLabel must not be blank" }
            return UnitLabel(raw.trim())
        }
    }
}

/** The number of units in one package. */
@JvmInline
value class PackageSize(
    val units: Int,
) {
    init {
        require(units > 0) { "packageSize must be positive" }
    }
}

/** N [units] consumed every M [days]. */
data class ConsumptionRate(
    val units: Int,
    val days: Int,
) {
    init {
        require(units > 0) { "consumptionRate.units must be positive" }
        require(days > 0) { "consumptionRate.days must be positive" }
    }
}
