package dev.jdgomez.customnotifier.productlist

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.jdgomez.customnotifier.alerts.NotificationAccess
import dev.jdgomez.customnotifier.alerts.NotificationAccessState
import dev.jdgomez.customnotifier.calendar.CalendarAccess
import dev.jdgomez.customnotifier.calendar.CalendarProblem
import dev.jdgomez.customnotifier.calendar.ChannelCheck
import dev.jdgomez.customnotifier.calendar.checkChannel
import dev.jdgomez.customnotifier.customNotifierContainer
import dev.jdgomez.customnotifier.domain.CalendarChannel
import dev.jdgomez.customnotifier.domain.CalendarChannelSettings
import dev.jdgomez.customnotifier.domain.CalendarGateway
import dev.jdgomez.customnotifier.domain.ProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds

private enum class RequestOrigin { Save, Banner }

private val REFRESH_PERIOD = 60.seconds

/**
 * The product list state: the stored products recomputed on every change and every minute, only while
 * someone collects [state] (the screen between STARTED and STOPPED), so nothing runs in the background.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModel(
    repository: ProductRepository,
    clock: Clock,
    zone: () -> ZoneId,
    private val access: NotificationAccess,
    private val calendar: CalendarChannelWatch,
    private val reconcileAlerts: () -> Unit,
) : ViewModel() {
    private val accessState = MutableStateFlow(access.current())
    private val calendarProblem = MutableStateFlow<CalendarProblem?>(null)
    private val recheck = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(calendar.settings.observe(), recheck) { channel, _ -> channel }
                .mapLatest { calendar.problemOf(it) }
                .collect { calendarProblem.value = it }
        }
    }

    val state: StateFlow<ProductListState> =
        combine(repository.observeAll(), ticker(), accessState, calendarProblem) { products, _, access, problem ->
            val list = products.toListState(clock.instant(), zone())
            if (list is ProductListState.Products) {
                list.copy(alertsOff = access != NotificationAccessState.Allowed, calendarProblem = problem)
            } else {
                list
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), ProductListState.Loading)

    /** Checks the calendar channel again; the screen calls it whenever it becomes visible. */
    fun refreshCalendar() {
        recheck.update { it + 1 }
    }

    /** Reads the notification access again; alerts that became due while it was off are delivered when it turns on. */
    fun refreshAccess(): NotificationAccessState {
        val before = accessState.value
        val now = access.current()
        accessState.value = now
        // Nothing is left to ask for, so a request that never reported back cannot keep the next one from starting.
        if (now != NotificationAccessState.PermissionMissing) inFlight = null
        if (before != NotificationAccessState.Allowed && now == NotificationAccessState.Allowed) reconcileAlerts()
        return now
    }

    // The origin of the request in flight, if any: it travels with the request, so a later one cannot overwrite it.
    private var inFlight: RequestOrigin? = null

    /**
     * The screen is about to show the system dialog; [userInitiated] tells the banner's "Turn on" from the request after a save.
     * Returns false while another request is in flight: the screen must not start a second one.
     */
    fun startRequest(userInitiated: Boolean): Boolean {
        if (inFlight != null) return false
        inFlight = if (userInitiated) RequestOrigin.Banner else RequestOrigin.Save
        return true
    }

    /**
     * The system dialog was answered or interrupted; [explanationDue] is the rationale flag after the request. The access is read
     * again, not taken from the result. Returns true when the screen must open the notification settings: the request came from
     * the banner and Android will not show an explanation, so only the settings can turn alerts on.
     */
    fun onPermissionResult(explanationDue: Boolean): Boolean {
        val origin = inFlight
        inFlight = null
        return refreshAccess() == NotificationAccessState.PermissionMissing && origin == RequestOrigin.Banner && !explanationDue
    }

    fun settingsIntent(): Intent = access.settingsIntent(accessState.value)

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val container = customNotifierContainer()
                    ProductListViewModel(
                        container.productRepository,
                        container.clock,
                        container.zone,
                        container.notificationAccess,
                        CalendarChannelWatch(
                            container.calendarChannelSettings,
                            container.calendarGateway,
                            container.calendarAccess,
                        ),
                    ) { container.reconcileAlerts {} }
                }
            }
    }
}

/** Emits at once and then every [REFRESH_PERIOD]; the product form refreshes its stock with it too. */
internal fun ticker() =
    flow {
        while (true) {
            emit(Unit)
            delay(REFRESH_PERIOD)
        }
    }

/** What the product list needs to judge the calendar channel: the same inputs as the Settings screen. */
class CalendarChannelWatch(
    val settings: CalendarChannelSettings,
    private val gateway: CalendarGateway,
    private val access: CalendarAccess,
) {
    /** The problem of the channel, or null when it is off or works. A failing gateway counts as no problem found. */
    @Suppress("TooGenericExceptionCaught") // Any failure of an adapter must leave the list usable.
    suspend fun problemOf(channel: CalendarChannel): CalendarProblem? =
        try {
            (checkChannel(channel, access, gateway) as? ChannelCheck.Broken)?.problem
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
}
