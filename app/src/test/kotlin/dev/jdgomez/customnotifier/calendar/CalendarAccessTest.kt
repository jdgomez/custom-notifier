package dev.jdgomez.customnotifier.calendar

import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import dev.jdgomez.customnotifier.CustomNotifierApplication
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalendarAccessTest {
    private val app: CustomNotifierApplication = ApplicationProvider.getApplicationContext()
    private val access = PlatformCalendarAccess(app)

    private fun grant() = shadowOf(app).grantPermissions(*CalendarAccess.PERMISSIONS)

    private fun deny() = shadowOf(app).denyPermissions(*CalendarAccess.PERMISSIONS)

    @Test
    fun `both permissions granted is granted`() {
        grant()
        assertEquals(CalendarAccessState.Granted, access.current())
    }

    @Test
    fun `no permission is missing`() {
        deny()
        assertEquals(CalendarAccessState.Missing, access.current())
    }

    @Test
    fun `only one of the two permissions is not enough`() {
        shadowOf(app).grantPermissions(CalendarAccess.PERMISSIONS[0])
        shadowOf(app).denyPermissions(CalendarAccess.PERMISSIONS[1])
        assertEquals(CalendarAccessState.Missing, access.current())
    }

    @Test
    fun `the answer follows the platform every time and nothing is stored`() {
        deny()
        assertEquals(CalendarAccessState.Missing, access.current())
        grant()
        assertEquals(CalendarAccessState.Granted, access.current())
        deny()
        assertEquals(CalendarAccessState.Missing, access.current())
        assertEquals(
            emptyList(),
            app.filesDir.parentFile!!
                .resolve("shared_prefs")
                .list()
                .orEmpty()
                .filter { "access" in it },
        )
    }

    @Test
    fun `the settings intent opens the app's system settings`() {
        val intent = access.settingsIntent()
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:${app.packageName}", intent.data.toString())
    }
}
