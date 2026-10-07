package dev.jdgomez.customnotifier.productform

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jdgomez.customnotifier.R
import dev.jdgomez.customnotifier.productlist.RowStatus
import dev.jdgomez.customnotifier.ui.AppIcons
import kotlinx.coroutines.flow.Flow
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ProductFormRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductFormViewModel = viewModel(factory = ProductFormViewModel.factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProductFormScreen(state, viewModel.events, viewModel::onTextChange, viewModel::onSave, viewModel::onDelete, onClose, modifier)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductFormScreen(
    state: ProductFormState,
    events: Flow<ProductFormEvent>,
    onTextChange: (ProductFormField, String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val requesters = remember { FieldFocus() }
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                ProductFormEvent.Saved, ProductFormEvent.Closed -> currentOnClose()
                is ProductFormEvent.FocusField -> requesters[event.field].requestFocus()
            }
        }
    }
    // Only while input is unsaved: otherwise back pops the screen with the system animation.
    BackHandler(enabled = state.isDirty) { confirmDiscard = true }
    val back = { if (state.isDirty) confirmDiscard = true else onClose() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.mode is ProductFormMode.New) R.string.product_form_title else R.string.product_form_title_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(AppIcons.ArrowBack, stringResource(R.string.product_form_navigate_up))
                    }
                },
                actions = { TextButton(onClick = onSave) { Text(stringResource(R.string.product_form_save)) } },
            )
        },
    ) { padding ->
        if (state.mode !is ProductFormMode.Loading) {
            FormFields(state, onTextChange, { confirmDelete = true }, requesters, Modifier.padding(padding))
        }
    }
    if (confirmDiscard) {
        DiscardDialog(onKeepEditing = { confirmDiscard = false }, onDiscard = onClose)
    }
    val mode = state.mode
    if (confirmDelete && mode is ProductFormMode.Edit) {
        DeleteDialog(mode.storedName, onCancel = { confirmDelete = false }, onDelete = onDelete)
    }
}

/** One focus requester per field, so a refused save can focus the first invalid one. */
@Immutable
private class FieldFocus {
    private val requesters = ProductFormField.entries.associateWith { FocusRequester() }

    operator fun get(field: ProductFormField): FocusRequester = requesters.getValue(field)
}

@Composable
private fun FormFields(
    state: ProductFormState,
    onTextChange: (ProductFormField, String) -> Unit,
    onDeleteClick: () -> Unit,
    requesters: FieldFocus,
    modifier: Modifier = Modifier,
) {
    val text = KeyboardType.Text
    val number = KeyboardType.Number
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextInput(ProductFormField.Name, state, onTextChange, requesters, R.string.product_form_name, text)
        TextInput(ProductFormField.Unit, state, onTextChange, requesters, R.string.product_form_unit, text, R.string.product_form_unit_help)
        TextInput(ProductFormField.PackageSize, state, onTextChange, requesters, R.string.product_form_package_size, number)
        val unit = state[ProductFormField.Unit].trim().ifEmpty { stringResource(R.string.product_form_consumption_unit_fallback) }
        InlineLine(state, ProductFormField.ConsumptionUnits, ProductFormField.ConsumptionDays) {
            NumberBox(
                ProductFormField.ConsumptionUnits,
                state,
                onTextChange,
                requesters,
                R.string.product_form_consumption_units_description,
            )
            Text(stringResource(R.string.product_form_consumption_unit_every, unit))
            NumberBox(ProductFormField.ConsumptionDays, state, onTextChange, requesters, R.string.product_form_consumption_days_description)
            Text(stringResource(R.string.product_form_consumption_days))
        }
        InlineLine(state, ProductFormField.LeadTime, help = R.string.product_form_lead_time_help) {
            Text(stringResource(R.string.product_form_lead_time_before))
            NumberBox(ProductFormField.LeadTime, state, onTextChange, requesters, R.string.product_form_lead_time_description)
            Text(stringResource(R.string.product_form_lead_time_after))
        }
        if (ProductFormField.UnitsNow in state.fields) {
            TextInput(
                ProductFormField.UnitsNow,
                state,
                onTextChange,
                requesters,
                R.string.product_form_units_now,
                number,
                ime = ImeAction.Done,
            )
        }
        state.preview?.let { Preview(it) }
        if (state.mode is ProductFormMode.Edit) {
            TextButton(onClick = onDeleteClick, colors = deleteColors(), contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.product_form_delete))
            }
        }
    }
}

@Composable
private fun TextInput(
    field: ProductFormField,
    state: ProductFormState,
    onTextChange: (ProductFormField, String) -> Unit,
    requesters: FieldFocus,
    label: Int,
    keyboard: KeyboardType,
    help: Int? = null,
    ime: ImeAction = ImeAction.Next,
) {
    val error = field in state.errors
    OutlinedTextField(
        value = state[field],
        onValueChange = { onTextChange(field, it) },
        modifier = Modifier.fillMaxWidth().focusRequester(requesters[field]).testTag(field.name),
        label = { Text(stringResource(label)) },
        supportingText = (if (error) errorText(field) else help?.let { stringResource(it) })?.let { { Text(it) } },
        isError = error,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = keyboard, imeAction = ime),
        singleLine = true,
    )
}

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
private fun errorText(field: ProductFormField): String {
    val range = field.range
    return when {
        field == ProductFormField.Name -> stringResource(R.string.product_form_error_name)
        field == ProductFormField.Unit -> stringResource(R.string.product_form_error_unit)
        range != null -> {
            val format = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0])
            stringResource(R.string.product_form_error_range, format.format(range.first), format.format(range.last))
        }
        else -> error("every field has an error text")
    }
}

@Composable
private fun Preview(status: RowStatus) {
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(LocalConfiguration.current.locales[0])
    val text =
        when (status) {
            is RowStatus.RanOut -> stringResource(R.string.product_list_ran_out, dateFormat.format(status.depletionDate))
            is RowStatus.Upcoming ->
                stringResource(
                    R.string.product_form_preview,
                    stringResource(R.string.product_list_runs_out, dateFormat.format(status.depletionDate)),
                    status.alertDate?.let { stringResource(R.string.product_list_alert, dateFormat.format(it)) }
                        ?: stringResource(R.string.product_list_alert_due),
                )
        }
    Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().testTag("preview"))
}
