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
    fun refreshAccess(backFromSystemSettings: Boolean = false): NotificationAccessState {
        // The user may have reset the permission to "ask every time" there, so a recorded denial is forgotten.
        if (backFromSystemSettings) access.clearPermanentDenial()
        val before = accessState.value
        val now = access.current()
        accessState.value = now
        if (before != NotificationAccessState.Allowed && now == NotificationAccessState.Allowed) reconcileAlerts()
        return now
    }

    /**
     * The system dialog was answered; [explanationBefore] and [explanationAfter] are the rationale flags around the request.
     * Returns true when Android showed no dialog for it: the screen then opens the notification settings.
     */
    fun onPermissionResult(
        granted: Boolean,
        explanationBefore: Boolean,
        explanationAfter: Boolean,
    ): Boolean {
        val noDialog = !granted && access.recordDenied(explanationBefore, explanationAfter)
        refreshAccess()
        return noDialog
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
