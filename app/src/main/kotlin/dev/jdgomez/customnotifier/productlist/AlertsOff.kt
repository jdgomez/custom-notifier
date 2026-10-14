package dev.jdgomez.customnotifier.productlist

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.jdgomez.customnotifier.R

@Composable
internal fun AlertsOffBanner(
    onTurnOn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.bodyMedium
    val twoLines = with(LocalDensity.current) { textStyle.lineHeight.toPx() } * 2
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer) {
        // The button sits to the right of the text while the text needs at most two lines beside it; otherwise it moves
        // below the text, at the end, and the text uses the full width.
        Layout(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            content = {
                Text(
                    text = stringResource(R.string.alerts_off_text),
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                TextButton(onClick = onTurnOn) {
                    Text(stringResource(R.string.alerts_off_turn_on), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            },
        ) { (textMeasurable, buttonMeasurable), constraints ->
            val width = constraints.maxWidth
            val gap = 8.dp.roundToPx()
            val button = buttonMeasurable.measure(Constraints())
            val besideWidth = (width - button.width - gap).coerceAtLeast(0)
            if (textMeasurable.minIntrinsicHeight(besideWidth) <= twoLines) {
                val text = textMeasurable.measure(Constraints(maxWidth = besideWidth))
                val height = maxOf(text.height, button.height)
                layout(width, height) {
                    text.placeRelative(0, (height - text.height) / 2)
                    button.placeRelative(width - button.width, (height - button.height) / 2)
                }
            } else {
                val text = textMeasurable.measure(Constraints(maxWidth = width))
                layout(width, text.height + button.height) {
                    text.placeRelative(0, 0)
                    button.placeRelative(width - button.width, text.height)
                }
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
