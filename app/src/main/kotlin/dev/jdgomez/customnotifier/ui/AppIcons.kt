package dev.jdgomez.customnotifier.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The two Material icons the app needs, as vectors, to avoid the material-icons dependency. */
private const val ICON_SIZE = 24f

object AppIcons {
    val Add: ImageVector = icon("Add", autoMirror = false, "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")

    val ArrowBack: ImageVector = icon("ArrowBack", autoMirror = true, "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")

    private fun icon(
        name: String,
        autoMirror: Boolean,
        pathData: String,
    ): ImageVector =
        ImageVector
            .Builder(name, ICON_SIZE.dp, ICON_SIZE.dp, ICON_SIZE, ICON_SIZE, autoMirror = autoMirror)
            .addPath(addPathNodes(pathData), fill = SolidColor(Color.Black))
            .build()
}
