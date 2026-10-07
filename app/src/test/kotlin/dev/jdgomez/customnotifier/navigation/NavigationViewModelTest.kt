package dev.jdgomez.customnotifier.navigation

import org.junit.Test
import kotlin.test.assertEquals

class NavigationViewModelTest {
    @Test
    fun startsOnTheProductListWhichBackDoesNotPop() {
        val model = NavigationViewModel()
        model.pop()
        assertEquals(listOf(Destination.ProductList), model.backStack.toList())
    }

    @Test
    fun pushOpensAScreenAndPopReturns() {
        val model = NavigationViewModel()
        model.push(Destination.NewProduct)
        assertEquals(listOf(Destination.ProductList, Destination.NewProduct), model.backStack.toList())
        model.pop()
        assertEquals(listOf(Destination.ProductList), model.backStack.toList())
    }

    @Test
    fun aNewProcessStartsOnTheListBecauseNothingIsRestored() {
        NavigationViewModel().push(Destination.NewProduct)
        assertEquals(listOf(Destination.ProductList), NavigationViewModel().backStack.toList())
    }
}
