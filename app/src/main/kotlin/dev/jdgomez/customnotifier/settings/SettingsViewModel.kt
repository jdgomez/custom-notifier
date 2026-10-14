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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Settings screen: the calendar channel switch, the chosen calendar and the calendar access flow. Turning the switch on never
 * stores the channel by itself: only choosing a calendar does.
 */
class SettingsViewModel(
    private val settings: CalendarChannelSettings,
    private val gateway: CalendarGateway,
    private val access: CalendarAccess,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = mutableState.asStateFlow()

    private var channel: CalendarChannel = CalendarChannel.Off

    // Only the latest check may write the state: an older one finishing late would show a stale answer.
    private var recomputeJob: Job? = null

    init {
        viewModelScope.launch {
            settings.observe().collect {
                channel = it
                refresh()
            }
        }
    }

    /**
     * Checks calendar access and the chosen calendar again; the screen calls it whenever it becomes visible. With
     * [backFromSystemSettings] the permission may have been reset to "ask every time" there, so a recorded denial is forgotten.
     */
    fun refresh(backFromSystemSettings: Boolean = false) {
        if (backFromSystemSettings) access.clearPermanentDenial()
        recomputeJob?.cancel()
        recomputeJob = viewModelScope.launch { recompute() }
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

    fun turnOff() {
        viewModelScope.launch { settings.set(CalendarChannel.Off) }
    }

    /**
     * The switch turned on, or the "Allow" button: asks for calendar access, then opens the chooser when no usable calendar is
     * chosen. [explanationDue] is Android's rationale flag, known only to the screen.
     */
    fun ask(explanationDue: Boolean) {
        // Android only reports a rationale when the dialog can still be shown: a recorded permanent denial is stale.
        if (explanationDue) access.clearPermanentDenial()
        viewModelScope.launch {
            when (val current = access.current()) {
                CalendarAccessState.Granted -> openChooser(onlyIfUnusable = true)
                is CalendarAccessState.Missing ->
                    when {
                        !current.canAsk -> mutableState.update { it.copy(dialog = SettingsDialog.DeniedForGood) }
                        explanationDue -> mutableState.update { it.copy(dialog = SettingsDialog.Explanation) }
                        else -> mutableState.update { it.copy(requestPermission = true) }
                    }
            }
        }
    }

    fun continueFromExplanation() = mutableState.update { it.copy(dialog = null, requestPermission = true) }

    fun onPermissionRequested() = mutableState.update { it.copy(requestPermission = false) }

    /**
     * The system dialog was answered or interrupted; [explanationBefore] and [explanationAfter] are the rationale flags around the
     * request. The result map is not trusted: the access is read again, so a cancelled request counts as not granted.
     */
    fun onPermissionResult(
        explanationBefore: Boolean,
        explanationAfter: Boolean,
    ) {
        viewModelScope.launch {
            val granted = access.current() == CalendarAccessState.Granted
            val noDialog = !granted && access.recordDenied(explanationBefore, explanationAfter)
            recompute()
            when {
                granted -> openChooser(onlyIfUnusable = true)
                // Android showed no dialog: only the system settings can allow access.
                noDialog -> mutableState.update { it.copy(dialog = SettingsDialog.DeniedForGood) }
            }
        }
    }

    /** Opens the chooser; with [onlyIfUnusable], only when no usable calendar is chosen. */
    fun openChooser(onlyIfUnusable: Boolean = false) {
        viewModelScope.launch {
            val chosen = channel as? CalendarChannel.On
            if (onlyIfUnusable && chosen != null && gateway.find(chosen.calendarId) != null) return@launch
            val calendars = gateway.writableCalendars()
            val dialog =
                if (calendars.isEmpty()) SettingsDialog.NoCalendar else SettingsDialog.Chooser(calendars, chosen?.calendarId)
            mutableState.update { it.copy(dialog = dialog) }
        }
    }

    /** Stores the choice only while calendar access is there and the calendar still exists; otherwise the screen is brought up to date. */
    fun choose(calendar: DeviceCalendar) {
        mutableState.update { it.copy(dialog = null) }
        viewModelScope.launch {
            when {
                access.current() != CalendarAccessState.Granted -> recompute()
                gateway.find(calendar.id) == null -> openChooser()
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
