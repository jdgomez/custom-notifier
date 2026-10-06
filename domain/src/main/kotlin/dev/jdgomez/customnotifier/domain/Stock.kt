package dev.jdgomez.customnotifier.domain

import java.time.Duration
import java.time.Instant

private const val MILLIS_PER_DAY = 86_400_000L

/** The exact estimated stock at a moment, with its whole-unit view. */
data class EstimatedStock(
    val exact: Quantity,
) {
    /** The estimate rounded down to whole units. */
    val wholeUnits: Long get() = exact.floor().longValueExact()

    /** True when [wholeUnits] differs from [exact] because the exact value is not whole. */
    val rounded: Boolean get() = !exact.isWhole
}

/** The stock last recorded: an exact quantity of units as of [recordedAt]. */
data class Stock(
    val units: Quantity,
    val recordedAt: Instant,
) {
    /** The estimate at [now] under [rate]; never below zero, and [now] before [recordedAt] counts as no time elapsed. */
    fun estimateAt(
        now: Instant,
        rate: ConsumptionRate,
    ): EstimatedStock {
        val elapsedMillis = Duration.between(recordedAt, now).toMillis().coerceAtLeast(0)
        val consumed = Quantity.of(rate.units * elapsedMillis) / (rate.days * MILLIS_PER_DAY)
        return EstimatedStock((units - consumed).coerceAtLeast(Quantity.ZERO))
    }
}
