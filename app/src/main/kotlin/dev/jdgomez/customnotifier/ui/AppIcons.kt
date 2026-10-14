package dev.jdgomez.customnotifier.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The Material icons the app needs, as vectors, to avoid the material-icons dependency. */
private const val ICON_SIZE = 24f

object AppIcons {
    val Add: ImageVector = icon("Add", autoMirror = false, "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")

    val Remove: ImageVector = icon("Remove", autoMirror = false, "M19,13H5v-2h14v2z")

    val ArrowBack: ImageVector = icon("ArrowBack", autoMirror = true, "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")

    val Settings: ImageVector =
        icon(
            "Settings",
            autoMirror = false,
            "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 " +
                "0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94l-0.36," +
                "-2.54c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41l-0.36,2.54c-0.59,0.24 -1.13,0.57 " +
                "-1.62,0.94l-2.39,-0.96c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84," +
                "11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 " +
                "0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 " +
                "0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92," +
                "-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6," +
                "1.62 3.6,3.6S13.98,15.6 12,15.6z",
        )

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
