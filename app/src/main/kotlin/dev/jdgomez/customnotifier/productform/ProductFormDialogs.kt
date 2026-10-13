package dev.jdgomez.customnotifier.productform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jdgomez.customnotifier.R
import dev.jdgomez.customnotifier.domain.EstimatedStock
import dev.jdgomez.customnotifier.productlist.StockText
import dev.jdgomez.customnotifier.ui.AppIcons
import java.text.NumberFormat

@Composable
internal fun DiscardDialog(
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(R.string.product_form_discard_title)) },
        confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(R.string.product_form_discard_confirm)) } },
        dismissButton = { TextButton(onClick = onKeepEditing) { Text(stringResource(R.string.product_form_discard_keep)) } },
    )
}

@Composable
internal fun DeleteDialog(
    name: String,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.product_form_delete_title, name)) },
        text = { Text(stringResource(R.string.product_form_delete_text)) },
        confirmButton = {
            TextButton(
                onClick = onDelete,
                colors = deleteColors(),
            ) { Text(stringResource(R.string.product_form_delete_confirm)) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.product_form_delete_cancel)) } },
    )
}

@Composable
internal fun deleteColors() = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RestockDialog(
    name: String,
    unitLabel: String,
    dialog: StockDialog.Restock,
    onAction: (StockAction) -> Unit,
) {
    val format = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0])
    AlertDialog(
        onDismissRequest = { onAction(StockAction.Dismiss) },
        title = { Text(stringResource(R.string.stock_restock_title, name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedIconButton(
                        onClick = { onAction(StockAction.Step(-1)) },
                        enabled = dialog.packages > StockLimits.packages.first,
                    ) { Icon(AppIcons.Remove, contentDescription = stringResource(R.string.stock_restock_fewer)) }
                    Text(
                        format.format(dialog.packages),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("packages"),
                    )
                    OutlinedIconButton(
                        onClick = { onAction(StockAction.Step(1)) },
                        enabled = dialog.packages < StockLimits.packages.last,
                    ) { Icon(AppIcons.Add, contentDescription = stringResource(R.string.stock_restock_more)) }
                }
                Text(stringResource(R.string.stock_restock_added, format.format(dialog.addedUnits), unitLabel))
                StockAfter(dialog.stockAfter, unitLabel)
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(StockAction.Confirm) }, modifier = Modifier.testTag("stock_confirm")) {
                Text(stringResource(R.string.stock_restock))
            }
        },
        dismissButton = { TextButton(onClick = { onAction(StockAction.Dismiss) }) { Text(stringResource(R.string.stock_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdjustDialog(
    unitLabel: String,
    dialog: StockDialog.Adjust,
    onAction: (StockAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(StockAction.Dismiss) },
        title = { Text(stringResource(R.string.stock_adjust_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val labels = listOf(R.string.stock_adjust_add to false, R.string.stock_adjust_remove to true)
                    labels.forEachIndexed { index, (label, remove) ->
                        SegmentedButton(
                            selected = dialog.remove == remove,
                            onClick = { onAction(StockAction.SetMode(remove)) },
                            shape = SegmentedButtonDefaults.itemShape(index, labels.size),
                        ) { Text(stringResource(label)) }
                    }
                }
                UnitsField(dialog.units) { onAction(StockAction.SetUnits(it)) }
                dialog.stockAfter?.let { StockAfter(it, unitLabel) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(StockAction.Confirm) },
                enabled = dialog.stockAfter != null,
                modifier = Modifier.testTag("stock_confirm"),
            ) {
                Text(stringResource(R.string.stock_adjust))
            }
        },
        dismissButton = { TextButton(onClick = { onAction(StockAction.Dismiss) }) { Text(stringResource(R.string.stock_cancel)) } },
    )
}

@Composable
private fun StockAfter(
    stock: EstimatedStock,
    unitLabel: String,
) {
    StockText(stock, unitLabel, label = stringResource(R.string.stock_after))
}

/** The units field of the adjust dialog; it takes focus (and so the keyboard) once it is laid out, when it is surely attached. */
@Composable
private fun UnitsField(
    units: String,
    onUnitsChange: (String) -> Unit,
) {
    val focus = remember { FocusRequester() }
    var focused by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = units,
        onValueChange = onUnitsChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .onGloballyPositioned {
                    if (!focused) {
                        focused = true
                        focus.requestFocus()
                    }
                }.testTag("adjust_units"),
        label = { Text(stringResource(R.string.stock_adjust_units)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        singleLine = true,
    )
}
