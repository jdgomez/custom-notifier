package dev.jdgomez.customnotifier.productlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class ProductListAddButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var added = 0

    private fun addButton() = composeRule.onNodeWithText("Add product", useUnmergedTree = true)

    private fun show(names: List<String>) =
        composeRule.setContent {
            CustomNotifierTheme {
                ProductListScreen(names.map { testProduct(it, NOW) }.toListState(NOW, ZoneOffset.UTC), onAddProduct = { added++ })
            }
        }

    @Test
    fun buttonIsShownOnTheEmptyList() {
        show(emptyList())
        composeRule.onNodeWithText("No products yet").assertIsDisplayed()
        addButton().assertIsDisplayed()
    }

    @Test
    fun buttonOpensTheForm() {
        show(listOf("Detergent"))
        addButton().performClick()
        assertEquals(1, added)
    }

    @Test
    fun lastRowClearsTheButtonWhenScrolledToTheEnd() {
        val names = (1..12).map { "Product %02d".format(it) }
        show(names)
        composeRule.onNode(hasScrollAction()).performScrollToIndex(names.lastIndex)
        // Every row ends with an "Alert due" chip: the last one is the last line of the list.
        val lastLineBottom =
            composeRule
                .onAllNodesWithText("Alert due")
                .onLast()
                .getUnclippedBoundsInRoot()
                .bottom
        val buttonTop = addButton().getUnclippedBoundsInRoot().top
        assertTrue(lastLineBottom <= buttonTop, "last row ends at $lastLineBottom, below the button top $buttonTop")
    }
}
