package dev.jdgomez.customnotifier.productform

import dev.jdgomez.customnotifier.domain.ConsumptionRate
import dev.jdgomez.customnotifier.domain.LeadTime
import dev.jdgomez.customnotifier.domain.PackageSize
import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.domain.ProductName
import dev.jdgomez.customnotifier.domain.Quantity
import dev.jdgomez.customnotifier.domain.Stock
import dev.jdgomez.customnotifier.domain.UnitLabel
import dev.jdgomez.customnotifier.productlist.FakeRepository
import dev.jdgomez.customnotifier.productlist.MutableClock
import dev.jdgomez.customnotifier.productlist.RowStatus
import dev.jdgomez.customnotifier.productlist.testProduct
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
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.jdgomez.customnotifier.domain.Rule as AlertRule

@OptIn(ExperimentalCoroutinesApi::class)
class ProductFormViewModelTest {
    private val now = Instant.parse("2026-02-20T10:00:00Z")
    private val repository = FakeRepository()

    @After
    fun resetMain() = Dispatchers.resetMain()

    private fun kotlinx.coroutines.test.TestScope.viewModel(): ProductFormViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return ProductFormViewModel(repository, MutableClock(now)) { ZoneOffset.UTC }
    }

    private fun ProductFormViewModel.fill(
        name: String = "Vitamin D",
        unit: String = "pills",
        packageSize: String = "90",
        units: String = "1",
        days: String = "1",
        lead: String = "7",
        now: String = "30",
    ) = mapOf(
        ProductFormField.Name to name,
        ProductFormField.Unit to unit,
        ProductFormField.PackageSize to packageSize,
        ProductFormField.ConsumptionUnits to units,
        ProductFormField.ConsumptionDays to days,
        ProductFormField.LeadTime to lead,
        ProductFormField.UnitsNow to now,
    ).forEach { (field, text) -> onTextChange(field, text) }

    @Test
    fun startsWithTheSpecifiedDefaults() =
        runTest {
            val state = viewModel().state.value
            ProductFormField.entries.forEach {
                val expected = mapOf(ProductFormField.ConsumptionDays to "1", ProductFormField.LeadTime to "7")[it] ?: ""
                assertEquals(expected, state[it], it.name)
            }
            assertFalse(state.isDirty)
            assertNull(state.preview)
        }

    @Test
    fun numberFieldsKeepOnlyDigits() =
        runTest {
            val viewModel = viewModel()
            viewModel.onTextChange(ProductFormField.PackageSize, "-2.5")
            assertEquals("25", viewModel.state.value[ProductFormField.PackageSize])
        }

    @Test
    fun textLengthsAreCapped() =
        runTest {
            val viewModel = viewModel()
            viewModel.onTextChange(ProductFormField.Name, "a".repeat(61))
            viewModel.onTextChange(ProductFormField.Unit, "b".repeat(21))
            assertEquals("a".repeat(60), viewModel.state.value[ProductFormField.Name])
            assertEquals("b".repeat(20), viewModel.state.value[ProductFormField.Unit])
        }

    @Test
    fun dirtyOnlyWhileAFieldDiffersFromItsInitialValue() =
        runTest {
            val viewModel = viewModel()
            viewModel.onTextChange(ProductFormField.Name, "x")
            assertTrue(viewModel.state.value.isDirty)
            viewModel.onTextChange(ProductFormField.Name, "")
            assertFalse(viewModel.state.value.isDirty)
            viewModel.onTextChange(ProductFormField.LeadTime, "8")
            assertTrue(viewModel.state.value.isDirty)
        }

    @Test
    fun saveFlagsEveryInvalidFieldFocusesTheFirstAndStoresNothing() =
        runTest {
            val viewModel = viewModel()
            viewModel.fill(name = "   ", packageSize = "")
            viewModel.onSave()
            runCurrent()
            assertEquals(setOf(ProductFormField.Name, ProductFormField.PackageSize), viewModel.state.value.errors)
            assertEquals(ProductFormEvent.FocusField(ProductFormField.Name), viewModel.events.first())
            assertTrue(repository.products.value.isEmpty())
        }

    @Test
    fun outOfRangeAndEmptyValuesAreInvalid() =
        runTest {
            val viewModel = viewModel()
            viewModel.fill(units = "100001", days = "400", lead = "366", now = "1000001")
            viewModel.onSave()
            assertEquals(
                setOf(
                    ProductFormField.ConsumptionUnits,
                    ProductFormField.ConsumptionDays,
                    ProductFormField.LeadTime,
                    ProductFormField.UnitsNow,
                ),
                viewModel.state.value.errors,
            )
            viewModel.fill(unit = " ", units = "0", days = "0", lead = "", now = "")
            viewModel.onSave()
            assertEquals(
                setOf(
                    ProductFormField.Unit,
                    ProductFormField.ConsumptionUnits,
                    ProductFormField.ConsumptionDays,
                    ProductFormField.LeadTime,
                    ProductFormField.UnitsNow,
                ),
                viewModel.state.value.errors,
            )
        }

    @Test
    fun limitsAreAcceptedAtTheirEdges() =
        runTest {
            val viewModel = viewModel()
            viewModel.fill(packageSize = "100000", units = "100000", days = "365", lead = "0", now = "1000000")
            viewModel.onSave()
            runCurrent()
            assertEquals(1, repository.products.value.size)
        }

    @Test
    fun editingAFieldClearsOnlyItsError() =
        runTest {
            val viewModel = viewModel()
            viewModel.onSave()
            viewModel.onTextChange(ProductFormField.Name, "V")
            val errors = viewModel.state.value.errors
            assertFalse(ProductFormField.Name in errors)
            assertTrue(ProductFormField.Unit in errors)
        }

    @Test
    fun previewShowsTheSpecifiedDatesAndHidesWhileIncomplete() =
        runTest {
            val viewModel = viewModel()
            viewModel.fill(now = "")
            assertNull(viewModel.state.value.preview)
            viewModel.fill(units = "1", days = "1", lead = "10", now = "20")
            assertEquals(RowStatus.Upcoming(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 2)), viewModel.state.value.preview)
            viewModel.onTextChange(ProductFormField.LeadTime, "30")
            assertEquals(RowStatus.Upcoming(LocalDate.of(2026, 3, 12), null), viewModel.state.value.preview)
        }

    @Test
    fun saveStoresTheProductAndSignalsSaved() =
        runTest {
            val viewModel = viewModel()
            viewModel.fill(name = " Vitamin D ")
            viewModel.onSave()
            runCurrent()
            assertEquals(ProductFormEvent.Saved, viewModel.events.first())
            val product = repository.products.value.single()
            assertEquals(ProductName("Vitamin D"), product.name)
            assertEquals(UnitLabel("pills"), product.unitLabel)
            assertEquals(PackageSize(90), product.packageSize)
            assertEquals(ConsumptionRate(1, 1), product.consumptionRate)
            assertEquals(AlertRule(LeadTime(7)), product.rule)
            assertEquals(30.toBigInteger(), product.estimatedStockAt(now).wholeUnits)
        }

    @Test
    fun duplicateNamesAreBothStored() =
        runTest {
            repeat(2) {
                val viewModel = viewModel()
                viewModel.fill()
                viewModel.onSave()
                runCurrent()
            }
            assertEquals(listOf("Vitamin D", "Vitamin D"), repository.products.value.map { it.name.value })
        }

    private fun TestScope.editViewModel(product: Product? = stored): ProductFormViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        if (product != null) repository.products.value = listOf(product)
        return ProductFormViewModel(repository, MutableClock(now), (stored).id) { ZoneOffset.UTC }
    }

    private val stored = testProduct("Vitamin D", now.minus(Duration.ofDays(10)), units = 30, leadDays = 7)

    @Test
    fun editLoadsTheStoredValuesWithoutTheStock() =
        runTest {
            val viewModel = editViewModel()
            assertEquals(ProductFormMode.Loading, viewModel.state.value.mode)
            runCurrent()
            val state = viewModel.state.value
            assertEquals(ProductFormMode.Edit("Vitamin D"), state.mode)
            assertEquals("Vitamin D", state[ProductFormField.Name])
            assertEquals("pills", state[ProductFormField.Unit])
            assertEquals("30", state[ProductFormField.PackageSize])
            assertEquals("1", state[ProductFormField.ConsumptionUnits])
            assertEquals("1", state[ProductFormField.ConsumptionDays])
            assertEquals("7", state[ProductFormField.LeadTime])
            assertFalse(ProductFormField.UnitsNow in state.fields)
            assertFalse(state.isDirty)
        }

    @Test
    fun editOfAMissingProductCloses() =
        runTest {
            val viewModel = editViewModel(null)
            runCurrent()
            assertEquals(ProductFormEvent.Closed, viewModel.events.first())
        }

    @Test
    fun editIsDirtyAgainstTheStoredValues() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.onTextChange(ProductFormField.Name, "Vitamin D3")
            assertTrue(viewModel.state.value.isDirty)
            viewModel.onTextChange(ProductFormField.Name, "Vitamin D")
            assertFalse(viewModel.state.value.isDirty)
        }

    @Test
    fun editPreviewStartsFromTheCurrentStock() =
        runTest {
            val viewModel = editViewModel(testProduct("Vitamin D", now, units = 20, leadDays = 10))
            runCurrent()
            viewModel.onTextChange(ProductFormField.LeadTime, "5")
            assertEquals(RowStatus.Upcoming(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 7)), viewModel.state.value.preview)
        }

    @Test
    fun editSaveWithoutAStockFieldFlagsOnlyVisibleFields() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.onTextChange(ProductFormField.Name, "")
            viewModel.onSave()
            runCurrent()
            assertEquals(setOf(ProductFormField.Name), viewModel.state.value.errors)
            assertEquals(ProductFormEvent.FocusField(ProductFormField.Name), viewModel.events.first())
            assertEquals(stored, repository.products.value.single())
        }

    @Test
    fun editSaveRenamesAndKeepsTheStockWhenTheRateIsUnchanged() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.onTextChange(ProductFormField.Name, "Vitamin D3")
            viewModel.onSave()
            runCurrent()
            assertEquals(ProductFormEvent.Saved, viewModel.events.first())
            val saved = repository.products.value.single()
            assertEquals(stored.copy(name = ProductName("Vitamin D3")), saved)
        }

    @Test
    fun editSaveWithANewRateKeepsThePastConsumption() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.onTextChange(ProductFormField.ConsumptionUnits, "2")
            viewModel.onSave()
            runCurrent()
            val saved = repository.products.value.single()
            assertEquals(20.toBigInteger(), saved.estimatedStockAt(now).wholeUnits)
            assertEquals(LocalDate.of(2026, 3, 2), saved.depletionDateIn(ZoneOffset.UTC).date)
        }

    @Test
    fun deleteRemovesTheStoredProductAndCloses() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.onTextChange(ProductFormField.Name, "Changed")
            viewModel.onDelete()
            runCurrent()
            assertEquals(ProductFormEvent.Closed, viewModel.events.first())
            assertTrue(repository.products.value.isEmpty())
        }

    private suspend fun ProductFormViewModel.nextEventOrNull() = withTimeoutOrNull(1) { events.first() }

    @Test
    fun saveWhileLoadingDoesNothing() =
        runTest {
            val viewModel = editViewModel()
            viewModel.onSave()
            assertTrue(
                viewModel.state.value.errors
                    .isEmpty(),
            )
            assertNull(viewModel.nextEventOrNull())
            runCurrent()
            assertEquals(ProductFormMode.Edit("Vitamin D"), viewModel.state.value.mode)
            assertTrue(
                viewModel.state.value.errors
                    .isEmpty(),
            )
            assertNull(viewModel.nextEventOrNull())
            assertEquals(stored, repository.products.value.single())
        }

    @Test
    fun editTextsDoNotHoldTheHiddenStockField() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            assertFalse(ProductFormField.UnitsNow in viewModel.state.value.texts)
            assertEquals(ProductFormField.entries - ProductFormField.UnitsNow, viewModel.state.value.fields)
        }

    @Test
    fun deleteTwiceClosesOnce() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.onDelete()
            viewModel.onDelete()
            runCurrent()
            assertEquals(ProductFormEvent.Closed, viewModel.events.first())
            assertNull(viewModel.nextEventOrNull())
        }

    @Test
    fun deleteAfterSaveIsIgnored() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.onTextChange(ProductFormField.Name, "Vitamin D3")
            viewModel.onSave()
            viewModel.onDelete()
            runCurrent()
            assertEquals(ProductFormEvent.Saved, viewModel.events.first())
            assertNull(viewModel.nextEventOrNull())
            assertEquals(
                "Vitamin D3",
                repository.products.value
                    .single()
                    .name.value,
            )
        }

    @Test
    fun saveAfterDeleteDoesNotResurrectTheProduct() =
        runTest {
            val viewModel = editViewModel()
            runCurrent()
            viewModel.onTextChange(ProductFormField.Name, "Vitamin D3")
            viewModel.onDelete()
            viewModel.onSave()
            runCurrent()
            assertEquals(ProductFormEvent.Closed, viewModel.events.first())
            assertNull(viewModel.nextEventOrNull())
            assertTrue(repository.products.value.isEmpty())
        }

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
            val created = viewModel()
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
}
