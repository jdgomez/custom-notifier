package dev.jdgomez.customnotifier.productform

import android.os.Looper
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import dev.jdgomez.customnotifier.productlist.FakeRepository
import dev.jdgomez.customnotifier.productlist.MutableClock
import dev.jdgomez.customnotifier.productlist.RowStatus
import dev.jdgomez.customnotifier.ui.CustomNotifierTheme
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val FILLED =
    ProductFormState().let { initial ->
        initial.copy(
            texts =
                initial.texts +
                    mapOf(
                        ProductFormField.Name to "Vitamin D",
                        ProductFormField.Unit to "pills",
                        ProductFormField.PackageSize to "90",
                        ProductFormField.ConsumptionUnits to "3",
                        ProductFormField.ConsumptionDays to "2",
                        ProductFormField.UnitsNow to "30",
                    ),
            preview = RowStatus.Upcoming(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 5)),
        )
    }

private val WITH_ERRORS =
    ProductFormState(
        texts = ProductFormState.INITIAL_TEXTS + (ProductFormField.Name to "  ") + (ProductFormField.ConsumptionDays to "400"),
        errors =
            setOf(ProductFormField.Name, ProductFormField.Unit, ProductFormField.PackageSize, ProductFormField.ConsumptionUnits) +
                ProductFormField.ConsumptionDays + ProductFormField.UnitsNow,
    )

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w360dp-h800dp-xhdpi")
class ProductFormScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val options =
        RoborazziOptions(
            // maxDistance = 0 makes any color change on any pixel a failure.
            compareOptions =
                RoborazziOptions.CompareOptions(changeThreshold = 0f, imageComparator = SimpleImageComparator(maxDistance = 0f)),
        )

    private fun settle() {
        composeRule.waitForIdle()
        // See ProductListScreenshotTest: identical screens rendered in one JVM are captured blank otherwise.
        val root = composeRule.onRoot().fetchSemanticsNode().root as View
        composeRule.runOnUiThread { root.invalidate() }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun capture(
        name: String,
        state: ProductFormState,
        darkTheme: Boolean = false,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                CustomNotifierTheme(darkTheme) { ProductFormScreen(state, emptyFlow(), { _, _ -> }, {}, {}) }
            }
        }
        settle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/product_form_$name.png", roborazziOptions = options)
    }

    @Test
    fun empty() = capture("empty", ProductFormState())

    @Test
    fun errors() = capture("errors", WITH_ERRORS)

    @Test
    fun filledWithPreview() = capture("filled", FILLED)

    @Test
    fun filledDark() = capture("filled_dark", FILLED, darkTheme = true)

    @Test
    fun errorsLargestFont() = capture("errors_font_2", WITH_ERRORS, fontScale = 2f)

    @Test
    @OptIn(ExperimentalRoborazziApi::class)
    fun discardDialog() {
        val viewModel = ProductFormViewModel(FakeRepository(), MutableClock(Instant.parse("2026-02-20T10:00:00Z"))) { ZoneOffset.UTC }
        composeRule.setContent { CustomNotifierTheme { ProductFormRoute(onClose = {}, viewModel = viewModel) } }
        composeRule.onNodeWithTag("Name").performTextReplacement("Vitamin D")
        composeRule.onNodeWithContentDescription("Navigate up").performClick()
        composeRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        captureScreenRoboImage("src/test/screenshots/product_form_discard.png", options)
    }
}
