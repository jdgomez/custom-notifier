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
    fun `never asked, access is missing and can be asked`() {
        deny()
        assertEquals(CalendarAccessState.Missing(canAsk = true), access.current())
    }

    @Test
    fun `denied once, it can still be asked`() {
        deny()
        access.recordDenied(explanationBefore = false, explanationAfter = true)
        assertEquals(CalendarAccessState.Missing(canAsk = true), access.current())
    }

    @Test
    fun `denied for good, it cannot be asked anymore`() {
        deny()
        access.recordDenied(explanationBefore = true, explanationAfter = false)
        assertEquals(CalendarAccessState.Missing(canAsk = false), access.current())
    }

    @Test
    fun `dismissed dialog, it can be asked again`() {
        deny()
        access.recordDenied(explanationBefore = false, explanationAfter = false)
        assertEquals(CalendarAccessState.Missing(canAsk = true), access.current())
    }

    @Test
    fun `granted, access is there and the permanent denial is forgotten`() {
        deny()
        access.recordDenied(explanationBefore = true, explanationAfter = false)
        grant()
        assertEquals(CalendarAccessState.Granted, access.current())
        deny()
        assertEquals(CalendarAccessState.Missing(canAsk = true), access.current())
    }

    @Test
    fun `only one of the two permissions is not enough`() {
        shadowOf(app).grantPermissions(CalendarAccess.PERMISSIONS[0])
        shadowOf(app).denyPermissions(CalendarAccess.PERMISSIONS[1])
        assertEquals(CalendarAccessState.Missing(canAsk = true), access.current())
    }

    @Test
    fun `the settings intent opens the app's system settings`() {
        val intent = access.settingsIntent()
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:${app.packageName}", intent.data.toString())
    }

    @Test
    fun `a permission reset to ask again without granting can be asked again after clearing the denial`() {
        deny()
        access.recordDenied(explanationBefore = true, explanationAfter = false)
        assertEquals(CalendarAccessState.Missing(canAsk = false), access.current())
        access.clearPermanentDenial()
        assertEquals(CalendarAccessState.Missing(canAsk = true), access.current())
    }

    @Test
    fun `silent denial after returning from the settings unchanged is reported and blocks further asking`() {
        deny()
        assertEquals(false, access.recordDenied(explanationBefore = false, explanationAfter = true))
        assertEquals(false, access.recordDenied(explanationBefore = true, explanationAfter = false))
        access.clearPermanentDenial()
        assertEquals(CalendarAccessState.Missing(canAsk = true), access.current())

        // The permission was not changed in the settings: Android shows no dialog and the request comes back at once.
        assertEquals(true, access.recordDenied(explanationBefore = false, explanationAfter = false))
        assertEquals(CalendarAccessState.Missing(canAsk = false), access.current())
    }

    @Test
    fun `reset to ask every time then denied once can be asked again`() {
        deny()
        access.recordDenied(explanationBefore = false, explanationAfter = true)
        access.recordDenied(explanationBefore = true, explanationAfter = false)
        access.clearPermanentDenial()

        // Android shows the dialog again and the user denies it: a normal first denial.
        assertEquals(false, access.recordDenied(explanationBefore = false, explanationAfter = true))
        assertEquals(CalendarAccessState.Missing(canAsk = true), access.current())
    }

    @Test
    fun `a first dismissed dialog is asked again and is not reported`() {
        deny()
        assertEquals(false, access.recordDenied(explanationBefore = false, explanationAfter = false))
        assertEquals(CalendarAccessState.Missing(canAsk = true), access.current())
    }

    @Test
    fun `granting forgets that the user was asked`() {
        deny()
        access.recordDenied(explanationBefore = false, explanationAfter = true)
        grant()
        access.current()
        deny()
        // Revoked in the settings and asked again: a dismissed dialog is a first dismissal, not a silent denial.
        assertEquals(false, access.recordDenied(explanationBefore = false, explanationAfter = false))
        assertEquals(CalendarAccessState.Missing(canAsk = true), access.current())
    }
}
