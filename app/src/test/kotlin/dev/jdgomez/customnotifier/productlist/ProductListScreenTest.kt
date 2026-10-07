package dev.jdgomez.customnotifier.productlist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.ui.CustomNotifierTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val NOW = Instant.parse("2026-03-05T10:00:00Z")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w360dp-h800dp-xhdpi")
class ProductListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var edited: ProductId? = null

    private fun show(
        products: List<Product>,
        now: Instant = NOW,
        zone: ZoneOffset = ZoneOffset.UTC,
    ) = composeRule.setContent {
        CustomNotifierTheme { ProductListScreen(products.toListState(now, zone), onAddProduct = {}, onEditProduct = { edited = it }) }
    }

    @Test
    fun tappingARowOpensThatProductWithTheEditAction() {
        val vitamin = testProduct("Vitamin D", NOW)
        show(listOf(testProduct("Detergent", NOW), vitamin))
        composeRule.onNodeWithText("Vitamin D").assertHasClickAction()
        composeRule.onNodeWithText("Vitamin D").performClick()
        assertEquals(vitamin.id, edited)
        composeRule.onNodeWithText("Vitamin D").assert(
            SemanticsMatcher("onClick label Edit") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Edit" },
        )
    }

    private fun stockOf(
        text: String,
        description: String? = null,
    ) = composeRule
        .onNodeWithText(text, useUnmergedTree = true)
        .also { node ->
            node.assertIsDisplayed()
            description?.let {
                node.assert(
                    SemanticsMatcher("contentDescription $it") { n ->
                        n.config.getOrNull(SemanticsProperties.ContentDescription) ==
                            listOf(it)
                    },
                )
            }
        }

    @Test
    fun showsTheAppNameAndTheEmptyState() {
        show(emptyList())

        composeRule.onNodeWithText("Custom Notifier").assertIsDisplayed()
        composeRule.onNodeWithText("No products yet").assertIsDisplayed()
        composeRule.onNodeWithText("Products you add will appear here with their stock and next alert.").assertIsDisplayed()
    }

    @Test
    fun wholeEstimateHasNoPrefix() {
        show(listOf(testProduct("Vitamin", NOW, units = 8)))

        stockOf("8 pills")
    }

    @Test
    fun roundedEstimateHasMutedPrefixAndAboutDescription() {
        show(listOf(testProduct("Vitamin", NOW, units = 17, halves = true)))

        stockOf("≈ 8 pills", description = "about 8 pills")
        val text =
            composeRule
                .onNodeWithText("≈ 8 pills", useUnmergedTree = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .single()
        assertTrue(text.spanStyles.any { it.start == 0 && it.end == 2 && it.item.color != Color.Unspecified })
    }

    @Test
    fun zeroEstimateIsBoldWithoutErrorStyling() {
        show(listOf(testProduct("Vitamin", NOW, units = 0)))

        val text: AnnotatedString =
            composeRule
                .onNodeWithText("0 pills", useUnmergedTree = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .single()
        assertEquals(
            FontWeight.Bold,
            text.spanStyles
                .single()
                .item.fontWeight,
        )
    }

    @Test
    fun largeStockUsesLocaleGrouping() {
        show(listOf(testProduct("Soap", NOW, units = 12_000, unitLabel = "ml")))

        stockOf("12,000 ml")
    }

    @Test
    fun alertScheduled() {
        show(listOf(testProduct("A", Instant.parse("2026-02-20T12:00:00Z"), units = 19)), now = Instant.parse("2026-02-20T09:00:00Z"))

        composeRule.onNodeWithText("Runs out Mar 11, 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Alert Mar 1, 2026").assertIsDisplayed()
    }

    @Test
    fun alertDue() {
        show(listOf(testProduct("A", Instant.parse("2026-03-01T12:00:00Z"), units = 10)))

        composeRule.onNodeWithText("Runs out Mar 11, 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Alert due").assertIsDisplayed()
    }

    @Test
    fun alreadyRanOut() {
        show(listOf(testProduct("A", Instant.parse("2026-03-01T12:00:00Z"), units = 10)), now = Instant.parse("2026-03-12T10:00:00Z"))

        composeRule.onNodeWithText("Ran out Mar 11, 2026").assertIsDisplayed()
        composeRule.onAllNodes(hasText("Runs out", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("Alert", substring = true)).assertCountEquals(0)
    }

    @Test
    fun depletionDateUsesTheDeviceZone() {
        show(listOf(testProduct("A", Instant.parse("2026-03-01T23:30:00Z"), units = 10)), zone = ZoneOffset.ofHours(2))

        composeRule.onNodeWithText("Runs out Mar 12, 2026").assertIsDisplayed()
    }

    @Test
    fun rowIsOneButtonForScreenReadersAnnouncingItsContentAndTheEditAction() {
        show(listOf(testProduct("A", NOW, units = 8), testProduct("B", NOW, units = 5)))

        val rows = composeRule.onAllNodes(hasClickAction() and hasText("pills", substring = true))
        rows.assertCountEquals(2)
        // One merged node per row: the name, the stock and the dates, a button whose click label is "Edit".
        composeRule.onNode(hasText("A") and hasText("8 pills") and hasText("Runs out", substring = true)).assert(
            SemanticsMatcher("button with click label Edit") {
                it.config.getOrNull(SemanticsProperties.Role) == Role.Button &&
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == "Edit"
            },
        )
        // The children are not separate focus stops: the merged tree has no node with only the name.
        composeRule.onAllNodes(hasText("A") and !hasText("8 pills")).assertCountEquals(0)
    }
}
