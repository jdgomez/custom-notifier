package dev.jdgomez.customnotifier.productform

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.jdgomez.customnotifier.R
import dev.jdgomez.customnotifier.ui.AppIcons

/**
 * The usual one-line top bar while the title fits next to the back arrow and the save action; the medium bar, which shows the
 * title in full below them, only when the measured title does not fit the width (never a guess from the font scale).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormTopBar(
    title: String,
    saveEnabled: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val saveLabel = stringResource(R.string.product_form_save)
    val navigationIcon = @Composable {
        IconButton(onClick = onBack) { Icon(AppIcons.ArrowBack, stringResource(R.string.product_form_navigate_up)) }
    }
    val actions: @Composable RowScope.() -> Unit = {
        TextButton(onClick = onSave, enabled = saveEnabled) { Text(saveLabel) }
    }
    val measurer = rememberTextMeasurer()
    val typography = MaterialTheme.typography
    val insets = TopAppBarDefaults.windowInsets.asPaddingValues()
    BoxWithConstraints {
        val density = LocalDensity.current
        val titleWidth = measurer.measure(title, typography.titleLarge, softWrap = false, maxLines = 1).size.width
        val saveWidth = measurer.measure(saveLabel, typography.labelLarge, softWrap = false, maxLines = 1).size.width
        val titleDp = with(density) { titleWidth.toDp() }
        // The bar's 4dp side paddings, the 48dp back arrow, and the save button: its label plus 12dp each side, at least 58dp wide.
        val saveDp = maxOf(with(density) { saveWidth.toDp() } + 24.dp, 58.dp)
        val sideInsets = insets.calculateLeftPadding(LayoutDirection.Ltr) + insets.calculateRightPadding(LayoutDirection.Ltr)
        val fits = titleDp <= maxWidth - sideInsets - 4.dp - 48.dp - saveDp - 4.dp
        if (fits) {
            TopAppBar(title = { Text(title) }, navigationIcon = navigationIcon, actions = actions)
        } else {
            MediumTopAppBar(title = { Text(title) }, navigationIcon = navigationIcon, actions = actions)
        }
    }
}
