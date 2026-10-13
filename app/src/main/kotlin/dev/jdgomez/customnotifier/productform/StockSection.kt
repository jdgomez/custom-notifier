package dev.jdgomez.customnotifier.productform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jdgomez.customnotifier.R
import dev.jdgomez.customnotifier.productlist.StockText

@Composable
internal fun StockDialogs(
    state: ProductFormState,
    onStockAction: (StockAction) -> Unit,
) {
    val mode = state.mode
    val summary = state.stock
    if (mode !is ProductFormMode.Edit || summary == null) return
    when (val dialog = state.stockDialog) {
        is StockDialog.Restock -> RestockDialog(mode.storedName, summary.unitLabel, dialog, onStockAction)
        is StockDialog.Adjust -> AdjustDialog(summary.unitLabel, dialog, onStockAction)
        null -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StockSection(
    summary: StockSummary,
    onAction: (StockAction) -> Unit,
) {
    Column(Modifier.fillMaxWidth().testTag("stock_section"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.stock_section_title), style = MaterialTheme.typography.titleSmall)
        StockText(summary.stock, summary.unitLabel, Modifier.testTag("stock"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onAction(StockAction.OpenRestock) }, modifier = Modifier.testTag("restock")) {
                Text(stringResource(R.string.stock_restock))
            }
            OutlinedButton(onClick = { onAction(StockAction.OpenAdjust) }, modifier = Modifier.testTag("adjust")) {
                Text(stringResource(R.string.stock_adjust))
            }
        }
    }
}
