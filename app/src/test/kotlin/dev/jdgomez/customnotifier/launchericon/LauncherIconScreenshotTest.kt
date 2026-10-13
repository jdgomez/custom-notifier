package dev.jdgomez.customnotifier.launchericon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.jdgomez.customnotifier.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertNotNull

// An adaptive icon is a 108dp canvas of which the launcher shows a mask-shaped 72dp window; drawn at 4 px per dp.
private const val PX_PER_DP = 4
private const val CANVAS_DP = 108
private const val WINDOW_DP = 72

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class LauncherIconScreenshotTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun icon() = context.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable

    // Draws the layers over white into the visible 72dp window, optionally clipped to a circle like a launcher mask.
    private fun render(
        layers: List<Drawable>,
        circleMask: Boolean,
    ): Bitmap {
        val window = WINDOW_DP * PX_PER_DP
        val canvasPx = CANVAS_DP * PX_PER_DP
        val bitmap = Bitmap.createBitmap(window, window, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        if (circleMask) {
            canvas.clipPath(Path().apply { addCircle(window / 2f, window / 2f, window / 2f, Path.Direction.CW) })
        }
        canvas.translate(-(canvasPx - window) / 2f, -(canvasPx - window) / 2f)
        layers.forEach {
            it.setBounds(0, 0, canvasPx, canvasPx)
            it.draw(canvas)
        }
        return bitmap
    }

    private fun verify(
        bitmap: Bitmap,
        name: String,
    ) = bitmap.captureRoboImage(
        filePath = "src/test/screenshots/launcher_icon_$name.png",
        roborazziOptions =
            RoborazziOptions(
                compareOptions =
                    RoborazziOptions.CompareOptions(
                        changeThreshold = 0f,
                        imageComparator = SimpleImageComparator(maxDistance = 0f),
                    ),
            ),
    )

    @Test
    fun adaptiveIconInCircleMask() {
        val icon = icon()
        verify(render(listOf(icon.background, icon.foreground), circleMask = true), "circle")
    }

    @Test
    fun monochromeLayerAlone() {
        val layer = assertNotNull(icon().monochrome)
        layer.setTint(Color.BLACK)
        verify(render(listOf(layer), circleMask = false), "monochrome")
    }
}
