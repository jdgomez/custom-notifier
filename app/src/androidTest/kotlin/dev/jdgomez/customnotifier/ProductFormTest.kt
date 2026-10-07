package dev.jdgomez.customnotifier

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductFormTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun awaitText(
        text: String,
        timeoutMs: Long = LOAD_TIMEOUT_MS,
    ) = composeRule.waitUntil(timeoutMs) {
        composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun openForm() {
        awaitText("No products yet")
        composeRule.onNodeWithText("Add product", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("New product").assertIsDisplayed()
    }

    private fun type(
        tag: String,
        text: String,
    ) = composeRule.onNodeWithTag(tag).performScrollTo().performTextReplacement(text)

    @Test
    fun createdProductIsListed() {
        openForm()
        type("Name", "Vitamin D")
        type("Unit", "pills")
        type("PackageSize", "90")
        type("ConsumptionUnits", "1")
        type("UnitsNow", "30")
        composeRule.onNodeWithText("Save").performClick()

        awaitText("Vitamin D")
        // The stock has already started to decrease: "≈ 29 pills" is as valid as "30 pills".
        composeRule.onAllNodes(hasText("pills", substring = true)).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Add product", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun backWithInputAsksToDiscardAndDiscardReturnsToTheEmptyList() {
        openForm()
        type("Name", "Vitamin D")
        // A device back press is swallowed by the keyboard while it is open, so close it first.
        Espresso.closeSoftKeyboard()
        device.pressBack()
        awaitText("Discard changes?")
        composeRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        composeRule.onNodeWithText("Discard").performClick()

        awaitText("No products yet")
        composeRule.onNodeWithText("Add product", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun backWithoutInputReturnsToTheListDirectly() {
        openForm()
        device.pressBack()
        awaitText("No products yet")
    }

    @Test
    fun rotationKeepsTheInput() {
        openForm()
        type("Name", "Vitamin D")
        try {
            device.setOrientationLandscape()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("Name").performScrollTo().assertTextEquals("Name", "Vitamin D")
        } finally {
            device.setOrientationNatural()
            device.unfreezeRotation()
        }
    }

    private companion object {
        const val LOAD_TIMEOUT_MS = 5_000L
    }
}
