package dev.jdgomez.customnotifier

import android.os.Build
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
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
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.jdgomez.customnotifier.domain.Rule as AlertRule

/** The notification permission flow on Android 13 and later, driving the system dialog with UiAutomator. */
@RunWith(AndroidJUnit4::class)
class NotificationPermissionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val application = ApplicationProvider.getApplicationContext<CustomNotifierApplication>()

    @Before
    fun requiresRuntimePermission() = assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)

    private fun awaitRow(text: String) =
        composeRule.waitUntil(UI_TIMEOUT_MS) {
            composeRule.onAllNodes(hasText(text) and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }

    private fun awaitBanner() =
        composeRule.waitUntil(UI_TIMEOUT_MS) { composeRule.onAllNodes(hasText("Turn on")).fetchSemanticsNodes().isNotEmpty() }

    private fun bannerShown() = composeRule.onAllNodes(hasText("Turn on")).fetchSemanticsNodes().isNotEmpty()

    private fun type(
        tag: String,
        text: String,
    ) = composeRule.onNodeWithTag(tag).performScrollTo().performTextReplacement(text)

    /** Fills and saves the new-product form. The system dialog may take the focus right after, so the caller awaits the result. */
    private fun saveProduct(
        name: String,
        onEmptyList: Boolean = false,
    ) {
        if (onEmptyList) awaitRow("No products yet")
        composeRule.onNodeWithText("Add product", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("New product").assertIsDisplayed()
        type("Name", name)
        type("Unit", "pills")
        type("PackageSize", "90")
        type("ConsumptionUnits", "1")
        type("UnitsNow", "30")
        composeRule.onNodeWithText("Save").performClick()
    }

    /** Whether the system dialog shows up within [timeoutMs], keeping the app's frames running while waiting. */
    private fun dialogShown(timeoutMs: Long = UI_TIMEOUT_MS) =
        try {
            composeRule.waitUntil(timeoutMs) { device.hasObject(By.res(ALLOW_ID)) }
            true
        } catch (_: ComposeTimeoutException) {
            false
        }

    /**
     * Answers the system dialog; waiting through the compose rule keeps the app's own frames running meanwhile.
     * A tap right after the dialog appears can be dropped while its window does not accept input yet, so it is repeated.
     */
    private fun answer(buttonId: String) {
        composeRule.waitUntil(UI_TIMEOUT_MS) { device.hasObject(By.res(buttonId)) }
        composeRule.waitUntil(UI_TIMEOUT_MS) {
            device.findObject(By.res(buttonId))?.click()
            device.wait(Until.gone(By.res(buttonId)), CLICK_RETRY_MS)
        }
    }

    @Test
    fun grantedAfterTheFirstSaveShowsNoBannerAndAsksNothingMore() {
        saveProduct("Vitamin D", onEmptyList = true)
        answer(ALLOW_ID)
        awaitRow("Vitamin D")
        assertFalse(bannerShown())

        saveProduct("Detergent")
        awaitRow("Detergent")
        assertFalse(dialogShown(SHORT_WAIT_MS), "the permission was asked again")
    }

    @Test
    fun deniedShowsTheBannerAndTurnOnAsksAgainThenOpensSettings() {
        saveProduct("Vitamin D", onEmptyList = true)
        answer(DENY_ID)
        awaitBanner()
        composeRule.onNodeWithText("Alerts are off. You will not be told before a product runs out.").assertIsDisplayed()

        // The banner always asks, without our own explanation; denying for good leaves nothing for Android to explain,
        // so the app notification settings open at once.
        composeRule.onNodeWithText("Turn on").performClick()
        answer(DENY_FOR_GOOD_ID)
        assertTrue(device.wait(Until.gone(By.pkg(application.packageName)), UI_TIMEOUT_MS))
        device.pressBack()
        awaitBanner()

        // Later attempts: Android answers without a dialog and the settings open again.
        composeRule.onNodeWithText("Turn on").performClick()
        assertTrue(device.wait(Until.gone(By.pkg(application.packageName)), UI_TIMEOUT_MS))
        assertFalse(device.hasObject(By.res(ALLOW_ID)))
        device.pressBack()
    }

    @Test
    fun dismissedDialogIsAskedAgainOnTheNextSave() {
        saveProduct("Vitamin D", onEmptyList = true)
        assertTrue(dialogShown(), "no system dialog after the first save")
        device.pressBack()
        assertTrue(device.wait(Until.gone(By.res(ALLOW_ID)), UI_TIMEOUT_MS))
        awaitBanner()

        saveProduct("Detergent")
        assertTrue(dialogShown(), "the permission was not asked again")
    }

    @Test
    fun dueAlertIsShownAfterAllowingFromTheBanner() {
        val product =
            Product(
                id = ProductId(UUID.randomUUID()),
                name = ProductName("Vitamin D"),
                unitLabel = UnitLabel("pills"),
                packageSize = PackageSize(10),
                consumptionRate = ConsumptionRate(units = 1, days = 1),
                stock = Stock(Quantity.of(30), Instant.now().minusSeconds(25 * SECONDS_PER_DAY)),
                rule = AlertRule(LeadTime(10)),
            )
        runBlocking { application.container.productRepository.save(product) }
        awaitBanner()

        composeRule.onNodeWithText("Turn on").performClick()
        answer(ALLOW_ID)

        device.openNotification()
        try {
            assertNotNull(device.wait(Until.findObject(By.text("Buy Vitamin D")), UI_TIMEOUT_MS), "the due alert was not shown")
        } finally {
            // An open shade would cover the app of the next test.
            device.executeShellCommand("cmd statusbar collapse")
        }
    }

    private companion object {
        const val ALLOW_ID = "com.android.permissioncontroller:id/permission_allow_button"
        const val DENY_FOR_GOOD_ID = "com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button"
        const val DENY_ID = "com.android.permissioncontroller:id/permission_deny_button"
        const val SHORT_WAIT_MS = 5_000L
        const val CLICK_RETRY_MS = 2_000L
        const val SECONDS_PER_DAY = 86_400L
    }
}
