package dev.jdgomez.customnotifier

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlaceholderScreenScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun placeholderScreen() {
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/placeholder_screen.png",
            roborazziOptions =
                RoborazziOptions(
                    // The default comparator tolerates small per-pixel color distances; maxDistance = 0
                    // makes any color change on any pixel a failure.
                    compareOptions =
                        RoborazziOptions.CompareOptions(
                            changeThreshold = 0f,
                            imageComparator = SimpleImageComparator(maxDistance = 0f),
                        ),
                ),
        )
    }
}
