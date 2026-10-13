package dev.jdgomez.customnotifier.productform

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w360dp-h800dp-xhdpi")
class StockUpdateScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-02-20T10:00:00Z")
    private val repository = FakeRepository()
    private var closed = 0

    /** Opens the edit form of a product holding [units] (or [units] / 2) as of the fixed clock moment, consuming 1 per day. */
    private fun showEdit(
        units: Long,
        halves: Boolean = false,
        name: String = "Vitamin D",
        fontScale: Float = 1f,
    ) {
        val product = testProduct(name, now, units = units, halves = halves)
        repository.products.value = listOf(product)
        val viewModel = ProductFormViewModel(repository, MutableClock(now), product.id) { ZoneOffset.UTC }
        // The system font scale: dialogs are windows of their own and ignore a LocalDensity override.
        RuntimeEnvironment.setFontScale(fontScale)
        composeRule.setContent { CustomNotifierTheme { ProductFormRoute(onClose = { closed++ }, viewModel = viewModel) } }
        composeRule.waitForIdle()
    }

    private fun confirm() {
        composeRule.onNodeWithTag("stock_confirm").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun editFormShowsTheStockAboveTheFieldsWithBothButtons() {
        showEdit(units = 59, halves = true)
        composeRule.onNodeWithText("Stock").assertIsDisplayed()
        composeRule.onNodeWithTag("stock").assertTextEquals("≈\u00A029\u00A0pills")
        composeRule.onNodeWithTag("restock").assertIsDisplayed()
        composeRule.onNodeWithTag("adjust").assertIsDisplayed()
        val stockTop =
            composeRule
                .onNodeWithTag("stock_section")
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertTrue(
            stockTop <
                composeRule
                    .onNodeWithTag("Name")
                    .fetchSemanticsNode()
                    .boundsInRoot.top,
        )
    }

    @Test
    fun newProductFormHasNoStockSection() {
        val viewModel = ProductFormViewModel(repository, MutableClock(now)) { ZoneOffset.UTC }
        composeRule.setContent { CustomNotifierTheme { ProductFormRoute(onClose = {}, viewModel = viewModel) } }
        composeRule.onNodeWithTag("stock_section").assertDoesNotExist()
        composeRule.onNodeWithTag("restock").assertDoesNotExist()
        composeRule.onNodeWithTag("adjust").assertDoesNotExist()
    }

    @Test
    fun restockDialogShowsTheAddedUnitsAndTheStockAfter() {
        showEdit(units = 5)
        composeRule.onNodeWithTag("restock").performClick()
        composeRule.onNodeWithText("Restock Vitamin D").assertIsDisplayed()
        composeRule.onNodeWithTag("packages").assertTextEquals("1")
        composeRule.onNodeWithContentDescription("More packages").performClick()
        composeRule.onNodeWithTag("packages").assertTextEquals("2")
        composeRule.onNodeWithText("+60\u00A0pills").assertIsDisplayed()
        composeRule.onNodeWithText("Stock after: 65\u00A0pills").assertIsDisplayed()
    }

    @Test
    fun restockCountAtOneDisablesMinusOnly() {
        showEdit(units = 5)
        composeRule.onNodeWithTag("restock").performClick()
        composeRule.onNodeWithContentDescription("Fewer packages").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("More packages").assertIsEnabled()
    }

    @Test
    fun restockCountAtNinetyNineDisablesPlusOnly() {
        showEdit(units = 5)
        composeRule.onNodeWithTag("restock").performClick()
        repeat(98) { composeRule.onNodeWithContentDescription("More packages").performClick() }
        composeRule.onNodeWithTag("packages").assertTextEquals("99")
        composeRule.onNodeWithContentDescription("More packages").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Fewer packages").assertIsEnabled()
    }

    @Test
    fun cancelledRestockChangesNothing() {
        showEdit(units = 5)
        composeRule.onNodeWithTag("restock").performClick()
        composeRule.onNodeWithContentDescription("More packages").performClick()
        composeRule.onNodeWithContentDescription("More packages").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Restock Vitamin D").assertDoesNotExist()
        composeRule.onNodeWithTag("stock").assertTextEquals("5\u00A0pills")
        assertEquals(
            5.toBigInteger(),
            repository.products.value
                .single()
                .estimatedStockAt(now)
                .wholeUnits,
        )
    }

    @Test
    fun restockIsStoredAndTheFormStaysOpenWithoutAskingToDiscard() {
        showEdit(units = 5)
        composeRule.onNodeWithTag("restock").performClick()
        confirm()
        composeRule.onNodeWithText("Restock Vitamin D").assertDoesNotExist()
        composeRule.onNodeWithTag("stock").assertTextEquals("35\u00A0pills")
        assertEquals(
            35.toBigInteger(),
            repository.products.value
                .single()
                .estimatedStockAt(now)
                .wholeUnits,
        )
        composeRule.onNodeWithContentDescription("Navigate up").performClick()
        composeRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        assertEquals(1, closed)
    }

    @Test
    fun adjustDialogStartsOnRemoveWithFocusedEmptyUnitsAndNoStockAfter() {
        showEdit(units = 20)
        composeRule.onNodeWithTag("adjust").performClick()
        composeRule.onNodeWithText("Adjust stock").assertIsDisplayed()
        composeRule.onNodeWithText("Remove").assertIsDisplayed()
        composeRule.onNodeWithTag("adjust_units").assertTextEquals("Units", "").assertIsFocused()
        composeRule.onNodeWithText("Stock after:", substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("stock_confirm").assertIsNotEnabled()
    }

    @Test
    fun removeUnitsShowsTheStockAfter() {
        showEdit(units = 20)
        composeRule.onNodeWithTag("adjust").performClick()
        composeRule.onNodeWithTag("adjust_units").performTextInput("3")
        composeRule.onNodeWithText("Stock after: 17\u00A0pills").assertIsDisplayed()
        composeRule.onNodeWithTag("stock_confirm").assertIsEnabled()
    }

    @Test
    fun removingMoreThanTheStockShowsZero() {
        showEdit(units = 2)
        composeRule.onNodeWithTag("adjust").performClick()
        composeRule.onNodeWithTag("adjust_units").performTextInput("5")
        composeRule.onNodeWithText("Stock after: 0\u00A0pills").assertIsDisplayed()
    }

    @Test
    fun addUnitsShowsTheStockAfter() {
        showEdit(units = 20)
        composeRule.onNodeWithTag("adjust").performClick()
        composeRule.onNodeWithText("Add").performClick()
        composeRule.onNodeWithTag("adjust_units").performTextInput("4")
        composeRule.onNodeWithText("Stock after: 24\u00A0pills").assertIsDisplayed()
    }

    @Test
    fun clearingTheUnitsHidesTheStockAfterAndDisablesAdjust() {
        showEdit(units = 20)
        composeRule.onNodeWithTag("adjust").performClick()
        composeRule.onNodeWithTag("adjust_units").performTextInput("3")
        composeRule.onNodeWithTag("adjust_units").performTextReplacement("")
        composeRule.onNodeWithText("Stock after:", substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("stock_confirm").assertIsNotEnabled()
    }

    @Test
    fun theUnitsFieldAcceptsOnlyDigits() {
        showEdit(units = 20)
        composeRule.onNodeWithTag("adjust").performClick()
        composeRule.onNodeWithTag("adjust_units").performTextInput("1a.2")
        composeRule.onNodeWithTag("adjust_units").assertTextEquals("Units", "12")
    }

    @Test
    fun confirmedAdjustmentIsStoredAndKeepsUnsavedEdits() {
        showEdit(units = 20)
        composeRule.onNodeWithTag("Name").performTextReplacement("Vitamin D3")
        composeRule.onNodeWithTag("adjust").performClick()
        composeRule.onNodeWithTag("adjust_units").performTextInput("3")
        confirm()
        composeRule.onNodeWithTag("stock").assertTextEquals("17\u00A0pills")
        composeRule.onNodeWithTag("Name").assertTextEquals("Name", "Vitamin D3")
        assertEquals(
            "Vitamin D",
            repository.products.value
                .single()
                .name.value,
        )
        composeRule.onNodeWithContentDescription("Navigate up").performClick()
        composeRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        assertEquals(0, closed)
    }

    @Test
    fun theDepletionDateMovesWithTheStock() {
        showEdit(units = 5)
        composeRule.onNodeWithTag("preview").assertTextEquals("Runs out Feb 25, 2026 · Alert due")
        composeRule.onNodeWithTag("restock").performClick()
        confirm()
        composeRule.onNodeWithTag("preview").assertTextEquals("Runs out Mar 27, 2026 · Alert Mar 17, 2026")
    }

    @Test
    fun everyTextStaysVisibleAtTheLargestFontWithALongName() {
        showEdit(units = 5, name = "N".repeat(60), fontScale = 2f)
        composeRule.onNodeWithTag("restock").performClick()
        composeRule.onNodeWithText("Restock ${"N".repeat(60)}").assertIsDisplayed()
        composeRule.onNodeWithTag("packages").assertIsDisplayed()
        composeRule.onNodeWithText("+30\u00A0pills").assertIsDisplayed()
        composeRule.onNodeWithText("Stock after: 35\u00A0pills").assertIsDisplayed()
        composeRule.onNodeWithTag("stock_confirm").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
