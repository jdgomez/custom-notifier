package dev.jdgomez.customnotifier.settings

import android.content.Intent
import dev.jdgomez.customnotifier.calendar.CalendarAccess
import dev.jdgomez.customnotifier.calendar.CalendarAccessState
import dev.jdgomez.customnotifier.domain.CalendarChannel
import dev.jdgomez.customnotifier.domain.CalendarChannelSettings
import dev.jdgomez.customnotifier.domain.CalendarGateway
import dev.jdgomez.customnotifier.domain.CalendarId
import dev.jdgomez.customnotifier.domain.DeviceCalendar
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
    override suspend fun writableCalendars() = calendars

    override suspend fun find(id: CalendarId) = calendars.firstOrNull { it.id == id }
}

private class FakeAccess(
    var state: CalendarAccessState = CalendarAccessState.Missing(canAsk = true),
) : CalendarAccess {
    var denied: Pair<Boolean, Boolean>? = null

    override fun current() = state

    override fun recordDenied(
        explanationBefore: Boolean,
        explanationAfter: Boolean,
    ) {
        denied = explanationBefore to explanationAfter
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
        viewModel.onPermissionResult(granted = true, explanationBefore = false, explanationAfter = false)

        assertEquals(listOf(HOME, FAMILY), viewModel.chooser().calendars)
        assertFalse(viewModel.state.value.channelOn)
    }

    @Test
    fun `access denied closes everything and the switch stays off`() {
        val viewModel = viewModel()
        viewModel.ask(false)
        viewModel.onPermissionResult(granted = false, explanationBefore = false, explanationAfter = true)

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
        viewModel.onPermissionResult(granted = true, explanationBefore = false, explanationAfter = false)

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
        viewModel.onPermissionResult(granted = true, explanationBefore = false, explanationAfter = false)

        assertEquals(listOf(FAMILY), viewModel.chooser().calendars)
    }
}
