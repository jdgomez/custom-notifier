package dev.jdgomez.customnotifier.productlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jdgomez.customnotifier.R
import dev.jdgomez.customnotifier.alerts.NotificationAccess
import dev.jdgomez.customnotifier.alerts.NotificationAccessState
import dev.jdgomez.customnotifier.domain.EstimatedStock
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.ui.AppIcons
import dev.jdgomez.customnotifier.ui.findActivity
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ProductListRoute(
    onAddProduct: () -> Unit,
    onEditProduct: (ProductId) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    askForNotifications: Boolean = false,
    onConsumeAskForNotifications: () -> Unit = {},
    viewModel: ProductListViewModel = viewModel(factory = ProductListViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var explaining by rememberSaveable { mutableStateOf(false) }
    // Whether the request in flight comes from the banner: only then a denial opens the notification settings.
    var userInitiated by rememberSaveable { mutableStateOf(false) }
    val explanationDue = { context.findActivity()?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, PERMISSION) } == true }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (viewModel.onPermissionResult(userInitiated, explanationDue())) {
                context.startActivity(viewModel.settingsIntent())
            }
        }
    // Returning from settings or from the system dialog: the state may have changed.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }
    val currentConsume by rememberUpdatedState(onConsumeAskForNotifications)
    LaunchedEffect(askForNotifications) {
        if (!askForNotifications) return@LaunchedEffect
        currentConsume()
        if (viewModel.refreshAccess() == NotificationAccessState.PermissionMissing) {
            if (explanationDue()) {
                explaining = true
            } else {
                userInitiated = false
                launcher.launch(PERMISSION)
            }
        }
    }
    ProductListScreen(
        state,
        onAddProduct,
        onEditProduct,
        modifier,
        onOpenSettings = onOpenSettings,
        onTurnOnAlerts = {
            val access = viewModel.refreshAccess()
            if (access == NotificationAccessState.PermissionMissing) {
                userInitiated = true
                launcher.launch(PERMISSION)
            } else if (access != NotificationAccessState.Allowed) {
                context.startActivity(viewModel.settingsIntent())
            }
        },
    )
    if (explaining) {
        AlertsExplanationDialog(onContinue = {
            explaining = false
            userInitiated = false
            launcher.launch(PERMISSION)
        }, onNotNow = { explaining = false })
    }
}

private const val PERMISSION = NotificationAccess.PERMISSION

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    state: ProductListState,
    onAddProduct: () -> Unit,
    onEditProduct: (ProductId) -> Unit,
    modifier: Modifier = Modifier,
    onTurnOnAlerts: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(AppIcons.Settings, contentDescription = stringResource(R.string.settings_action))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                icon = { Icon(AppIcons.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.product_list_add)) },
            )
        },
    ) { padding ->
        when (state) {
            ProductListState.Loading -> Unit
            ProductListState.Empty -> EmptyState(Modifier.padding(padding))
            is ProductListState.Products ->
                Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                    if (state.alertsOff) AlertsOffBanner(onTurnOnAlerts)
                    ProductList(
                        state,
                        onEditProduct,
                        PaddingValues(bottom = padding.calculateBottomPadding()),
                        Modifier.weight(1f),
                    )
                }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.product_list_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.product_list_empty_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProductList(
    products: ProductListState.Products,
    onEditProduct: (ProductId) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // Room below the last row for the floating action button (56dp plus its 16dp margins).
    val padding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 88.dp)
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = padding) {
        items(products.rows, key = { it.id.value }) { row ->
            ProductRowItem(row, onClick = { onEditProduct(row.id) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun ProductRowItem(
    row: ProductRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClickLabel = stringResource(R.string.product_list_edit), role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                // One item for screen readers; its children keep their own text.
                .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = row.name, style = MaterialTheme.typography.titleMedium)
        StockText(row.stock, row.unitLabel)
        when (val status = row.status) {
            is RowStatus.Upcoming -> {
                DateText(stringResource(R.string.product_list_runs_out, status.depletionDate.format(dateFormat)))
                if (status.alertDate == null) {
                    AlertDue()
                } else {
                    DateText(stringResource(R.string.product_list_alert, status.alertDate.format(dateFormat)))
                }
            }
            is RowStatus.RanOut ->
                DateText(stringResource(R.string.product_list_ran_out, status.depletionDate.format(dateFormat)))
        }
    }
}

private fun LocalDate.format(formatter: DateTimeFormatter): String = formatter.format(this)

/**
 * The estimated [stock] in [unitLabel] with the list's rounding rules: "≈ " when rounded down. [label], when given, goes first
 * (for example "Stock after:"). The number and the unit label are joined by a no-break space (see the strings) so the value wraps as one.
 */
@Composable
internal fun StockText(
    stock: EstimatedStock,
    unitLabel: String,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val number = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0]).format(stock.wholeUnits)
    val whole = stringResource(R.string.product_list_stock, number, unitLabel)
    val prefix = stringResource(R.string.product_list_stock_approximate_prefix)
    val zeroStyle = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    val text =
        buildAnnotatedString {
            if (label != null) append(stringResource(R.string.stock_labeled, label, ""))
            if (stock.rounded) withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(prefix) }
            if (stock.wholeUnits.signum() == 0) withStyle(zeroStyle) { append(whole) } else append(whole)
        }
    val description =
        if (stock.rounded) {
            val approximate = stringResource(R.string.product_list_stock_approximate_description, number, unitLabel)
            if (label == null) approximate else stringResource(R.string.stock_labeled, label, approximate)
        } else {
            null
        }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = if (description == null) modifier else modifier.semantics { contentDescription = description },
    )
}

@Composable
private fun DateText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun AlertDue() {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = stringResource(R.string.product_list_alert_due),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
