package dev.jdgomez.customnotifier.settings

import android.os.Looper
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureScreenRoboImage
import dev.jdgomez.customnotifier.domain.CalendarId
import dev.jdgomez.customnotifier.domain.DeviceCalendar
import dev.jdgomez.customnotifier.referencePath
import dev.jdgomez.customnotifier.ui.CustomNotifierTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val HOME = DeviceCalendar(CalendarId(1), "Home", "user@example.com")
private val FAMILY = DeviceCalendar(CalendarId(2), "Family", "family@example.com")
private val ON = SettingsState(channel = ChannelSwitch.On, calendarRow = CalendarRow.Chosen(HOME))

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w360dp-h800dp-xhdpi")
@OptIn(ExperimentalRoborazziApi::class)
open class SettingsScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(
        name: String,
        state: SettingsState,
        darkTheme: Boolean = false,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                CustomNotifierTheme(darkTheme) {
                    SettingsScreen(state, {}, {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
        composeRule.waitForIdle()
        // A first capture in the JVM is blank unless the root view is invalidated and the main looper drained first.
        if (state.dialog == null) {
            val root = composeRule.onRoot().fetchSemanticsNode().root as View
            composeRule.runOnUiThread { root.invalidate() }
        }
        shadowOf(Looper.getMainLooper()).idle()
        captureScreenRoboImage(
            referencePath("settings_$name", darkTheme),
            RoborazziOptions(
                compareOptions =
                    RoborazziOptions.CompareOptions(changeThreshold = 0f, imageComparator = SimpleImageComparator(maxDistance = 0f)),
            ),
        )
    }

    @Test
    fun off() = capture("off", SettingsState(channel = ChannelSwitch.Off))

    @Test
    fun offDark() = capture("off_dark", SettingsState(channel = ChannelSwitch.Off), darkTheme = true)

    @Test
    fun offLargestFont() = capture("off_font_2", SettingsState(channel = ChannelSwitch.Off), fontScale = 2f)

    @Test
    fun on() = capture("on", ON)

    @Test
    fun onDark() = capture("on_dark", ON, darkTheme = true)

    @Test
    fun onLargestFont() = capture("on_font_2", ON, fontScale = 2f)

    @Test
    fun accessRemoved() = capture("access_removed", SettingsState(channel = ChannelSwitch.On, accessRemoved = true))

    @Test
    fun accessRemovedDark() =
        capture("access_removed_dark", SettingsState(channel = ChannelSwitch.On, accessRemoved = true), darkTheme = true)

    @Test
    fun accessRemovedLargestFont() =
        capture("access_removed_font_2", SettingsState(channel = ChannelSwitch.On, accessRemoved = true), fontScale = 2f)

    @Test
    fun calendarGone() = capture("calendar_gone", SettingsState(channel = ChannelSwitch.On, calendarRow = CalendarRow.Unavailable))

    @Test
    fun calendarGoneDark() =
        capture("calendar_gone_dark", SettingsState(channel = ChannelSwitch.On, calendarRow = CalendarRow.Unavailable), darkTheme = true)

    @Test
    fun calendarGoneLargestFont() =
        capture("calendar_gone_font_2", SettingsState(channel = ChannelSwitch.On, calendarRow = CalendarRow.Unavailable), fontScale = 2f)

    @Test
    fun chooser() =
        capture("chooser", SettingsState(channel = ChannelSwitch.Off, dialog = SettingsDialog.Chooser(listOf(HOME, FAMILY), HOME.id)))

    @Test
    fun chooserDark() =
        capture(
            "chooser_dark",
            SettingsState(channel = ChannelSwitch.Off, dialog = SettingsDialog.Chooser(listOf(HOME, FAMILY), null)),
            darkTheme = true,
        )

    @Test
    fun noCalendar() = capture("no_calendar", SettingsState(channel = ChannelSwitch.Off, dialog = SettingsDialog.NoCalendar))

    @Test
    fun noCalendarDark() =
        capture("no_calendar_dark", SettingsState(channel = ChannelSwitch.Off, dialog = SettingsDialog.NoCalendar), darkTheme = true)

    @Test
    fun explanation() = capture("explanation", SettingsState(channel = ChannelSwitch.Off, dialog = SettingsDialog.Explanation))

    @Test
    fun explanationDark() =
        capture("explanation_dark", SettingsState(channel = ChannelSwitch.Off, dialog = SettingsDialog.Explanation), darkTheme = true)

    @Test
    fun deniedForGood() = capture("denied_for_good", SettingsState(channel = ChannelSwitch.Off, dialog = SettingsDialog.DeniedForGood))

    @Test
    fun deniedForGoodDark() =
        capture("denied_for_good_dark", SettingsState(channel = ChannelSwitch.Off, dialog = SettingsDialog.DeniedForGood), darkTheme = true)
}

@Config(qualifiers = "es-rES-w360dp-h800dp-xhdpi")
class SettingsScreenshotSpanishTest : SettingsScreenshotTest()
