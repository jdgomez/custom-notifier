package dev.jdgomez.customnotifier.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.jdgomez.customnotifier.calendar.CalendarAccess
import dev.jdgomez.customnotifier.calendar.CalendarAccessState
import dev.jdgomez.customnotifier.calendar.CalendarProblem
import dev.jdgomez.customnotifier.calendar.ChannelCheck
import dev.jdgomez.customnotifier.calendar.checkChannel
import dev.jdgomez.customnotifier.customNotifierContainer
import dev.jdgomez.customnotifier.domain.CalendarChannel
import dev.jdgomez.customnotifier.domain.CalendarChannelSettings
import dev.jdgomez.customnotifier.domain.CalendarGateway
import dev.jdgomez.customnotifier.domain.DeviceCalendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Settings screen: the calendar channel switch, the chosen calendar and the calendar access flow. Turning the switch on never
 * stores the channel by itself: only choosing a calendar does.
 *
 * Events that need no I/O update the state at once on the main thread. The gateway reads run in two jobs, each of which a newer
 * request supersedes: the one that recomputes the calendar row and the one that opens a dialog.
 */
@Suppress("TooManyFunctions") // One function per user action of the screen.
class SettingsViewModel(
    private val settings: CalendarChannelSettings,
    private val gateway: CalendarGateway,
    private val access: CalendarAccess,
    private val onAccessGranted: () -> Unit = {},
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = mutableState.asStateFlow()

    private var channel: CalendarChannel = CalendarChannel.Off
    private var rowJob: Job? = null
    private var dialogJob: Job? = null

    init {
        viewModelScope.launch {
            settings.observe().collect {
                channel = it
                refresh()
            }
        }
    }

    /** Checks calendar access and the chosen calendar again; the screen calls it whenever it becomes visible. */
    fun refresh() {
        rowJob = relaunch(rowJob) { recompute() }
    }

    fun turnOff() {
        viewModelScope.launch {
            settings.set(CalendarChannel.Off)
            channel = CalendarChannel.Off
            refresh()
        }
    }

    /**
     * The switch turned on, or the "Allow" button: always asks Android, after the explanation when [explanationDue] (Android's
     * rationale flag, known only to the screen); opens the chooser instead when access is there and no usable calendar is chosen.
     */
    fun ask(explanationDue: Boolean) {
        when (access.current()) {
            CalendarAccessState.Granted -> openChooser(onlyIfUnusable = true)
            CalendarAccessState.Missing ->
                mutableState.update {
                    if (explanationDue) it.copy(dialog = SettingsDialog.Explanation) else it.copy(requestPermission = true)
                }
        }
    }

    fun continueFromExplanation() = mutableState.update { it.copy(dialog = null, requestPermission = true) }

    /** The screen is launching the system dialog: the request is consumed in the same step, so a recreation cannot launch it again. */
    fun onPermissionRequested() = mutableState.update { it.copy(requestPermission = false) }

    /**
     * The system dialog was answered or interrupted; [explanationDue] is the rationale flag after the request. The result map is
     * not trusted: the access is read again, so a cancelled request or a partial grant counts as not granted. Not granted with no
     * explanation due means Android will not ask again (or the dialog was dismissed): the settings dialog shows at once.
     */
    fun onPermissionResult(explanationDue: Boolean) {
        refresh()
        when {
            access.current() == CalendarAccessState.Granted -> openChooser(onlyIfUnusable = true)
            !explanationDue -> mutableState.update { it.copy(dialog = SettingsDialog.DeniedForGood) }
        }
    }

    /** Opens the chooser; with [onlyIfUnusable], only when no usable calendar is chosen. */
    fun openChooser(onlyIfUnusable: Boolean = false) {
        dialogJob = relaunch(dialogJob) { showChooser(onlyIfUnusable) }
    }

    /** Stores the choice only while calendar access is there and the calendar still exists; otherwise the screen is brought up to date. */
    fun choose(calendar: DeviceCalendar) {
        mutableState.update { it.copy(dialog = null) }
        dialogJob =
            relaunch(dialogJob) {
                when {
                    access.current() != CalendarAccessState.Granted -> Unit
                    gateway.find(calendar.id) == null -> showChooser(onlyIfUnusable = false)
                    else -> {
                        settings.set(CalendarChannel.On(calendar.id))
                        // The chosen value is used as it is: the stored channel reaches [channel] later and must not be re-read.
                        channel = CalendarChannel.On(calendar.id)
                    }
                }
                refresh()
            }
    }

    fun dismissDialog() {
        dialogJob?.cancel()
        mutableState.update { it.copy(dialog = null) }
    }

    /** Cancels [previous] and runs [block]; a failure of the gateway or the store leaves the screen as it is instead of crashing. */
    @Suppress("TooGenericExceptionCaught") // Any failure of an adapter must leave the screen usable.
    private fun relaunch(
        previous: Job?,
        block: suspend () -> Unit,
    ): Job {
        previous?.cancel()
        return viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(dialog = null) }
            }
        }
    }

    private suspend fun recompute() {
        val check = checkChannel(channel, access, gateway)
        val row =
            when (check) {
                is ChannelCheck.Working -> CalendarRow.Chosen(check.calendar)
                ChannelCheck.Broken(CalendarProblem.CalendarGone) -> CalendarRow.Unavailable
                else -> null
            }
        // Access that was missing while the channel is on and is back, however the user granted it: the events can be written now.
        val accessRemoved = check == ChannelCheck.Broken(CalendarProblem.AccessRemoved)
        val accessBack =
            mutableState.value.accessRemoved &&
                (check is ChannelCheck.Working || check == ChannelCheck.Broken(CalendarProblem.CalendarGone))
        if (accessBack) onAccessGranted()
        mutableState.update {
            it.copy(
                channelOn = check != ChannelCheck.Off,
                calendarRow = row,
                accessRemoved = accessRemoved,
            )
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

    fun settingsIntent(): Intent = access.settingsIntent()

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val container = customNotifierContainer()
                    SettingsViewModel(
                        container.calendarChannelSettings,
                        container.calendarGateway,
                        container.calendarAccess,
                        container::syncCalendar,
                    )
                }
            }
    }
}
