package dev.jdgomez.customnotifier.productlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.jdgomez.customnotifier.customNotifierContainer
import dev.jdgomez.customnotifier.domain.ProductRepository
import kotlinx.coroutines.delay
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
) : ViewModel() {
    val state: StateFlow<ProductListState> =
        combine(repository.observeAll(), ticker()) { products, _ -> products.toListState(clock.instant(), zone()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), ProductListState.Loading)

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val container = customNotifierContainer()
                    ProductListViewModel(container.productRepository, container.clock, container.zone)
                }
            }
    }
}

/** Emits at once and then every [REFRESH_PERIOD]. */
private fun ticker() =
    flow {
        while (true) {
            emit(Unit)
            delay(REFRESH_PERIOD)
        }
    }
