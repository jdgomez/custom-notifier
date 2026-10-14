package dev.jdgomez.customnotifier.productlist

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.jdgomez.customnotifier.alerts.NotificationAccess
import dev.jdgomez.customnotifier.alerts.NotificationAccessState
import dev.jdgomez.customnotifier.customNotifierContainer
import dev.jdgomez.customnotifier.domain.ProductRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds

private enum class RequestOrigin { Save, Banner }

private val REFRESH_PERIOD = 60.seconds

/**
 * The product list state: the stored products recomputed on every change and every minute, only while
 * someone collects [state] (the screen between STARTED and STOPPED), so nothing runs in the background.
 */
class ProductListViewModel(
    repository: ProductRepository,
    clock: Clock,
    zone: () -> ZoneId,
    private val access: NotificationAccess,
    private val reconcileAlerts: () -> Unit,
) : ViewModel() {
    private val accessState = MutableStateFlow(access.current())

    val state: StateFlow<ProductListState> =
        combine(repository.observeAll(), ticker(), accessState) { products, _, access ->
            val list = products.toListState(clock.instant(), zone())
            if (list is ProductListState.Products) list.copy(alertsOff = access != NotificationAccessState.Allowed) else list
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), ProductListState.Loading)

    /** Reads the notification access again; alerts that became due while it was off are delivered when it turns on. */
    fun refreshAccess(): NotificationAccessState {
        val before = accessState.value
        val now = access.current()
        accessState.value = now
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
