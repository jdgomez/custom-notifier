package dev.jdgomez.customnotifier.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jdgomez.customnotifier.R
import dev.jdgomez.customnotifier.calendar.CalendarAccess
import dev.jdgomez.customnotifier.domain.DeviceCalendar
import dev.jdgomez.customnotifier.ui.AppIcons
import dev.jdgomez.customnotifier.ui.findActivity

@Composable
fun SettingsRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // The rationale flag before the request, to tell a denial for good from a first one.
    var explanationBefore by rememberSaveable { mutableStateOf(false) }
    var leftForSystemSettings by rememberSaveable { mutableStateOf(false) }
    val explanationDue = {
        context.findActivity()?.let { activity ->
            CalendarAccess.PERMISSIONS.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        } == true
    }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.onPermissionResult(explanationBefore, explanationDue())
        }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh(backFromSystemSettings = leftForSystemSettings)
        leftForSystemSettings = false
    }
    LaunchedEffect(state.requestPermission) {
        if (!state.requestPermission) return@LaunchedEffect
        viewModel.onPermissionRequested()
        explanationBefore = explanationDue()
        launcher.launch(CalendarAccess.PERMISSIONS)
    }
    SettingsScreen(
        state = state,
        onBack = onClose,
        onSwitchChange = { on -> if (on) viewModel.ask(explanationDue()) else viewModel.turnOff() },
        onAllow = { viewModel.ask(explanationDue()) },
        onChooseCalendar = viewModel::openChooser,
        onContinueFromExplanation = viewModel::continueFromExplanation,
        onChoose = viewModel::choose,
        onOpenSystemSettings = {
            viewModel.dismissDialog()
            leftForSystemSettings = true
            context.startActivity(viewModel.settingsIntent())
        },
        onDismissDialog = viewModel::dismissDialog,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList") // One callback per user action of a stateless screen.
fun SettingsScreen(
    state: SettingsState,
    onBack: () -> Unit,
    onSwitchChange: (Boolean) -> Unit,
    onAllow: () -> Unit,
    onChooseCalendar: () -> Unit,
    onContinueFromExplanation: () -> Unit,
    onChoose: (DeviceCalendar) -> Unit,
    onOpenSystemSettings: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(AppIcons.ArrowBack, stringResource(R.string.settings_back)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.settings_calendar_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_calendar_switch)) },
                supportingContent = { Text(stringResource(R.string.settings_calendar_switch_help)) },
                trailingContent = { Switch(checked = state.channelOn, onCheckedChange = null) },
                modifier =
                    Modifier.toggleable(
                        value = state.channelOn,
                        role = Role.Switch,
                        onValueChange = onSwitchChange,
                    ),
            )
            if (state.accessRemoved) AccessRemoved(onAllow)
            state.calendarRow?.let { CalendarRowItem(it, onChooseCalendar) }
        }
    }
    SettingsDialogs(state.dialog, onContinueFromExplanation, onChoose, onOpenSystemSettings, onDismissDialog)
}

@Composable
private fun AccessRemoved(onAllow: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(stringResource(R.string.settings_access_removed), color = MaterialTheme.colorScheme.error)
        },
        supportingContent = { TextButton(onClick = onAllow) { Text(stringResource(R.string.settings_access_allow)) } },
    )
}

@Composable
private fun CalendarRowItem(
    row: CalendarRow,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_calendar_row)) },
        supportingContent = {
            when (row) {
                is CalendarRow.Chosen -> {
                    Column {
                        Text(row.calendar.name)
                        Text(row.calendar.accountName)
                    }
                }
                CalendarRow.Unavailable ->
                    Text(stringResource(R.string.settings_calendar_unavailable), color = MaterialTheme.colorScheme.error)
            }
        },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun SettingsDialogs(
    dialog: SettingsDialog?,
    onContinue: () -> Unit,
    onChoose: (DeviceCalendar) -> Unit,
    onOpenSystemSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit
        SettingsDialog.Explanation ->
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.settings_explanation_title)) },
                text = { Text(stringResource(R.string.settings_explanation_text)) },
                confirmButton = { TextButton(onClick = onContinue) { Text(stringResource(R.string.settings_explanation_continue)) } },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_explanation_not_now)) } },
            )
        SettingsDialog.DeniedForGood ->
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.settings_denied_title)) },
                text = { Text(stringResource(R.string.settings_denied_text)) },
                confirmButton = { TextButton(onClick = onOpenSystemSettings) { Text(stringResource(R.string.settings_denied_open)) } },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_denied_cancel)) } },
            )
        is SettingsDialog.Chooser -> ChooserDialog(dialog, onChoose, onDismiss)
        SettingsDialog.NoCalendar ->
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.settings_no_calendar_title)) },
                text = { Text(stringResource(R.string.settings_no_calendar_text)) },
                confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_no_calendar_ok)) } },
            )
    }
}

@Composable
private fun ChooserDialog(
    chooser: SettingsDialog.Chooser,
    onChoose: (DeviceCalendar) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_chooser_title)) },
        text = {
            LazyColumn {
                items(chooser.calendars, key = { it.id.value }) { calendar ->
                    val selected = calendar.id == chooser.selected
                    Row(
                        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = { onChoose(calendar) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
                            Text(calendar.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                calendar.accountName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_chooser_cancel)) } },
    )
}
