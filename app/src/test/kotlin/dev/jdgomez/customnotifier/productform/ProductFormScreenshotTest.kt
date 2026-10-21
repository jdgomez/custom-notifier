package dev.jdgomez.customnotifier.productform

import android.content.Context
import android.os.Looper
import android.view.View
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import dev.jdgomez.customnotifier.R
import dev.jdgomez.customnotifier.domain.EstimatedStock
import dev.jdgomez.customnotifier.domain.Quantity
import dev.jdgomez.customnotifier.productlist.FakeRepository
import dev.jdgomez.customnotifier.productlist.MutableClock
import dev.jdgomez.customnotifier.productlist.RowStatus
import dev.jdgomez.customnotifier.productlist.testProduct
import dev.jdgomez.customnotifier.referencePath
import dev.jdgomez.customnotifier.ui.CustomNotifierTheme
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigInteger
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

private val EDIT =
    FILLED.let { filled ->
        val texts = filled.texts + (ProductFormField.UnitsNow to "")
        filled.copy(
            texts = texts,
            initialTexts = texts,
            mode = ProductFormMode.Edit("Vitamin D"),
            stock = StockSummary(EstimatedStock(Quantity.of(BigInteger.valueOf(59), BigInteger.TWO)), "pills"),
        )
    }

/** The rule row "2 cans every 1 days": at the largest font size the last word used to fall alone onto the next line. */
private val RULE_ROW =
    EDIT.let { edit ->
        val texts =
            edit.texts +
                mapOf(ProductFormField.Unit to "cans", ProductFormField.ConsumptionUnits to "2", ProductFormField.ConsumptionDays to "1")
        edit.copy(texts = texts, initialTexts = texts)
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
open class ProductFormScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val options =
        RoborazziOptions(
            // maxDistance = 0 makes any color change on any pixel a failure.
            compareOptions =
                RoborazziOptions.CompareOptions(changeThreshold = 0f, imageComparator = SimpleImageComparator(maxDistance = 0f)),
        )

    private fun text(id: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(id)

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
                CustomNotifierTheme(darkTheme) { ProductFormScreen(state, emptyFlow(), { _, _ -> }, {}, {}, {}) }
            }
        }
        settle()
        composeRule.onRoot().captureRoboImage(referencePath("product_form_$name", darkTheme), roborazziOptions = options)
    }

    /** Shows the form without the press ripple: the click that opens a dialog would leave a frame that differs between runs. */
    private fun setRouteWithoutRipple(viewModel: ProductFormViewModel) =
        composeRule.setContent {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                CustomNotifierTheme { ProductFormRoute(onClose = {}, viewModel = viewModel) }
            }
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
    fun edit() = capture("edit", EDIT)

    @Test
    fun editDark() = capture("edit_dark", EDIT, darkTheme = true)

    @Test
    fun editLargestFont() = capture("edit_font_2", EDIT, fontScale = 2f)

    private fun showEdit(
        state: ProductFormState,
        fontScale: Float,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                CustomNotifierTheme { ProductFormScreen(state, emptyFlow(), { _, _ -> }, {}, {}, {}) }
            }
        }
        settle()
    }

    private val spanish get() =
        ApplicationProvider
            .getApplicationContext<Context>()
            .resources.configuration.locales[0]
            .language == "es"

    private fun assertOneLineBar(fontScale: Float) {
        showEdit(EDIT, fontScale)
        val title = composeRule.onNodeWithText(text(R.string.product_form_title_edit)).getBoundsInRoot()
        val save = composeRule.onNodeWithText(text(R.string.product_form_save)).getBoundsInRoot()
        assert(title.right <= save.left) { "title $title not on the line of save $save at font scale $fontScale" }
        assert(kotlin.math.abs((title.top + title.bottom).value - (save.top + save.bottom).value) < 2f) {
            "title $title and save $save are not centered on one bar at font scale $fontScale"
        }
    }

    @Test
    fun titleThatFitsKeepsTheOneLineBarAtDefaultSize() = assertOneLineBar(1f)

    @Test
    fun titleThatFitsKeepsTheOneLineBarAtTheLargestSize() {
        // Only the English title fits at the largest size; the Spanish one is covered by the next test.
        assumeFalse("The Spanish title does not fit at the largest size", spanish)
        assertOneLineBar(2f)
    }

    @Test
    fun largestFontKeepsTheTopBarWholeAndTheRuleRowInPairs() {
        showEdit(RULE_ROW, fontScale = 2f)
        val root = composeRule.onRoot().getBoundsInRoot()
        val title = composeRule.onNodeWithText(text(R.string.product_form_title_edit)).getBoundsInRoot()
        val save = composeRule.onNodeWithText(text(R.string.product_form_save)).getBoundsInRoot()
        val stock = composeRule.onNodeWithText(text(R.string.stock_section_title)).getBoundsInRoot()
        assert(title.bottom <= stock.top) { "title $title overlaps the content $stock" }
        assert(save.right <= root.right && save.top >= root.top) { "save $save is outside $root" }
        // The Spanish title does not fit next to the save action: it is shown whole below it, and save stays on the top row.
        if (spanish) assert(title.top >= save.bottom) { "title $title is not below save $save" } else assert(title.right <= save.left)
        // The word after the days field stays on the line of its field.
        composeRule.onNodeWithTag("ConsumptionDays").performScrollTo()
        val days = composeRule.onNodeWithTag("ConsumptionDays").getBoundsInRoot()
        val word = composeRule.onNodeWithText(text(R.string.product_form_consumption_days)).getBoundsInRoot()
        assert(word.left >= days.right && word.top < days.bottom && word.bottom > days.top) { "days $word is not next to its field $days" }
    }

    @Test
    @OptIn(ExperimentalRoborazziApi::class)
    fun deleteDialog() {
        val repository = FakeRepository()
        val product = testProduct("Vitamin D", Instant.parse("2026-02-20T10:00:00Z"), units = 90)
        repository.products.value = listOf(product)
        val viewModel = ProductFormViewModel(repository, MutableClock(Instant.parse("2026-02-20T10:00:00Z")), product.id) { ZoneOffset.UTC }
        setRouteWithoutRipple(viewModel)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text(R.string.product_form_delete)).performScrollTo().performClick()
        composeRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        captureScreenRoboImage(referencePath("product_form_delete"), options)
    }

    @Test
    @OptIn(ExperimentalRoborazziApi::class)
    fun discardDialog() {
        val viewModel = ProductFormViewModel(FakeRepository(), MutableClock(Instant.parse("2026-02-20T10:00:00Z"))) { ZoneOffset.UTC }
        setRouteWithoutRipple(viewModel)
        composeRule.onNodeWithTag("Name").performTextReplacement("Vitamin D")
        composeRule.onNodeWithContentDescription(text(R.string.product_form_navigate_up)).performClick()
        composeRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        captureScreenRoboImage(referencePath("product_form_discard"), options)
    }

    /** Opens the edit form of a product holding 5 pills, taps [open] and captures the whole screen with the dialog on top. */
    @OptIn(ExperimentalRoborazziApi::class)
    private fun captureStockDialog(
        name: String,
        open: String,
        darkTheme: Boolean = false,
        fontScale: Float = 1f,
        productName: String = "Vitamin D",
        units: String? = null,
        stockUnits: Long = 5,
    ) {
        val now = Instant.parse("2026-02-20T10:00:00Z")
        val repository = FakeRepository()
        val product = testProduct(productName, now, units = stockUnits)
        repository.products.value = listOf(product)
        val viewModel = ProductFormViewModel(repository, MutableClock(now), product.id) { ZoneOffset.UTC }
        // The system font scale, not a LocalDensity override: dialogs are windows of their own and ignore the override.
        RuntimeEnvironment.setFontScale(fontScale)
        composeRule.setContent {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                CustomNotifierTheme(darkTheme) { ProductFormRoute(onClose = {}, viewModel = viewModel) }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(open).performClick()
        if (units != null) composeRule.onNodeWithTag("adjust_units").performTextInput(units)
        composeRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        captureScreenRoboImage(referencePath("product_form_$name", darkTheme), options)
    }

    @Test
    fun restockDialog() = captureStockDialog("restock", open = "restock")

    @Test
    fun restockDialogDark() = captureStockDialog("restock_dark", open = "restock", darkTheme = true)

    @Test
    fun restockDialogLargestFont() =
        captureStockDialog("restock_font_2", open = "restock", fontScale = 2f, productName = "Extra strength vitamin D3 drops for adults")

    @Test
    fun restockDialogLargestFontWrapsTheStockAsOneUnit() =
        captureStockDialog("restock_font_2_wrap", open = "restock", fontScale = 2f, stockUnits = 12_345_678)

    @Test
    fun adjustDialogEmpty() = captureStockDialog("adjust_empty", open = "adjust")

    @Test
    fun adjustDialogWithUnits() = captureStockDialog("adjust", open = "adjust", units = "3")

    @Test
    fun adjustDialogWithUnitsDark() = captureStockDialog("adjust_dark", open = "adjust", darkTheme = true, units = "3")

    @Test
    fun adjustDialogLargestFont() = captureStockDialog("adjust_font_2", open = "adjust", fontScale = 2f, units = "3")
}

@Config(qualifiers = "es-rES-w360dp-h800dp-xhdpi")
class ProductFormScreenshotSpanishTest : ProductFormScreenshotTest()
