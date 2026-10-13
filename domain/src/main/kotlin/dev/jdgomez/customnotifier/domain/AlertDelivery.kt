package dev.jdgomez.customnotifier.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/** Port to the system alarms: at most one pending alarm per product. */
interface AlertScheduler {
    /** Makes [at] the only pending alarm of the product, replacing an earlier one. */
    fun schedule(
        id: ProductId,
        at: Instant,
    )

    /** Removes the product's pending alarm; does nothing when there is none. */
    fun cancel(id: ProductId)
}

/** Port to the user-visible alert notification. */
interface AlertNotifier {
    /**
     * Shows the notification of [product], which runs out on [depletionDate] ([today] is the current local date).
     * Returns false, showing nothing, when notifications are disabled.
     */
    fun notify(
        product: Product,
        depletionDate: DepletionDate,
        today: LocalDate,
    ): Boolean
}

/** Port to the stored record of which products already had their current alert delivered. */
interface AlertDeliveries {
    suspend fun isDelivered(id: ProductId): Boolean

    suspend fun record(id: ProductId)

    suspend fun clear(id: ProductId)
}

/** What to do for one product, from its [NextAlert] and whether that alert was already delivered. */
sealed interface AlertAction {
    /** Have exactly one alarm at [at] and clear the delivery record, so the alert can fire again. */
    data class Schedule(
        val at: ZonedDateTime,
    ) : AlertAction

    /** Post the notification and record the delivery. */
    data object Deliver : AlertAction

    /** Nothing to do; any pending alarm is not needed. */
    data object Skip : AlertAction

    companion object {
        fun decide(
            next: NextAlert,
            delivered: Boolean,
        ): AlertAction =
            when (next) {
                is NextAlert.Scheduled -> Schedule(next.at)
                NextAlert.Due -> if (delivered) Skip else Deliver
                NextAlert.None -> Skip
            }
    }
}
