package dev.jdgomez.customnotifier.settings

import android.content.Intent
import dev.jdgomez.customnotifier.calendar.CalendarAccess
import dev.jdgomez.customnotifier.calendar.CalendarAccessState
import dev.jdgomez.customnotifier.domain.CalendarChannel
import dev.jdgomez.customnotifier.domain.CalendarChannelSettings
import dev.jdgomez.customnotifier.domain.CalendarGateway
import dev.jdgomez.customnotifier.domain.CalendarId
import dev.jdgomez.customnotifier.domain.DeviceCalendar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val HOME = DeviceCalendar(CalendarId(1), "Home", "user@example.com")
private val FAMILY = DeviceCalendar(CalendarId(2), "Family", "user@example.com")

private class FakeSettings(
    initial: CalendarChannel = CalendarChannel.Off,
) : CalendarChannelSettings {
    val channel = MutableStateFlow(initial)

    override fun observe(): Flow<CalendarChannel> = channel

    override suspend fun set(channel: CalendarChannel) {
        this.channel.value = channel
    }
}

private class FakeGateway(
    var calendars: List<DeviceCalendar> = listOf(HOME, FAMILY),
) : CalendarGateway {
    /** When set, the next [find] waits for it and answers with the calendars as they were when it started. */
    var holdNextFind: CompletableDeferred<Unit>? = null

    override suspend fun writableCalendars() = calendars

    override suspend fun find(id: CalendarId): DeviceCalendar? {
        val answer = calendars.firstOrNull { it.id == id }
        holdNextFind?.let {
            holdNextFind = null
            it.await()
        }
        return answer
    }
}

private class FakeAccess(
    var state: CalendarAccessState = CalendarAccessState.Missing(canAsk = true),
) : CalendarAccess {
    var denied: Pair<Boolean, Boolean>? = null
    var cleared = 0
    var silent = false

    override fun current() = state

    override fun recordDenied(
        explanationBefore: Boolean,
        explanationAfter: Boolean,
    ): Boolean {
        denied = explanationBefore to explanationAfter
        return silent
    }

    override fun clearPermanentDenial() {
        cleared++
    }

    override fun settingsIntent() = Intent("settings")
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val settings = FakeSettings()
    private val gateway = FakeGateway()
    private val access = FakeAccess()

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMain() = Dispatchers.resetMain()

    private fun viewModel() = SettingsViewModel(settings, gateway, access)

    private fun SettingsViewModel.chooser() = state.value.dialog as SettingsDialog.Chooser

    @Test
    fun `the channel is off on a fresh install`() {
        val state = viewModel().state.value
        assertFalse(state.channelOn)
        assertNull(state.calendarRow)
        assertNull(state.dialog)
    }

    @Test
    fun `turning on the first time requests the system dialog`() {
        val viewModel = viewModel()
        viewModel.ask(explanationDue = false)
        assertTrue(viewModel.state.value.requestPermission)
        assertNull(viewModel.state.value.dialog)
        viewModel.onPermissionRequested()
        assertFalse(viewModel.state.value.requestPermission)
    }

    @Test
    fun `access granted opens the chooser and the switch stays off until a choice`() {
        val viewModel = viewModel()
        viewModel.ask(false)
        access.state = CalendarAccessState.Granted
        viewModel.onPermissionResult(explanationBefore = false, explanationAfter = false)

        assertEquals(listOf(HOME, FAMILY), viewModel.chooser().calendars)
        assertFalse(viewModel.state.value.channelOn)
    }

    @Test
    fun `access denied closes everything and the switch stays off`() {
        val viewModel = viewModel()
        viewModel.ask(false)
        viewModel.onPermissionResult(explanationBefore = false, explanationAfter = true)

        assertNull(viewModel.state.value.dialog)
        assertFalse(viewModel.state.value.channelOn)
        assertEquals(false to true, access.denied)
    }

    @Test
    fun `after an earlier denial the explanation comes first and Continue requests`() {
        val viewModel = viewModel()
        viewModel.ask(explanationDue = true)
        assertEquals(SettingsDialog.Explanation, viewModel.state.value.dialog)
        assertFalse(viewModel.state.value.requestPermission)

        viewModel.continueFromExplanation()
        assertNull(viewModel.state.value.dialog)
        assertTrue(viewModel.state.value.requestPermission)
    }

    @Test
    fun `Not now closes the explanation`() {
        val viewModel = viewModel()
        viewModel.ask(explanationDue = true)
        viewModel.dismissDialog()
        assertNull(viewModel.state.value.dialog)
        assertFalse(viewModel.state.value.channelOn)
    }

    @Test
    fun `denied for good shows the settings dialog`() {
        access.state = CalendarAccessState.Missing(canAsk = false)
        val viewModel = viewModel()
        viewModel.ask(explanationDue = false)
        assertEquals(SettingsDialog.DeniedForGood, viewModel.state.value.dialog)
        assertFalse(viewModel.state.value.requestPermission)
        assertNotNull(viewModel.settingsIntent())
    }

    @Test
    fun `no access request is made when the screen only opens`() {
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.requestPermission)
        assertNull(viewModel.state.value.dialog)
    }

    @Test
    fun `choosing a calendar turns the channel on with it`() {
        access.state = CalendarAccessState.Granted
        val viewModel = viewModel()
        viewModel.ask(false)
        viewModel.choose(HOME)

        assertEquals(CalendarChannel.On(HOME.id), settings.channel.value)
        assertTrue(viewModel.state.value.channelOn)
        assertEquals(CalendarRow.Chosen(HOME), viewModel.state.value.calendarRow)
        assertNull(viewModel.state.value.dialog)
    }

    @Test
    fun `cancelling the first choice leaves the channel off`() {
        access.state = CalendarAccessState.Granted
        val viewModel = viewModel()
        viewModel.ask(false)
        viewModel.dismissDialog()

        assertEquals(CalendarChannel.Off, settings.channel.value)
        assertFalse(viewModel.state.value.channelOn)
    }

    @Test
    fun `read-only calendars are not offered because the gateway does not list them`() {
        access.state = CalendarAccessState.Granted
        gateway.calendars = listOf(HOME)
        val viewModel = viewModel()
        viewModel.ask(false)
        assertEquals(listOf(HOME), viewModel.chooser().calendars)
    }

    @Test
    fun `no calendar on the device shows the empty dialog and the channel stays off`() {
        access.state = CalendarAccessState.Granted
        gateway.calendars = emptyList()
        val viewModel = viewModel()
        viewModel.ask(false)

        assertEquals(SettingsDialog.NoCalendar, viewModel.state.value.dialog)
        viewModel.dismissDialog()
        assertFalse(viewModel.state.value.channelOn)
    }

    @Test
    fun `the stored channel shows the calendar row`() {
        access.state = CalendarAccessState.Granted
        settings.channel.value = CalendarChannel.On(HOME.id)
        val state = viewModel().state.value

        assertTrue(state.channelOn)
        assertEquals(CalendarRow.Chosen(HOME), state.calendarRow)
        assertFalse(state.accessRemoved)
    }

    @Test
    fun `turning the channel off hides the row`() {
        access.state = CalendarAccessState.Granted
        settings.channel.value = CalendarChannel.On(HOME.id)
        val viewModel = viewModel()
        viewModel.turnOff()

        assertEquals(CalendarChannel.Off, settings.channel.value)
        assertFalse(viewModel.state.value.channelOn)
        assertNull(viewModel.state.value.calendarRow)
    }

    @Test
    fun `changing the calendar keeps the channel on with the selection marked`() {
        access.state = CalendarAccessState.Granted
        settings.channel.value = CalendarChannel.On(HOME.id)
        val viewModel = viewModel()
        viewModel.openChooser()
        assertEquals(HOME.id, viewModel.chooser().selected)

        viewModel.choose(FAMILY)
        assertEquals(CalendarRow.Chosen(FAMILY), viewModel.state.value.calendarRow)
        assertTrue(viewModel.state.value.channelOn)
    }

    @Test
    fun `access removed keeps the channel on and reports it`() {
        access.state = CalendarAccessState.Missing(canAsk = true)
        settings.channel.value = CalendarChannel.On(HOME.id)
        val state = viewModel().state.value

        assertTrue(state.channelOn)
        assertTrue(state.accessRemoved)
        assertNull(state.calendarRow)
        assertEquals(CalendarChannel.On(HOME.id), settings.channel.value)
    }

    @Test
    fun `access given back removes the message and keeps the usable calendar without the chooser`() {
        access.state = CalendarAccessState.Missing(canAsk = true)
        settings.channel.value = CalendarChannel.On(HOME.id)
        val viewModel = viewModel()
        viewModel.ask(false)
        access.state = CalendarAccessState.Granted
        viewModel.onPermissionResult(explanationBefore = false, explanationAfter = false)

        assertFalse(viewModel.state.value.accessRemoved)
        assertEquals(CalendarRow.Chosen(HOME), viewModel.state.value.calendarRow)
        assertNull(viewModel.state.value.dialog)
    }

    @Test
    fun `calendar removed shows the unavailable row and the chooser opens from it`() {
        access.state = CalendarAccessState.Granted
        settings.channel.value = CalendarChannel.On(HOME.id)
        gateway.calendars = listOf(FAMILY)
        val viewModel = viewModel()

        assertEquals(CalendarRow.Unavailable, viewModel.state.value.calendarRow)
        assertTrue(viewModel.state.value.channelOn)
        viewModel.openChooser()
        assertEquals(listOf(FAMILY), viewModel.chooser().calendars)
    }

    @Test
    fun `refresh picks up a calendar that comes back`() {
        access.state = CalendarAccessState.Granted
        settings.channel.value = CalendarChannel.On(HOME.id)
        gateway.calendars = emptyList()
        val viewModel = viewModel()
        assertEquals(CalendarRow.Unavailable, viewModel.state.value.calendarRow)

        gateway.calendars = listOf(HOME)
        viewModel.refresh()
        assertEquals(CalendarRow.Chosen(HOME), viewModel.state.value.calendarRow)
    }

    @Test
    fun `access given back while the calendar is gone opens the chooser`() {
        access.state = CalendarAccessState.Missing(canAsk = true)
        settings.channel.value = CalendarChannel.On(HOME.id)
        gateway.calendars = listOf(FAMILY)
        val viewModel = viewModel()
        viewModel.ask(false)
        access.state = CalendarAccessState.Granted
        viewModel.onPermissionResult(explanationBefore = false, explanationAfter = false)

        assertEquals(listOf(FAMILY), viewModel.chooser().calendars)
    }

    @Test
    fun `a cancelled request returns no result and is not treated as granted`() {
        val viewModel = viewModel()
        viewModel.ask(false)
        // The system dialog was interrupted: access is still missing, whatever the result map said.
        viewModel.onPermissionResult(explanationBefore = false, explanationAfter = false)

        assertNull(viewModel.state.value.dialog)
        assertFalse(viewModel.state.value.channelOn)
    }

    @Test
    fun `the latest check wins when an older one finishes late`() {
        access.state = CalendarAccessState.Granted
        settings.channel.value = CalendarChannel.On(HOME.id)
        val viewModel = viewModel()
        assertEquals(CalendarRow.Chosen(HOME), viewModel.state.value.calendarRow)

        // The older check starts while "Home" exists and is held; the calendar then disappears and a newer check sees that.
        val gate = CompletableDeferred<Unit>()
        gateway.holdNextFind = gate
        viewModel.refresh()
        gateway.calendars = emptyList()
        viewModel.refresh()
        assertEquals(CalendarRow.Unavailable, viewModel.state.value.calendarRow)

        gate.complete(Unit)
        assertEquals(CalendarRow.Unavailable, viewModel.state.value.calendarRow)
    }

    @Test
    fun `choosing is refused when access was removed meanwhile`() {
        access.state = CalendarAccessState.Granted
        val viewModel = viewModel()
        viewModel.ask(false)
        access.state = CalendarAccessState.Missing(canAsk = true)
        viewModel.choose(HOME)

        assertEquals(CalendarChannel.Off, settings.channel.value)
        assertFalse(viewModel.state.value.channelOn)
        assertNull(viewModel.state.value.dialog)
    }

    @Test
    fun `choosing is refused when the calendar is gone and the chooser shows the current ones`() {
        access.state = CalendarAccessState.Granted
        val viewModel = viewModel()
        viewModel.ask(false)
        gateway.calendars = listOf(FAMILY)
        viewModel.choose(HOME)

        assertEquals(CalendarChannel.Off, settings.channel.value)
        assertEquals(listOf(FAMILY), viewModel.chooser().calendars)
    }

    @Test
    fun `an explanation due means a recorded permanent denial is stale`() {
        val viewModel = viewModel()
        viewModel.ask(explanationDue = true)
        assertEquals(1, access.cleared)
        viewModel.ask(explanationDue = false)
        assertEquals(1, access.cleared)
    }

    @Test
    fun `coming back from the system settings forgets the permanent denial`() {
        access.state = CalendarAccessState.Missing(canAsk = false)
        val viewModel = viewModel()
        viewModel.refresh(backFromSystemSettings = true)
        assertEquals(1, access.cleared)
    }

    @Test
    fun `a request denied with no dialog shown leads to the access is off dialog`() {
        val viewModel = viewModel()
        viewModel.ask(explanationDue = false)
        access.silent = true
        viewModel.onPermissionResult(explanationBefore = false, explanationAfter = false)

        assertEquals(SettingsDialog.DeniedForGood, viewModel.state.value.dialog)
        assertFalse(viewModel.state.value.channelOn)
    }

    @Test
    fun `a denial with a dialog shown does not open the access is off dialog yet`() {
        val viewModel = viewModel()
        viewModel.ask(explanationDue = false)
        viewModel.onPermissionResult(explanationBefore = true, explanationAfter = false)
        assertNull(viewModel.state.value.dialog)
    }
}
