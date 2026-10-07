package dev.jdgomez.customnotifier.productform

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.jdgomez.customnotifier.productlist.FakeRepository
import dev.jdgomez.customnotifier.productlist.MutableClock
import dev.jdgomez.customnotifier.productlist.testProduct
import dev.jdgomez.customnotifier.ui.CustomNotifierTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w360dp-h800dp-xhdpi")
class ProductFormScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeRepository()
    private var closed = 0

    private fun show() {
        val viewModel = ProductFormViewModel(repository, MutableClock(Instant.parse("2026-02-20T10:00:00Z"))) { ZoneOffset.UTC }
        composeRule.setContent { CustomNotifierTheme { ProductFormRoute(onClose = { closed++ }, viewModel = viewModel) } }
    }

    private val stored = testProduct("Vitamin D", Instant.parse("2026-02-20T10:00:00Z"), units = 90)

    private fun showEdit() {
        repository.products.value = listOf(stored)
        val viewModel = ProductFormViewModel(repository, MutableClock(Instant.parse("2026-02-20T10:00:00Z")), stored.id) { ZoneOffset.UTC }
        composeRule.setContent { CustomNotifierTheme { ProductFormRoute(onClose = { closed++ }, viewModel = viewModel) } }
        composeRule.waitForIdle()
    }

    private fun type(
        field: ProductFormField,
        text: String,
    ) = composeRule.onNodeWithTag(field.name).performTextReplacement(text)

    private fun fillValid() {
        type(ProductFormField.Name, "Vitamin D")
        type(ProductFormField.Unit, "pills")
        type(ProductFormField.PackageSize, "90")
        type(ProductFormField.ConsumptionUnits, "1")
        type(ProductFormField.UnitsNow, "30")
    }

    @Test
    fun fieldsAreInTheSpecifiedOrderWithTheirDefaults() {
        show()
        val tops = ProductFormField.entries.map { composeRule.onNodeWithTag(it.name).getUnclippedBoundsInRoot() }
        // The two number fields of a line share a row: order is by row, then by position.
        assertEquals(tops, tops.sortedWith(compareBy({ it.top.value.toInt() / 10 }, { it.left })))
        composeRule.onNodeWithTag("ConsumptionDays").assertTextEquals("1")
        composeRule.onNodeWithTag("LeadTime").assertTextEquals("7")
        composeRule.onNodeWithTag("Name").assertTextEquals("Name", "")
        composeRule.onNodeWithText("0 alerts on the day it runs out").assertIsDisplayed()
        composeRule.onNodeWithText("What you count, as it will be shown: pills, diapers, ml").assertIsDisplayed()
    }

    @Test
    fun consumptionLineFollowsTheUnit() {
        show()
        composeRule.onNodeWithText("units every").assertIsDisplayed()
        type(ProductFormField.Unit, "pills")
        composeRule.onNodeWithText("pills every").assertIsDisplayed()
    }

    @Test
    fun saveWithInvalidFieldsShowsMessagesAndFocusesTheFirst() {
        show()
        type(ProductFormField.Name, "   ")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Enter a name").assertIsDisplayed()
        composeRule.onNodeWithText("Enter a unit").assertIsDisplayed()
        // Units per package and the consumption units share the message.
        composeRule.onAllNodesWithText("Enter a whole number from 1 to 100,000").assertCountEquals(2)
        composeRule.onNodeWithText("Enter a whole number from 0 to 1,000,000").assertIsDisplayed()
        composeRule.onNodeWithTag("Name").assertIsFocused()
        assertTrue(repository.products.value.isEmpty())
    }

    @Test
    fun anErrorDisappearsWhenItsFieldIsEdited() {
        show()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithTag("Name").performTextInput("V")
        composeRule.onNodeWithText("Enter a name").assertDoesNotExist()
        composeRule.onNodeWithText("Enter a unit").assertIsDisplayed()
    }

    @Test
    fun previewAppearsOnlyWhenEveryFieldIsValid() {
        show()
        composeRule.onNodeWithTag("preview").assertDoesNotExist()
        fillValid()
        composeRule.onNodeWithTag("preview").assertTextEquals("Runs out Mar 22, 2026 · Alert Mar 15, 2026")
    }

    @Test
    fun savingStoresTheProductAndCloses() {
        show()
        fillValid()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("Vitamin D"), repository.products.value.map { it.name.value })
        assertEquals(1, closed)
    }

    @Test
    fun upArrowWithInputAsksToDiscardAndKeepEditingKeepsTheInput() {
        show()
        type(ProductFormField.Name, "Vitamin D")
        // The system back gesture takes the same path; the E2E suite presses it on a device.
        composeRule.onNodeWithContentDescription("Navigate up").performClick()
        composeRule.onNodeWithText("Discard changes?").assertExists()
        composeRule.onNodeWithText("Keep editing").performClick()
        composeRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        composeRule.onNodeWithTag("Name").assertTextEquals("Name", "Vitamin D")
        assertEquals(0, closed)
    }

    @Test
    fun discardClosesWithoutStoring() {
        show()
        type(ProductFormField.Name, "Vitamin D")
        composeRule.onNodeWithContentDescription("Navigate up").performClick()
        composeRule.onNodeWithText("Discard").performClick()
        assertEquals(1, closed)
        assertTrue(repository.products.value.isEmpty())
    }

    @Test
    fun upArrowWithoutInputClosesDirectly() {
        show()
        composeRule.onNodeWithContentDescription("Navigate up").performClick()
        composeRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        assertEquals(1, closed)
    }

    @Test
    fun editFormIsTitledPrefilledAndHasNoStockField() {
        showEdit()
        composeRule.onNodeWithText("Edit product").assertIsDisplayed()
        composeRule.onNodeWithTag("Name").assertTextEquals("Name", "Vitamin D")
        composeRule.onNodeWithTag("PackageSize").assertTextEquals("Units per package", "30")
        composeRule.onNodeWithTag("UnitsNow").assertDoesNotExist()
        composeRule.onNodeWithText("Units you have now").assertDoesNotExist()
    }

    @Test
    fun editWithAClearedNameShowsTheErrorAndKeepsTheStoredProduct() {
        showEdit()
        type(ProductFormField.Name, "")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Enter a name").assertIsDisplayed()
        composeRule.onNodeWithTag("Name").assertIsFocused()
        assertEquals(stored, repository.products.value.single())
    }

    @Test
    fun editSaveStoresTheRenamedProductAndCloses() {
        showEdit()
        type(ProductFormField.Name, "Vitamin D3")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("Vitamin D3"), repository.products.value.map { it.name.value })
        assertEquals(1, closed)
    }

    @Test
    fun editBackWithoutChangesClosesDirectlyAndWithChangesAsksToDiscard() {
        showEdit()
        composeRule.onNodeWithContentDescription("Navigate up").performClick()
        composeRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        assertEquals(1, closed)
    }

    @Test
    fun editBackWithChangesAsksAndDiscardLeavesTheProductUnchanged() {
        showEdit()
        type(ProductFormField.Name, "Vitamin D3")
        composeRule.onNodeWithContentDescription("Navigate up").performClick()
        composeRule.onNodeWithText("Discard changes?").assertExists()
        composeRule.onNodeWithText("Discard").performClick()
        assertEquals(1, closed)
        assertEquals(stored, repository.products.value.single())
    }

    @Test
    fun deleteDialogUsesTheStoredNameAndCancelKeepsTheProduct() {
        showEdit()
        type(ProductFormField.Name, "Vitamin D3")
        composeRule.onNodeWithText("Delete product").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete Vitamin D?").assertIsDisplayed()
        composeRule.onNodeWithText("Its stock and alert will be removed. This can't be undone.").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Delete Vitamin D?").assertDoesNotExist()
        composeRule.onNodeWithText("Edit product").assertIsDisplayed()
        assertEquals(stored, repository.products.value.single())
        assertEquals(0, closed)
    }

    @Test
    fun confirmingDeleteRemovesTheProductAndCloses() {
        showEdit()
        composeRule.onNodeWithText("Delete product").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()
        assertTrue(repository.products.value.isEmpty())
        assertEquals(1, closed)
    }

    @Test
    fun newProductFormHasNoDeleteButton() {
        show()
        composeRule.onNodeWithText("Delete product").assertDoesNotExist()
    }

    @Test
    fun editOfAMissingProductCloses() {
        val viewModel = ProductFormViewModel(repository, MutableClock(Instant.parse("2026-02-20T10:00:00Z")), stored.id) { ZoneOffset.UTC }
        composeRule.setContent { CustomNotifierTheme { ProductFormRoute(onClose = { closed++ }, viewModel = viewModel) } }
        composeRule.waitForIdle()
        assertEquals(1, closed)
    }
}
