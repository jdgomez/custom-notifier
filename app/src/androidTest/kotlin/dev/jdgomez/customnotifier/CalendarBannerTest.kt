package dev.jdgomez.customnotifier

import android.Manifest
import android.content.ContentValues
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dev.jdgomez.customnotifier.domain.CalendarChannel
import dev.jdgomez.customnotifier.domain.CalendarId
import dev.jdgomez.customnotifier.domain.ConsumptionRate
import dev.jdgomez.customnotifier.domain.LeadTime
import dev.jdgomez.customnotifier.domain.PackageSize
import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.domain.ProductName
import dev.jdgomez.customnotifier.domain.Quantity
import dev.jdgomez.customnotifier.domain.Stock
import dev.jdgomez.customnotifier.domain.UnitLabel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import dev.jdgomez.customnotifier.domain.Rule as AlertRule

/** The product list banner for a calendar channel that cannot work. Each test starts without calendar access. */
@RunWith(AndroidJUnit4::class)
class CalendarBannerTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val application = ApplicationProvider.getApplicationContext<CustomNotifierApplication>()

    @After
    fun cleanUp() {
        scenario?.close()
        withShellPermissions {
            application.contentResolver.delete(syncAdapterUri(), "${Calendars.ACCOUNT_NAME} = ?", arrayOf(ACCOUNT))
        }
    }

    private fun launch() {
        scenario?.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun seed(
        calendarId: Long,
        withAccess: Boolean,
    ) {
        val product =
            Product(
                id = ProductId(UUID.randomUUID()),
                name = ProductName("Coffee"),
                unitLabel = UnitLabel("pills"),
                packageSize = PackageSize(10),
                consumptionRate = ConsumptionRate(units = 1, days = 1),
                stock = Stock(Quantity.of(30), Instant.now()),
                rule = AlertRule(LeadTime(2)),
            )
        runBlocking {
            application.container.productRepository.save(product)
            application.container.calendarChannelSettings.set(CalendarChannel.On(CalendarId(calendarId)))
        }
        if (withAccess) {
            CalendarAccessPermissions.forEach { instrumentation.uiAutomation.grantRuntimePermission(application.packageName, it) }
        }
    }

    private fun createCalendar(): Long =
        withShellPermissions {
            val values =
                ContentValues().apply {
                    put(Calendars.ACCOUNT_NAME, ACCOUNT)
                    put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                    put(Calendars.NAME, "Home")
                    put(Calendars.CALENDAR_DISPLAY_NAME, "Home")
                    put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
                    put(Calendars.OWNER_ACCOUNT, ACCOUNT)
                    put(Calendars.VISIBLE, 1)
                    put(Calendars.SYNC_EVENTS, 1)
                }
            assertNotNull(application.contentResolver.insert(syncAdapterUri(), values)).lastPathSegment!!.toLong()
        }

    private fun deleteCalendar(id: Long) =
        withShellPermissions {
            application.contentResolver.delete(syncAdapterUri(), "${Calendars._ID} = ?", arrayOf(id.toString()))
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

    private fun awaitText(text: String) = composeRule.waitUntil(UI_TIMEOUT_MS) { textShown(text) }

    private fun textShown(text: String) = composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun awaitNoBanner() = composeRule.waitUntil(UI_TIMEOUT_MS) { !textShown("Fix") && textShown("Coffee") }

    private fun fix() {
        composeRule.onNodeWithText("Fix").performClick()
        composeRule.awaitSettingsSwitchEnabled()
    }

    @Test
    fun accessRemovedShowsTheBannerAndFixAllowsAgain() {
        val home = createCalendar()
        seed(home, withAccess = false)
        launch()
        awaitText(ACCESS_REMOVED)
        composeRule.onNodeWithText("Fix").assertIsDisplayed()

        fix()
        composeRule.onNodeWithText("Allow").performClick()
        val allow = "com.android.permissioncontroller:id/permission_allow_button"
        composeRule.waitUntil(UI_TIMEOUT_MS) { device.hasObject(By.res(allow)) }
        composeRule.waitUntil(UI_TIMEOUT_MS) {
            device.findObject(By.res(allow))?.click()
            device.wait(Until.gone(By.res(allow)), CLICK_RETRY_MS)
        }
        composeRule.onNodeWithContentDescription("Back").performClick()
        awaitNoBanner()
        assertFalse(textShown(ACCESS_REMOVED))
    }

    @Test
    fun calendarGoneShowsTheBannerAndTurningTheChannelOffRemovesIt() {
        val home = createCalendar()
        seed(home, withAccess = true)
        deleteCalendar(home)
        launch()
        awaitText(CALENDAR_GONE)

        fix()
        composeRule.onNodeWithText("Add alerts to my calendar").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        awaitNoBanner()
        assertFalse(textShown(CALENDAR_GONE))
    }

    private companion object {
        const val ACCOUNT = "calendar-banner-test@example.com"
        const val ACCESS_REMOVED = "Calendar alerts are off. Calendar access was removed."
        const val CALENDAR_GONE = "Calendar alerts are off. The chosen calendar is no longer available."
        const val CLICK_RETRY_MS = 2_000L
        val CalendarAccessPermissions = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    }
}
