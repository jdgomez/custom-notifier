package dev.jdgomez.customnotifier.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.productform.ProductFormRoute
import dev.jdgomez.customnotifier.productform.ProductFormViewModel
import dev.jdgomez.customnotifier.productlist.ProductListRoute

/** The screens of the app. */
sealed interface Destination {
    data object ProductList : Destination

    data object NewProduct : Destination

    data class EditProduct(
        val id: ProductId,
    ) : Destination
}

/**
 * The back stack, held in memory by the activity: it survives configuration changes but not process
 * death, after which the app starts again on the product list (ADR 0016).
 */
class NavigationViewModel : ViewModel() {
    val backStack = mutableStateListOf<Destination>(Destination.ProductList)

    fun push(destination: Destination) {
        backStack.add(destination)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}

@Composable
fun AppNavigation(model: NavigationViewModel = viewModel()) {
    NavDisplay(
        backStack = model.backStack,
        onBack = model::pop,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        entryProvider =
            entryProvider {
                entry<Destination.ProductList> {
                    ProductListRoute(
                        onAddProduct = { model.push(Destination.NewProduct) },
                        onEditProduct = { model.push(Destination.EditProduct(it)) },
                    )
                }
                entry<Destination.NewProduct> { ProductFormRoute(onClose = model::pop) }
                entry<Destination.EditProduct> { key ->
                    ProductFormRoute(onClose = model::pop, viewModel = viewModel(factory = ProductFormViewModel.factory(key.id)))
                }
            },
    )
}
