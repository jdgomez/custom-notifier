package dev.jdgomez.customnotifier.productlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.jdgomez.customnotifier.calendar.CalendarProblem
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

private val NOW = Instant.parse("2026-03-05T10:00:00Z")
private const val BANNER = "Alerts are off. You will not be told before a product runs out."

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w360dp-h800dp-xhdpi")
class ProductListAlertsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var turnedOn = 0
    private var fixed = 0
    private var continued = 0
    private var notNow = 0

    private fun show(state: ProductListState) =
        composeRule.setContent {
            CustomNotifierTheme {
                ProductListScreen(state, onAddProduct = {}, onEditProduct = {}, onTurnOnAlerts = { turnedOn++ })
            }
        }

    private fun products(alertsOff: Boolean) = ProductListState.Products(listOf(testProduct("Vitamin D", NOW)).toRows(), alertsOff)

    private fun List<dev.jdgomez.customnotifier.domain.Product>.toRows() =
        (toListState(NOW, ZoneOffset.UTC) as ProductListState.Products).rows

    @Test
    fun bannerShowsTextAndButtonAboveTheProducts() {
        show(products(alertsOff = true))
        composeRule.onNodeWithText(BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("Turn on").assertIsDisplayed()
        composeRule.onNodeWithText("Vitamin D").assertIsDisplayed()
    }

    @Test
    fun turnOnIsReported() {
        show(products(alertsOff = true))
        composeRule.onNodeWithText("Turn on").performClick()
        assertEquals(1, turnedOn)
    }

    @Test
    fun noBannerWhenAlertsCanBeShown() {
        show(products(alertsOff = false))
        composeRule.onNodeWithText("Vitamin D").assertIsDisplayed()
        composeRule.onNodeWithText(BANNER).assertDoesNotExist()
    }

    @Test
    fun noBannerWithoutProducts() {
        show(ProductListState.Empty)
        composeRule.onNodeWithText("No products yet").assertIsDisplayed()
        composeRule.onNodeWithText(BANNER).assertDoesNotExist()
    }

    private fun withCalendar(
        problem: CalendarProblem?,
        alertsOff: Boolean = false,
    ) = products(alertsOff).copy(calendarProblem = problem)

    private fun showCalendar(state: ProductListState) =
        composeRule.setContent {
            CustomNotifierTheme {
                ProductListScreen(
                    state,
                    onAddProduct = {},
                    onEditProduct = {},
                    onTurnOnAlerts = { turnedOn++ },
                    onOpenSettings = { fixed++ },
                )
            }
        }

    @Test
    fun calendarBannerForRemovedAccess() {
        showCalendar(withCalendar(CalendarProblem.AccessRemoved))
        composeRule.onNodeWithText("Calendar alerts are off. Calendar access was removed.").assertIsDisplayed()
        composeRule.onNodeWithText("Fix").assertIsDisplayed()
        composeRule.onNodeWithText(BANNER).assertDoesNotExist()
    }

    @Test
    fun calendarBannerForAGoneCalendar() {
        showCalendar(withCalendar(CalendarProblem.CalendarGone))
        composeRule.onNodeWithText("Calendar alerts are off. The chosen calendar is no longer available.").assertIsDisplayed()
    }

    @Test
    fun fixOpensSettings() {
        showCalendar(withCalendar(CalendarProblem.AccessRemoved))
        composeRule.onNodeWithText("Fix").performClick()
        assertEquals(1, fixed)
        assertEquals(0, turnedOn)
    }

    @Test
    fun noCalendarBannerWithoutAProblem() {
        showCalendar(withCalendar(null))
        composeRule.onNodeWithText("Vitamin D").assertIsDisplayed()
        composeRule.onNodeWithText("Fix").assertDoesNotExist()
    }

    @Test
    fun noCalendarBannerWithoutProducts() {
        showCalendar(ProductListState.Empty)
        composeRule.onNodeWithText("Fix").assertDoesNotExist()
    }

    @Test
    fun bothBannersAlertsOffFirst() {
        showCalendar(withCalendar(CalendarProblem.AccessRemoved, alertsOff = true))
        val alerts =
            composeRule
                .onNodeWithText(BANNER)
                .fetchSemanticsNode()
                .boundsInRoot.top
        val calendar =
            composeRule
                .onNodeWithText(
                    "Calendar alerts are off. Calendar access was removed.",
                ).fetchSemanticsNode()
                .boundsInRoot.top
        assertTrue(alerts < calendar)
        composeRule.onNodeWithText("Turn on").performClick()
        composeRule.onNodeWithText("Fix").performClick()
        assertEquals(1 to 1, turnedOn to fixed)
    }

    @Test
    fun explanationDialogButtons() {
        composeRule.setContent {
            CustomNotifierTheme { AlertsExplanationDialog(onContinue = { continued++ }, onNotNow = { notNow++ }) }
        }
        composeRule.onNodeWithText("Allow alerts?").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Custom Notifier shows one notification before a product runs out, so you can buy more in time. " +
                    "Without it you get no alerts.",
            ).assertIsDisplayed()
        composeRule.onNodeWithText("Not now").performClick()
        composeRule.onNodeWithText("Continue").performClick()
        assertEquals(1 to 1, continued to notNow)
    }
}
