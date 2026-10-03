package dev.jdgomez.customnotifier

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class LaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launcherIntentShowsMainScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val context = instrumentation.targetContext

        // Start from the launcher: UI Automator brings the home screen up, then the launcher intent starts the app.
        device.pressHome()
        val launchIntent =
            assertNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(launchIntent)
        assertNotNull(device.wait(Until.findObject(By.pkg(context.packageName)), LAUNCH_TIMEOUT_MS))

        composeRule.onNodeWithText("CI failure proof: this text does not exist").assertIsDisplayed()
    }

    private companion object {
        const val LAUNCH_TIMEOUT_MS = 5_000L
    }
}
