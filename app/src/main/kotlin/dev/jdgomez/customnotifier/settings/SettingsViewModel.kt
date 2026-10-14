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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The Settings screen: the calendar channel switch, the chosen calendar and the calendar access flow. Turning the switch on never
 * stores the channel by itself: only choosing a calendar does.
 */
@Suppress("TooManyFunctions") // One function per user action of the screen, plus the serial write path.
class SettingsViewModel(
    private val settings: CalendarChannelSettings,
    private val gateway: CalendarGateway,
    private val access: CalendarAccess,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = mutableState.asStateFlow()

    private var channel: CalendarChannel = CalendarChannel.Off

    // Every coroutine that writes the state runs through [serially]: one at a time, in the order they were requested, each one
    // reading the access and the calendars when it runs, so the last request always has the last word.
    private val writes = Mutex()

    init {
        viewModelScope.launch {
            settings.observe().collect {
                channel = it
                refresh()
            }
        }
    }

    private fun serially(block: suspend () -> Unit) {
        viewModelScope.launch { writes.withLock { block() } }
    }

    /** Checks calendar access and the chosen calendar again; the screen calls it whenever it becomes visible. */
    fun refresh() = serially { recompute() }

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

    fun turnOff() {
        viewModelScope.launch { settings.set(CalendarChannel.Off) }
    }

    /**
     * The switch turned on, or the "Allow" button: always asks Android, after the explanation when [explanationDue] (Android's
     * rationale flag, known only to the screen); opens the chooser instead when access is there and no usable calendar is chosen.
     */
    fun ask(explanationDue: Boolean) =
        serially {
            when (access.current()) {
                CalendarAccessState.Granted -> showChooser(onlyIfUnusable = true)
                CalendarAccessState.Missing ->
                    mutableState.update {
                        if (explanationDue) it.copy(dialog = SettingsDialog.Explanation) else it.copy(requestPermission = true)
                    }
            }
        }

    fun continueFromExplanation() = mutableState.update { it.copy(dialog = null, requestPermission = true) }

    fun onPermissionRequested() = mutableState.update { it.copy(requestPermission = false) }

    /**
     * The system dialog was answered or interrupted; [explanationDue] is the rationale flag after the request. The result map is
     * not trusted: the access is read again, so a cancelled request or a partial grant counts as not granted. Not granted with no
     * explanation due means Android will not ask again (or the dialog was dismissed): the settings dialog shows at once.
     */
    fun onPermissionResult(explanationDue: Boolean) =
        serially {
            val granted = access.current() == CalendarAccessState.Granted
            recompute()
            when {
                granted -> showChooser(onlyIfUnusable = true)
                !explanationDue -> mutableState.update { it.copy(dialog = SettingsDialog.DeniedForGood) }
            }
        }

    /** Opens the chooser; with [onlyIfUnusable], only when no usable calendar is chosen. */
    fun openChooser(onlyIfUnusable: Boolean = false) = serially { showChooser(onlyIfUnusable) }

    private suspend fun showChooser(onlyIfUnusable: Boolean) {
        val chosen = channel as? CalendarChannel.On
        if (onlyIfUnusable && chosen != null && gateway.find(chosen.calendarId) != null) return
        val calendars = gateway.writableCalendars()
        val dialog =
            if (calendars.isEmpty()) SettingsDialog.NoCalendar else SettingsDialog.Chooser(calendars, chosen?.calendarId)
        mutableState.update { it.copy(dialog = dialog) }
    }

    /** Stores the choice only while calendar access is there and the calendar still exists; otherwise the screen is brought up to date. */
    fun choose(calendar: DeviceCalendar) {
        mutableState.update { it.copy(dialog = null) }
        serially {
            when {
                access.current() != CalendarAccessState.Granted -> recompute()
                gateway.find(calendar.id) == null -> showChooser(onlyIfUnusable = false)
                else -> settings.set(CalendarChannel.On(calendar.id))
            }
        }
    }

    fun dismissDialog() = mutableState.update { it.copy(dialog = null) }

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
