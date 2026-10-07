package dev.jdgomez.customnotifier.productlist

import android.os.Looper
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.ui.CustomNotifierTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneOffset

private val NOW = Instant.parse("2026-03-05T10:00:00Z")
private const val LONG_NAME = "Extra strength dishwasher detergent tablets, lemon scent, family pack"

private fun products(firstName: String = "Detergent") =
    listOf(
        testProduct(firstName, NOW, units = 8, leadDays = 2),
        testProduct("Diapers", NOW, units = 17, halves = true),
        testProduct("Vitamin D", NOW, units = 0),
        testProduct("Shampoo", NOW, units = 12_000, unitLabel = "ml"),
        testProduct("Water filter", Instant.parse("2026-02-01T12:00:00Z")),
    )

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w360dp-h800dp-xhdpi")
class ProductListScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(
        name: String,
        darkTheme: Boolean = false,
        fontScale: Float = 1f,
        products: List<Product> = emptyList(),
        scrollToEnd: Boolean = false,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Screen(products.toListState(NOW, ZoneOffset.UTC), darkTheme)
            }
        }
        if (scrollToEnd) composeRule.onNode(hasScrollAction()).performScrollToIndex(products.lastIndex)
        composeRule.waitForIdle()
        // A screen identical to one rendered by an earlier test in the same JVM (the empty state) is captured
        // blank unless the root view is invalidated and the main looper drained first.
        val root = composeRule.onRoot().fetchSemanticsNode().root as View
        composeRule.runOnUiThread { root.invalidate() }
        shadowOf(Looper.getMainLooper()).idle()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/product_list_$name.png",
            roborazziOptions =
                RoborazziOptions(
                    // maxDistance = 0 makes any color change on any pixel a failure.
                    compareOptions =
                        RoborazziOptions.CompareOptions(
                            changeThreshold = 0f,
                            imageComparator = SimpleImageComparator(maxDistance = 0f),
                        ),
                ),
        )
    }

    @Composable
    private fun Screen(
        state: ProductListState,
        darkTheme: Boolean,
    ) = CustomNotifierTheme(darkTheme) { ProductListScreen(state, onAddProduct = {}) }

    @Test
    fun empty() {
        capture("empty")
    }

    @Test
    fun rows() = capture("rows", products = products())

    @Test
    fun rowsDark() = capture("rows_dark", darkTheme = true, products = products())

    @Test
    fun rowsScrolledToTheEnd() =
        capture("rows_end", products = products() + (1..6).map { testProduct("Item $it", NOW) }, scrollToEnd = true)

    @Test
    fun rowsLargestFont() {
        capture("rows_font_2", fontScale = 2f, products = products(LONG_NAME))
    }
}
