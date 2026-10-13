package dev.jdgomez.customnotifier.productform

import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.domain.ProductRepository
import dev.jdgomez.customnotifier.domain.Quantity
import dev.jdgomez.customnotifier.domain.Stock
import dev.jdgomez.customnotifier.productlist.FakeRepository
import dev.jdgomez.customnotifier.productlist.MutableClock
import dev.jdgomez.customnotifier.productlist.RowStatus
import dev.jdgomez.customnotifier.productlist.testProduct
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The stock section and the restock and adjust dialogs of the edit form (the stock-update spec). */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductFormStockViewModelTest {
    private val now = Instant.parse("2026-02-20T10:00:00Z")
    private val repository = FakeRepository()

    /** 20 pills at [now]: 30 recorded ten days ago, consumed at 1 per day. */
    private val stored = testProduct("Vitamin D", now.minus(Duration.ofDays(10)), units = 30, leadDays = 7)

    @After
    fun resetMain() = Dispatchers.resetMain()

    private fun TestScope.editViewModel(product: Product? = stored): ProductFormViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        if (product != null) repository.products.value = listOf(product)
        return ProductFormViewModel(repository, MutableClock(now), stored.id) { ZoneOffset.UTC }
    }

    private fun TestScope.newViewModel(): ProductFormViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return ProductFormViewModel(repository, MutableClock(now)) { ZoneOffset.UTC }
    }

    private suspend fun ProductFormViewModel.nextEventOrNull() = withTimeoutOrNull(1) { events.first() }

    private fun ProductFormViewModel.act(vararg actions: StockAction) = actions.forEach(::onStockAction)

    @Test
    fun editShowsTheEstimatedStockAndNewShowsNone() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            assertEquals(StockSummary(stored.estimatedStockAt(now), "pills"), viewModel.state.value.stock)
            assertEquals(
                20.toBigInteger(),
                viewModel.state.value.stock
                    ?.stock
                    ?.wholeUnits,
            )
            val created = newViewModel()
            created.act(StockAction.OpenRestock, StockAction.OpenAdjust)
            assertNull(created.state.value.stock)
            assertNull(created.state.value.stockDialog)
        }

    @Test
    fun restockDialogStartsAtOnePackageAndShowsTheStockAfter() =
        runTest {
            val viewModel = editViewModel(testProduct("Vitamin D", now, units = 5))
            runCurrent()
            viewModel.act(StockAction.OpenRestock)
            val first = viewModel.state.value.stockDialog as StockDialog.Restock
            assertEquals(1, first.packages)
            assertEquals(30, first.addedUnits)
            assertEquals(35.toBigInteger(), first.stockAfter.wholeUnits)
            viewModel.act(StockAction.Step(1))
            val second = viewModel.state.value.stockDialog as StockDialog.Restock
            assertEquals(Triple(2, 60, 65.toBigInteger()), Triple(second.packages, second.addedUnits, second.stockAfter.wholeUnits))
        }

    @Test
    fun restockCountStaysWithinOneAndNinetyNine() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.act(StockAction.OpenRestock, StockAction.Step(-1))
            assertEquals(1, (viewModel.state.value.stockDialog as StockDialog.Restock).packages)
            viewModel.act(StockAction.Step(98))
            assertEquals(99, (viewModel.state.value.stockDialog as StockDialog.Restock).packages)
            viewModel.act(StockAction.Step(1))
            assertEquals(99, (viewModel.state.value.stockDialog as StockDialog.Restock).packages)
        }

    @Test
    fun confirmedRestockIsStoredAtTheClockMomentAndTheFormStaysOpen() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.act(StockAction.OpenRestock, StockAction.Step(2), StockAction.Confirm)
            runCurrent()
            val saved = repository.products.value.single()
            assertEquals(Stock(Quantity.of(110), now), saved.stock)
            assertNull(viewModel.state.value.stockDialog)
            assertEquals(
                110.toBigInteger(),
                viewModel.state.value.stock
                    ?.stock
                    ?.wholeUnits,
            )
            assertNull(viewModel.nextEventOrNull())
        }

    @Test
    fun cancelledDialogChangesNothing() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.act(StockAction.OpenRestock, StockAction.Step(2), StockAction.Dismiss)
            runCurrent()
            assertNull(viewModel.state.value.stockDialog)
            assertEquals(stored, repository.products.value.single())
        }

    @Test
    fun adjustDialogStartsOnRemoveWithoutUnits() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.act(StockAction.OpenAdjust)
            assertEquals(StockDialog.Adjust(remove = true, units = "", stockAfter = null), viewModel.state.value.stockDialog)
        }

    @Test
    fun adjustStockAfterFollowsModeAndUnitsAndNeverGoesBelowZero() =
        runTest {
            val viewModel = editViewModel(testProduct("Vitamin D", now, units = 2))
            runCurrent()

            fun after() = (viewModel.state.value.stockDialog as StockDialog.Adjust).stockAfter?.wholeUnits
            viewModel.act(StockAction.OpenAdjust, StockAction.SetUnits("1"))
            assertEquals(1.toBigInteger(), after())
            viewModel.act(StockAction.SetUnits("5"))
            assertEquals(0.toBigInteger(), after())
            viewModel.act(StockAction.SetMode(remove = false))
            assertEquals(7.toBigInteger(), after())
        }

    @Test
    fun adjustUnitsKeepOnlyDigitsAndMustBeWithinTheLimits() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()

            fun dialog() = viewModel.state.value.stockDialog as StockDialog.Adjust
            viewModel.act(StockAction.OpenAdjust, StockAction.SetUnits("1a2-"))
            assertEquals("12", dialog().units)
            viewModel.act(StockAction.SetUnits("0"))
            assertNull(dialog().stockAfter)
            viewModel.act(StockAction.SetUnits("1000000"))
            assertEquals(0.toBigInteger(), dialog().stockAfter?.wholeUnits)
            viewModel.act(StockAction.SetUnits("1000001"))
            assertNull(dialog().stockAfter)
            viewModel.act(StockAction.SetUnits("123456789"))
            assertEquals("1234567", dialog().units)
            val before = repository.products.value
            viewModel.act(StockAction.Confirm)
            runCurrent()
            assertEquals(before, repository.products.value)
        }

    @Test
    fun confirmedAdjustmentIsStoredAndClampedAtZero() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.act(StockAction.OpenAdjust, StockAction.SetUnits("5"), StockAction.Confirm)
            runCurrent()
            assertEquals(
                15.toBigInteger(),
                repository.products.value
                    .single()
                    .estimatedStockAt(now)
                    .wholeUnits,
            )
            viewModel.act(StockAction.OpenAdjust, StockAction.SetUnits("999"), StockAction.Confirm)
            runCurrent()
            assertEquals(
                Stock(Quantity.ZERO, now),
                repository.products.value
                    .single()
                    .stock,
            )
        }

    @Test
    fun aStockUpdateKeepsUnsavedEditsAndIsNotAChange() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.act(StockAction.OpenAdjust, StockAction.SetUnits("3"), StockAction.Confirm)
            runCurrent()
            assertFalse(viewModel.state.value.isDirty)
            viewModel.onTextChange(ProductFormField.Name, "Vitamin D3")
            viewModel.act(StockAction.OpenAdjust, StockAction.SetMode(remove = false), StockAction.SetUnits("4"), StockAction.Confirm)
            runCurrent()
            assertEquals("Vitamin D3", viewModel.state.value[ProductFormField.Name])
            assertTrue(viewModel.state.value.isDirty)
            assertEquals(
                "Vitamin D",
                repository.products.value
                    .single()
                    .name.value,
            )
        }

    @Test
    fun saveAfterAStockUpdateKeepsTheUpdatedStock() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.act(StockAction.OpenRestock, StockAction.Confirm)
            runCurrent()
            viewModel.onTextChange(ProductFormField.Name, "Vitamin D3")
            viewModel.onSave()
            runCurrent()
            val saved = repository.products.value.single()
            assertEquals("Vitamin D3", saved.name.value)
            assertEquals(Stock(Quantity.of(50), now), saved.stock)
        }

    @Test
    fun theLivePreviewFollowsTheNewStock() =
        runTest {
            val viewModel = editViewModel(testProduct("Vitamin D", now, units = 5))
            runCurrent()
            val before = (viewModel.state.value.preview as RowStatus.Upcoming).depletionDate
            viewModel.act(StockAction.OpenRestock, StockAction.Confirm)
            runCurrent()
            assertEquals(before.plusDays(30), (viewModel.state.value.preview as RowStatus.Upcoming).depletionDate)
        }

    @Test
    fun confirmOfAProductThatIsGoneClosesTheForm() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.act(StockAction.OpenRestock)
            repository.products.value = emptyList()
            viewModel.act(StockAction.Confirm)
            runCurrent()
            assertEquals(ProductFormEvent.Closed, viewModel.events.first())
            assertTrue(repository.products.value.isEmpty())
        }

    @Test
    fun stockUpdatesAreIgnoredOnceASaveOrDeleteIsUnderWay() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.act(StockAction.OpenRestock)
            viewModel.onSave()
            viewModel.act(StockAction.Confirm, StockAction.OpenAdjust)
            runCurrent()
            assertEquals(stored, repository.products.value.single())
            assertEquals(ProductFormEvent.Saved, viewModel.events.first())
            assertNull(viewModel.nextEventOrNull())
        }

    /** A repository whose reads wait for [gate] while it is set: they stay in flight, as a slow disk would keep them. */
    private inner class GatedRepository : ProductRepository by repository {
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun find(id: ProductId): Product? {
            gate?.await()
            return repository.find(id)
        }
    }

    @Test
    fun aSaveRightAfterConfirmingAStockUpdateKeepsBoth() =
        runTest {
            val gated = GatedRepository()
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            repository.products.value = listOf(stored)
            val viewModel = ProductFormViewModel(gated, MutableClock(now), stored.id) { ZoneOffset.UTC }
            runCurrent()
            gated.gate = CompletableDeferred()

            viewModel.act(StockAction.OpenRestock, StockAction.Step(1), StockAction.Confirm)
            viewModel.onTextChange(ProductFormField.Name, "Vitamin D3")
            viewModel.onSave()
            runCurrent()
            gated.gate?.complete(Unit)
            runCurrent()

            val saved = repository.products.value.single()
            assertEquals("Vitamin D3", saved.name.value)
            assertEquals(Stock(Quantity.of(80), now), saved.stock)
            assertEquals(ProductFormEvent.Saved, viewModel.events.first())
        }

    @Test
    fun aDeleteRightAfterConfirmingAStockUpdateStillDeletes() =
        runTest {
            val gated = GatedRepository()
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            repository.products.value = listOf(stored)
            val viewModel = ProductFormViewModel(gated, MutableClock(now), stored.id) { ZoneOffset.UTC }
            runCurrent()
            gated.gate = CompletableDeferred()

            viewModel.act(StockAction.OpenRestock, StockAction.Confirm)
            viewModel.onDelete()
            runCurrent()
            gated.gate?.complete(Unit)
            runCurrent()

            assertTrue(repository.products.value.isEmpty())
            assertEquals(ProductFormEvent.Closed, viewModel.events.first())
        }

    @Test
    fun theTickRecomputesTheStockTheDialogAndThePreviewFromTheCurrentMoment() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val clock = MutableClock(now)
            repository.products.value = listOf(stored)
            val viewModel = ProductFormViewModel(repository, clock, stored.id) { ZoneOffset.UTC }
            runCurrent()
            viewModel.act(StockAction.OpenAdjust, StockAction.SetUnits("3"))
            assertEquals(
                20.toBigInteger(),
                viewModel.state.value.stock
                    ?.stock
                    ?.wholeUnits,
            )
            assertEquals(
                17.toBigInteger(),
                viewModel.state.value.stockDialog
                    ?.stockAfter
                    ?.wholeUnits,
            )

            clock.now = now.plus(Duration.ofDays(2))
            viewModel.onTick()

            assertEquals(
                18.toBigInteger(),
                viewModel.state.value.stock
                    ?.stock
                    ?.wholeUnits,
            )
            assertEquals(
                15.toBigInteger(),
                viewModel.state.value.stockDialog
                    ?.stockAfter
                    ?.wholeUnits,
            )
            assertEquals("3", (viewModel.state.value.stockDialog as StockDialog.Adjust).units)
        }

    @Test
    fun actionsThatDoNotApplyToTheOpenDialogAreIgnored() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.act(StockAction.Step(1), StockAction.SetUnits("4"), StockAction.SetMode(remove = false), StockAction.Confirm)
            assertNull(viewModel.state.value.stockDialog)

            viewModel.act(StockAction.OpenRestock, StockAction.SetUnits("4"), StockAction.SetMode(remove = false), StockAction.OpenAdjust)
            assertEquals(1, (viewModel.state.value.stockDialog as StockDialog.Restock).packages)

            viewModel.act(StockAction.Dismiss, StockAction.OpenAdjust, StockAction.Step(1), StockAction.OpenRestock)
            assertEquals(StockDialog.Adjust(remove = true, units = "", stockAfter = null), viewModel.state.value.stockDialog)
            runCurrent()
            assertEquals(stored, repository.products.value.single())
        }
}
