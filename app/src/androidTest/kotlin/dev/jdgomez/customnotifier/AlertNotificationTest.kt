package dev.jdgomez.customnotifier

import android.Manifest
import android.os.Build
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.jdgomez.customnotifier.domain.Rule as AlertRule

@RunWith(AndroidJUnit4::class)
class AlertNotificationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val application = ApplicationProvider.getApplicationContext<CustomNotifierApplication>()
    private val container = application.container

    @Before
    fun allowNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(application.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        device.pressHome()
    }

    /** A product consuming 1 unit a day whose stock was recorded [daysAgo] days ago. */
    private fun seed(
        units: Long,
        daysAgo: Long,
    ) = Product(
        id = ProductId(UUID.randomUUID()),
        name = ProductName("Vitamin D"),
        unitLabel = UnitLabel("pills"),
        packageSize = PackageSize(10),
        consumptionRate = ConsumptionRate(units = 1, days = 1),
        stock = Stock(Quantity.of(units), Instant.now().minusSeconds(daysAgo * SECONDS_PER_DAY)),
        rule = AlertRule(LeadTime(10)),
    ).also { runBlocking { container.productRepository.save(it) } }

    @Test
    fun dueProductPostsTheNotificationAndTappingOpensTheList() {
        // Runs out in 5 days with a 10 day lead time: the alert is due now.
        val product = seed(units = 30, daysAgo = 25)
        val date = product.depletionDateIn(ZoneId.systemDefault()).date
        val expected = "Runs out " + DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(date)

        device.openNotification()
        val title = assertNotNull(device.wait(Until.findObject(By.text("Buy Vitamin D")), UI_TIMEOUT_MS))
        assertNotNull(device.findObject(By.text(expected)))
        title.click()

        assertNotNull(device.wait(Until.findObject(By.pkg(application.packageName).text("Vitamin D")), UI_TIMEOUT_MS))
        assertTrue(device.wait(Until.gone(By.text("Buy Vitamin D")), UI_TIMEOUT_MS))
    }

    @Test
    fun scheduledProductHasAPendingAlarm() {
        seed(units = 30, daysAgo = 0)

        val deadline = System.currentTimeMillis() + UI_TIMEOUT_MS
        var alarms: String
        do {
            alarms = device.executeShellCommand("dumpsys alarm")
            if (alarms.contains(ALARM_RECEIVER)) break
            Thread.sleep(POLL_MS)
        } while (System.currentTimeMillis() < deadline)

        assertTrue(alarms.contains(ALARM_RECEIVER), "no pending alarm for ${application.packageName}")
    }

    private companion object {
        const val POLL_MS = 250L
        const val SECONDS_PER_DAY = 86_400L
        const val ALARM_RECEIVER = "dev.jdgomez.customnotifier/.alerts.AlertAlarmReceiver"
    }
}
