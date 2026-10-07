package dev.jdgomez.customnotifier.productform

import dev.jdgomez.customnotifier.domain.ConsumptionRate
import dev.jdgomez.customnotifier.domain.LeadTime
import dev.jdgomez.customnotifier.domain.PackageSize
import dev.jdgomez.customnotifier.domain.ProductName
import dev.jdgomez.customnotifier.domain.UnitLabel
import dev.jdgomez.customnotifier.productlist.FakeRepository
import dev.jdgomez.customnotifier.productlist.MutableClock
import dev.jdgomez.customnotifier.productlist.RowStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test
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
}
