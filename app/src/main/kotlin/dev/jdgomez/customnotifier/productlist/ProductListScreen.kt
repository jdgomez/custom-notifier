package dev.jdgomez.customnotifier.productlist

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jdgomez.customnotifier.R
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.ui.AppIcons
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun ProductListRoute(
    onAddProduct: () -> Unit,
    onEditProduct: (ProductId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = viewModel(factory = ProductListViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProductListScreen(state, onAddProduct, onEditProduct, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    state: ProductListState,
    onAddProduct: () -> Unit,
    onEditProduct: (ProductId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
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
            is ProductListState.Products -> ProductList(state, onEditProduct, padding)
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
        StockText(row, locale)
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

@Composable
private fun StockText(
    row: ProductRow,
    locale: Locale,
) {
    val stock = row.stock
    val whole = stringResource(R.string.product_list_stock, NumberFormat.getIntegerInstance(locale).format(stock.wholeUnits), row.unitLabel)
    val prefix = stringResource(R.string.product_list_stock_approximate_prefix)
    val zeroStyle = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    val text =
        buildAnnotatedString {
            if (stock.rounded) withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(prefix) }
            if (stock.wholeUnits.signum() == 0) withStyle(zeroStyle) { append(whole) } else append(whole)
        }
    val description = if (stock.rounded) stringResource(R.string.product_list_stock_approximate_description, whole) else null
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = if (description == null) Modifier else Modifier.semantics { contentDescription = description },
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
