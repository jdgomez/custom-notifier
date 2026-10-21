package dev.jdgomez.customnotifier.productform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jdgomez.customnotifier.R

/** A number field without a label, as wide as six digits at the current font size. */
@Composable
private fun NumberBox(
    field: ProductFormField,
    state: ProductFormState,
    onTextChange: (ProductFormField, String) -> Unit,
    requesters: FieldFocus,
    description: Int,
) {
    val description = stringResource(description)
    val digitsWidth = with(LocalDensity.current) { 56.sp.toDp() }
    OutlinedTextField(
        value = state[field],
        onValueChange = { onTextChange(field, it) },
        modifier =
            Modifier
                .width(digitsWidth + 32.dp)
                .focusRequester(requesters[field])
                .testTag(field.name)
                .semantics { contentDescription = description },
        isError = field in state.errors,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        singleLine = true,
    )
}

/** Words and number fields that stay together on one line; a wrapped [InlineLine] breaks only between groups. */
@Composable
private fun InlineGroup(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Fields written inline with words; they wrap at large font sizes. Errors and help go below the line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineLine(
    state: ProductFormState,
    first: ProductFormField,
    second: ProductFormField? = null,
    help: Int? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) { content() }
        val errors = listOfNotNull(first, second).filter { it in state.errors }
        errors.forEach { SupportingText(errorText(it), error = true) }
        if (errors.isEmpty() && help != null) SupportingText(stringResource(help), error = false)
    }
}

@Composable
private fun SupportingText(
    text: String,
    error: Boolean,
) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun RuleRow(
    state: ProductFormState,
    onTextChange: (ProductFormField, String) -> Unit,
    requesters: FieldFocus,
) {
    val unit = state[ProductFormField.Unit].trim().ifEmpty { stringResource(R.string.product_form_consumption_unit_fallback) }
    InlineLine(state, ProductFormField.ConsumptionUnits, ProductFormField.ConsumptionDays) {
        InlineGroup {
            NumberBox(
                ProductFormField.ConsumptionUnits,
                state,
                onTextChange,
                requesters,
                R.string.product_form_consumption_units_description,
            )
            Text(stringResource(R.string.product_form_consumption_unit_every, unit))
        }
        InlineGroup {
            NumberBox(
                ProductFormField.ConsumptionDays,
                state,
                onTextChange,
                requesters,
                R.string.product_form_consumption_days_description,
            )
            Text(stringResource(R.string.product_form_consumption_days))
        }
    }
}

@Composable
internal fun LeadTimeRow(
    state: ProductFormState,
    onTextChange: (ProductFormField, String) -> Unit,
    requesters: FieldFocus,
) {
    InlineLine(state, ProductFormField.LeadTime, help = R.string.product_form_lead_time_help) {
        InlineGroup {
            Text(stringResource(R.string.product_form_lead_time_before))
            NumberBox(ProductFormField.LeadTime, state, onTextChange, requesters, R.string.product_form_lead_time_description)
        }
        // A phrase, not a single word: when it does not fit beside the field it goes whole onto the next line.
        Text(stringResource(R.string.product_form_lead_time_after))
    }
}
