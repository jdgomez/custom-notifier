package dev.jdgomez.customnotifier

import android.Manifest
import android.content.ContentValues
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The calendar channel on the Settings screen, with the system permission dialog driven by UiAutomator. */
@RunWith(AndroidJUnit4::class)
class CalendarChannelTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val application = ApplicationProvider.getApplicationContext<CustomNotifierApplication>()

    @After
    fun removeTestCalendars() {
        withShellPermissions {
            application.contentResolver.delete(syncAdapterUri(), "${Calendars.ACCOUNT_NAME} = ?", arrayOf(ACCOUNT))
        }
    }

    /** The test creates its own local calendars: a fresh emulator has no account. They are removed after each test. */
    private fun createCalendar(
        name: String,
        accessLevel: Int = Calendars.CAL_ACCESS_OWNER,
    ) = withShellPermissions {
        val values =
            ContentValues().apply {
                put(Calendars.ACCOUNT_NAME, ACCOUNT)
                put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                put(Calendars.NAME, name)
                put(Calendars.CALENDAR_DISPLAY_NAME, name)
                put(Calendars.CALENDAR_ACCESS_LEVEL, accessLevel)
                put(Calendars.OWNER_ACCOUNT, ACCOUNT)
                put(Calendars.VISIBLE, 1)
                put(Calendars.SYNC_EVENTS, 1)
            }
        val uri = assertNotNull(application.contentResolver.insert(syncAdapterUri(), values))
        uri.lastPathSegment!!.toLong()
    }

    private fun deleteCalendar(id: Long) =
        withShellPermissions {
            application.contentResolver.delete(syncAdapterUri(), "${Calendars._ID} = ?", arrayOf(id.toString()))
        }

    private fun deviceHasWritableCalendar() =
        withShellPermissions {
            application.contentResolver
                .query(
                    Calendars.CONTENT_URI,
                    arrayOf(Calendars._ID),
                    "${Calendars.CALENDAR_ACCESS_LEVEL} >= ${Calendars.CAL_ACCESS_CONTRIBUTOR}",
                    null,
                    null,
                )?.use { it.count > 0 } == true
        }

    private fun <T> withShellPermissions(block: () -> T): T {
        val automation = instrumentation.uiAutomation
        automation.adoptShellPermissionIdentity(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        try {
            return block()
        } finally {
            automation.dropShellPermissionIdentity()
        }
    }

    private fun syncAdapterUri(): Uri =
        Calendars.CONTENT_URI
            .buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()

    private fun openSettings() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Add alerts to my calendar").assertIsDisplayed()
    }

    private fun turnSwitchOn() = composeRule.onNodeWithText("Add alerts to my calendar").performClick()

    private fun awaitText(text: String) = composeRule.waitUntil(UI_TIMEOUT_MS) { textShown(text) }

    private fun textShown(text: String) = composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** Answers the system dialog; waiting through the compose rule keeps the app's own frames running meanwhile. */
    private fun answer(buttonId: String) {
        composeRule.waitUntil(UI_TIMEOUT_MS) { device.hasObject(By.res(buttonId)) }
        composeRule.waitUntil(UI_TIMEOUT_MS) {
            device.findObject(By.res(buttonId))?.click()
            device.wait(Until.gone(By.res(buttonId)), CLICK_RETRY_MS)
        }
    }

    @Test
    fun firstTimeOnAsksThenChoosingKeepsTheChannel() {
        assumeTrue(!deviceHasWritableCalendar())
        createCalendar("Home")
        createCalendar("Holidays", Calendars.CAL_ACCESS_READ)
        openSettings()
        composeRule.onNodeWithText("Add alerts to my calendar").assertIsOff()

        turnSwitchOn()
        answer(ALLOW_ID)
        awaitText("Choose a calendar")
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        assertFalse(textShown("Holidays"), "a read-only calendar was offered")
        composeRule.onNodeWithText("Home").performClick()

        awaitText("Calendar")
        composeRule.onNodeWithText("Add alerts to my calendar").assertIsOn()
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText(ACCOUNT).assertIsDisplayed()

        // Leaving Settings drops its ViewModel: the choice is read again from the database.
        composeRule.onNodeWithContentDescription("Back").performClick()
        openSettings()
        awaitText("Calendar")
        composeRule.onNodeWithText("Add alerts to my calendar").assertIsOn()
        composeRule.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun deniedKeepsTheSwitchOff() {
        createCalendar("Home")
        openSettings()
        turnSwitchOn()
        answer(DENY_ID)
        composeRule.waitUntil(UI_TIMEOUT_MS) { !device.hasObject(By.res(DENY_ID)) }
        composeRule.onNodeWithText("Add alerts to my calendar").assertIsOff()
    }

    @Test
    fun deniedAgainExplainsThenDeniedForGoodLeadsToTheSystemSettings() {
        createCalendar("Home")
        openSettings()
        turnSwitchOn()
        answer(DENY_ID)

        turnSwitchOn()
        awaitText("Allow calendar access?")
        composeRule.onNodeWithText("Continue").performClick()
        answer(DENY_FOR_GOOD_ID)

        turnSwitchOn()
        awaitText("Calendar access is off")
        composeRule.onNodeWithText("Add alerts to my calendar").assertIsOff()
        composeRule.onNodeWithText("Open settings").performClick()
        assertTrue(device.wait(Until.gone(By.pkg(application.packageName)), UI_TIMEOUT_MS))
        assertFalse(device.hasObject(By.res(ALLOW_ID)))
        device.pressBack()
    }

    @Test
    fun noWritableCalendarShowsTheEmptyDialog() {
        assumeTrue(!deviceHasWritableCalendar())
        createCalendar("Holidays", Calendars.CAL_ACCESS_READ)
        openSettings()
        turnSwitchOn()
        answer(ALLOW_ID)
        awaitText("No calendar available")
        composeRule.onNodeWithText("OK").performClick()
        composeRule.onNodeWithText("Add alerts to my calendar").assertIsOff()
    }

    @Test
    fun removedCalendarIsReportedWhenSettingsOpensAgain() {
        assumeTrue(!deviceHasWritableCalendar())
        val home = createCalendar("Home")
        openSettings()
        turnSwitchOn()
        answer(ALLOW_ID)
        awaitText("Choose a calendar")
        composeRule.onNodeWithText("Home").performClick()
        awaitText("Calendar")
        composeRule.onNodeWithContentDescription("Back").performClick()

        deleteCalendar(home)
        openSettings()
        awaitText("The chosen calendar is no longer available. Choose another one.")
        composeRule.onNodeWithText("Add alerts to my calendar").assertIsOn()
        assertFalse(device.hasObject(By.res(ALLOW_ID)))
    }

    private companion object {
        const val ACCOUNT = "calendar-test@example.com"
        const val ALLOW_ID = "com.android.permissioncontroller:id/permission_allow_button"
        const val DENY_ID = "com.android.permissioncontroller:id/permission_deny_button"
        const val DENY_FOR_GOOD_ID = "com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button"
        const val CLICK_RETRY_MS = 2_000L
    }
}
