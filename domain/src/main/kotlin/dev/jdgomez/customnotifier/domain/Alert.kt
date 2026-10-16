package dev.jdgomez.customnotifier.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** The local date, in an explicit time zone, on which a product runs out. */
@JvmInline
value class DepletionDate(
    val date: LocalDate,
)

/** What is due next for a product's alert, regardless of whether it was already delivered. */
sealed interface NextAlert {
    /** The alert fires at [at]. */
    data class Scheduled(
        val at: ZonedDateTime,
    ) : NextAlert

    /** The alert moment has passed but the product has not run out yet: deliver it now, late. */
    data object Due : NextAlert

    /** The depletion date has passed: nothing to alert. */
    data object None : NextAlert
}

/** The local date in [zone] on which the product's alert fires: the depletion date minus the lead time. */
fun Product.alertDateIn(zone: ZoneId): LocalDate = depletionDateIn(zone).date.minusDays(rule.leadTime.days.toLong())
