package dev.jdgomez.customnotifier.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.jdgomez.customnotifier.calendar.CalendarAccess
import dev.jdgomez.customnotifier.calendar.CalendarAccessState
import dev.jdgomez.customnotifier.customNotifierContainer
import dev.jdgomez.customnotifier.domain.CalendarChannel
import dev.jdgomez.customnotifier.domain.CalendarChannelSettings
import dev.jdgomez.customnotifier.domain.CalendarGateway
import dev.jdgomez.customnotifier.domain.DeviceCalendar
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What happened on the Settings screen, processed strictly in order by the one consumer of [SettingsViewModel]. */
private sealed interface SettingsEvent {
    data class ChannelChanged(
        val channel: CalendarChannel,
    ) : SettingsEvent

    data object Refresh : SettingsEvent

    data object TurnOff : SettingsEvent

    data class Ask(
        val explanationDue: Boolean,
    ) : SettingsEvent

    data object Continue : SettingsEvent

    data object PermissionRequested : SettingsEvent

    data class PermissionResult(
        val explanationDue: Boolean,
    ) : SettingsEvent

    data class OpenChooser(
        val onlyIfUnusable: Boolean,
    ) : SettingsEvent

    data class Choose(
        val calendar: DeviceCalendar,
    ) : SettingsEvent

    data object Dismiss : SettingsEvent
}

/**
 * The Settings screen: the calendar channel switch, the chosen calendar and the calendar access flow. Turning the switch on never
 * stores the channel by itself: only choosing a calendar does.
 */
@Suppress("TooManyFunctions") // One function per user action of the screen, plus the event handlers.
class SettingsViewModel(
    private val settings: CalendarChannelSettings,
    private val gateway: CalendarGateway,
    private val access: CalendarAccess,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = mutableState.asStateFlow()

    // Every UI event and every change of the stored channel goes through this queue and is handled by one coroutine, in order:
    // that coroutine is the only code that writes the state, and each handler reads the access and the calendars when it runs.
    private val events = Channel<SettingsEvent>(Channel.UNLIMITED)
    private var channel: CalendarChannel = CalendarChannel.Off

    init {
        viewModelScope.launch {
            for (event in events) handle(event)
        }
        viewModelScope.launch {
            settings.observe().collect { events.trySend(SettingsEvent.ChannelChanged(it)) }
        }
    }

    /** Checks calendar access and the chosen calendar again; the screen calls it whenever it becomes visible. */
    fun refresh() = send(SettingsEvent.Refresh)

    fun turnOff() = send(SettingsEvent.TurnOff)

    /**
     * The switch turned on, or the "Allow" button: always asks Android, after the explanation when [explanationDue] (Android's
     * rationale flag, known only to the screen); opens the chooser instead when access is there and no usable calendar is chosen.
     */
    fun ask(explanationDue: Boolean) = send(SettingsEvent.Ask(explanationDue))

    fun continueFromExplanation() = send(SettingsEvent.Continue)

    fun onPermissionRequested() = send(SettingsEvent.PermissionRequested)

    /**
     * The system dialog was answered or interrupted; [explanationDue] is the rationale flag after the request. The result map is
     * not trusted: the access is read again, so a cancelled request or a partial grant counts as not granted. Not granted with no
     * explanation due means Android will not ask again (or the dialog was dismissed): the settings dialog shows at once.
     */
    fun onPermissionResult(explanationDue: Boolean) = send(SettingsEvent.PermissionResult(explanationDue))

    /** Opens the chooser; with [onlyIfUnusable], only when no usable calendar is chosen. */
    fun openChooser(onlyIfUnusable: Boolean = false) = send(SettingsEvent.OpenChooser(onlyIfUnusable))

    /** Stores the choice only while calendar access is there and the calendar still exists; otherwise the screen is brought up to date. */
    fun choose(calendar: DeviceCalendar) = send(SettingsEvent.Choose(calendar))

    fun dismissDialog() = send(SettingsEvent.Dismiss)

    private fun send(event: SettingsEvent) {
        events.trySend(event)
    }

    private suspend fun handle(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ChannelChanged -> {
                channel = event.channel
                recompute()
            }
            SettingsEvent.Refresh -> recompute()
            SettingsEvent.TurnOff -> settings.set(CalendarChannel.Off)
            is SettingsEvent.Ask -> onAsk(event.explanationDue)
            SettingsEvent.Continue -> mutableState.update { it.copy(dialog = null, requestPermission = true) }
            SettingsEvent.PermissionRequested -> mutableState.update { it.copy(requestPermission = false) }
            is SettingsEvent.PermissionResult -> onResult(event.explanationDue)
            is SettingsEvent.OpenChooser -> showChooser(event.onlyIfUnusable)
            is SettingsEvent.Choose -> onChoose(event.calendar)
            SettingsEvent.Dismiss -> mutableState.update { it.copy(dialog = null) }
        }
    }

    private suspend fun recompute() {
        val chosen = channel as? CalendarChannel.On
        val granted = access.current() == CalendarAccessState.Granted
        val row =
            if (chosen == null || !granted) {
                null
            } else {
                gateway.find(chosen.calendarId)?.let { CalendarRow.Chosen(it) } ?: CalendarRow.Unavailable
            }
        mutableState.update { it.copy(channelOn = chosen != null, calendarRow = row, accessRemoved = chosen != null && !granted) }
    }

    private suspend fun onAsk(explanationDue: Boolean) {
        when (access.current()) {
            CalendarAccessState.Granted -> showChooser(onlyIfUnusable = true)
            CalendarAccessState.Missing ->
                mutableState.update {
                    if (explanationDue) it.copy(dialog = SettingsDialog.Explanation) else it.copy(requestPermission = true)
                }
        }
    }

    private suspend fun onResult(explanationDue: Boolean) {
        val granted = access.current() == CalendarAccessState.Granted
        recompute()
        when {
            granted -> showChooser(onlyIfUnusable = true)
            !explanationDue -> mutableState.update { it.copy(dialog = SettingsDialog.DeniedForGood) }
        }
    }

    private suspend fun showChooser(onlyIfUnusable: Boolean) {
        val chosen = channel as? CalendarChannel.On
        if (onlyIfUnusable && chosen != null && gateway.find(chosen.calendarId) != null) return
        val calendars = gateway.writableCalendars()
        val dialog =
            if (calendars.isEmpty()) SettingsDialog.NoCalendar else SettingsDialog.Chooser(calendars, chosen?.calendarId)
        mutableState.update { it.copy(dialog = dialog) }
    }

    private suspend fun onChoose(calendar: DeviceCalendar) {
        mutableState.update { it.copy(dialog = null) }
        when {
            access.current() != CalendarAccessState.Granted -> recompute()
            gateway.find(calendar.id) == null -> showChooser(onlyIfUnusable = false)
            else -> settings.set(CalendarChannel.On(calendar.id))
        }
    }

    fun settingsIntent(): Intent = access.settingsIntent()

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val container = customNotifierContainer()
                    SettingsViewModel(container.calendarChannelSettings, container.calendarGateway, container.calendarAccess)
                }
            }
    }
}
