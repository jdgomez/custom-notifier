package dev.jdgomez.customnotifier.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.jdgomez.customnotifier.domain.CalendarId
import dev.jdgomez.customnotifier.domain.DeviceCalendar
import dev.jdgomez.customnotifier.productlist.ProductListScreen
import dev.jdgomez.customnotifier.productlist.ProductListState
import dev.jdgomez.customnotifier.ui.CustomNotifierTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

private val HOME = DeviceCalendar(CalendarId(1), "Home", "user@example.com")
private val FAMILY = DeviceCalendar(CalendarId(2), "Family", "family@example.com")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w360dp-h800dp-xhdpi")
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<String>()

    private fun show(state: SettingsState) =
        composeRule.setContent {
            CustomNotifierTheme {
                SettingsScreen(
                    state = state,
                    onBack = { events += "back" },
                    onSwitchChange = { events += "switch $it" },
                    onAllow = { events += "allow" },
                    onChooseCalendar = { events += "row" },
                    onContinueFromExplanation = { events += "continue" },
                    onChoose = { events += "choose ${it.name}" },
                    onOpenSystemSettings = { events += "open settings" },
                    onDismissDialog = { events += "dismiss" },
                )
            }
        }

    @Test
    fun `settings action opens the screen`() {
        var opened = 0
        composeRule.setContent {
            CustomNotifierTheme {
                ProductListScreen(
                    ProductListState.Empty,
                    onAddProduct = {},
                    onEditProduct = {},
                    onOpenSettings = { opened++ },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Settings").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun `shows the calendar section with the switch off`() {
        show(SettingsState(channel = ChannelSwitch.Off))
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Calendar").assertIsDisplayed()
        composeRule.onNodeWithText("Add alerts to my calendar").assertIsOff()
        composeRule.onNodeWithText("Each alert also appears as an all-day event in the calendar you choose.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertEquals(listOf("back"), events)
    }

    @Test
    fun `the switch is disabled and ignores taps while the setting is loading`() {
        show(SettingsState())
        composeRule.onNodeWithText("Add alerts to my calendar").assertIsNotEnabled().performClick()
        assertEquals(emptyList(), events)
    }

    @Test
    fun `tapping the switch reports it`() {
        show(SettingsState(channel = ChannelSwitch.Off))
        composeRule.onNodeWithText("Add alerts to my calendar").performClick()
        assertEquals(listOf("switch true"), events)
    }

    @Test
    fun `channel on shows the chosen calendar and account and the row opens the chooser`() {
        show(SettingsState(channel = ChannelSwitch.On, calendarRow = CalendarRow.Chosen(HOME)))
        composeRule.onNodeWithText("Add alerts to my calendar").assertIsOn()
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("user@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("Home").performClick()
        assertEquals(listOf("row"), events)
    }

    @Test
    fun `access removed shows the message and Allow`() {
        show(SettingsState(channel = ChannelSwitch.On, accessRemoved = true))
        composeRule.onNodeWithText("Calendar access was removed. Alerts are not added to your calendar.").assertIsDisplayed()
        composeRule.onNodeWithText("Allow").performClick()
        assertEquals(listOf("allow"), events)
    }

    @Test
    fun `calendar gone shows the message and the row opens the chooser`() {
        show(SettingsState(channel = ChannelSwitch.On, calendarRow = CalendarRow.Unavailable))
        composeRule.onNodeWithText("The chosen calendar is no longer available. Choose another one.").performClick()
        assertEquals(listOf("row"), events)
    }

    @Test
    fun `chooser lists calendars with accounts and choosing one reports it`() {
        show(SettingsState(channel = ChannelSwitch.Off, dialog = SettingsDialog.Chooser(listOf(HOME, FAMILY), selected = HOME.id)))
        composeRule.onNodeWithText("Choose a calendar").assertIsDisplayed()
        composeRule.onNodeWithText("family@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("Family").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("choose Family", "dismiss"), events)
    }

    @Test
    fun `explanation dialog buttons`() {
        show(SettingsState(channel = ChannelSwitch.Off, dialog = SettingsDialog.Explanation))
        composeRule.onNodeWithText("Allow calendar access?").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Custom Notifier uses calendar access only to list your calendars and to add one all-day event for each alert.",
            ).assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Not now").performClick()
        assertEquals(listOf("continue", "dismiss"), events)
    }

    @Test
    fun `denied for good dialog buttons`() {
        show(SettingsState(channel = ChannelSwitch.Off, dialog = SettingsDialog.DeniedForGood))
        composeRule.onNodeWithText("Calendar access is off").assertIsDisplayed()
        composeRule.onNodeWithText("Open settings").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("open settings", "dismiss"), events)
    }

    @Test
    fun `no calendar dialog`() {
        show(SettingsState(channel = ChannelSwitch.Off, dialog = SettingsDialog.NoCalendar))
        composeRule.onNodeWithText("No calendar available").assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()
        assertEquals(listOf("dismiss"), events)
    }
}
