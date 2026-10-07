package dev.jdgomez.customnotifier.productform

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.jdgomez.customnotifier.R

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
