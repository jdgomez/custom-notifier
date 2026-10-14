package dev.jdgomez.customnotifier.productlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jdgomez.customnotifier.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AlertsOffBanner(
    onTurnOn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer) {
        FlowRow(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.alerts_off_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            TextButton(onClick = onTurnOn) {
                Text(stringResource(R.string.alerts_off_turn_on), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

/** Shown before the system dialog when Android reports that the permission should be explained. */
@Composable
internal fun AlertsExplanationDialog(
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onNotNow,
        title = { Text(stringResource(R.string.alerts_explanation_title)) },
        text = { Text(stringResource(R.string.alerts_explanation_text)) },
        confirmButton = { TextButton(onClick = onContinue) { Text(stringResource(R.string.alerts_explanation_continue)) } },
        dismissButton = { TextButton(onClick = onNotNow) { Text(stringResource(R.string.alerts_explanation_not_now)) } },
    )
}
