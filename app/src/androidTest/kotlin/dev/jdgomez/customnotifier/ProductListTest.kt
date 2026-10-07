package dev.jdgomez.customnotifier

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.jdgomez.customnotifier.domain.ConsumptionRate
import dev.jdgomez.customnotifier.domain.LeadTime
import dev.jdgomez.customnotifier.domain.PackageSize
import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.domain.ProductName
import dev.jdgomez.customnotifier.domain.Quantity
import dev.jdgomez.customnotifier.domain.Stock
import dev.jdgomez.customnotifier.domain.UnitLabel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import kotlin.test.assertTrue
import dev.jdgomez.customnotifier.domain.Rule as AlertRule

@RunWith(AndroidJUnit4::class)
class ProductListTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val container = ApplicationProvider.getApplicationContext<CustomNotifierApplication>().container

    /**
     * A product consuming 1 unit per day. Its stock is recorded a day ahead so that no consumption has
     * elapsed yet and the estimate stays a whole number for the whole run.
     */
    private fun seed(
        name: String,
        unitLabel: String,
        units: Long,
        leadDays: Int,
    ) = Product(
        id = ProductId(UUID.randomUUID()),
        name = ProductName(name),
        unitLabel = UnitLabel(unitLabel),
        packageSize = PackageSize(10),
        consumptionRate = ConsumptionRate(units = 1, days = 1),
        stock = Stock(Quantity.of(units), Instant.now().plusSeconds(SECONDS_PER_DAY)),
        rule = AlertRule(LeadTime(leadDays)),
    ).also { runBlocking { container.productRepository.save(it) } }

    private fun runsOutText(product: Product): String {
        val date = product.depletionDateIn(ZoneId.systemDefault()).date
        return "Runs out " + DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(date)
    }

    /** The list loads from the database off the main thread, which Compose's idling does not wait for. */
    private fun awaitText(text: String) =
        composeRule.waitUntil(LOAD_TIMEOUT_MS) { composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun emptyStateOnACleanInstall() {
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("No products yet")
            composeRule.onNodeWithText("No products yet").assertIsDisplayed()
            composeRule.onNodeWithText("Products you add will appear here with their stock and next alert.").assertIsDisplayed()
        }
    }

    @Test
    fun seededProductsAreShownInOrderWithTheirTexts() {
        seed("Vitamin D", "pills", units = 30, leadDays = 10)
        seed("diapers", "pcs", units = 20, leadDays = 10)
        val detergent = seed("Detergent", "ml", units = 5, leadDays = 10)

        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Detergent")
            val tops = listOf("Detergent", "diapers", "Vitamin D").map { composeRule.onNodeWithText(it).getUnclippedBoundsInRoot().top }
            assertTrue(tops == tops.sorted(), "rows must follow the stored order, got tops $tops")

            composeRule.onNodeWithText("5 ml").assertIsDisplayed()
            composeRule.onNodeWithText("20 pcs").assertIsDisplayed()
            composeRule.onNodeWithText("30 pills").assertIsDisplayed()
            composeRule.onNodeWithText(runsOutText(detergent)).assertIsDisplayed()
            // 5 days to depletion with a 10 day lead time: the alert is due for Detergent only.
            composeRule.onNodeWithText("Alert due").assertIsDisplayed()
        }
    }

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
        const val LOAD_TIMEOUT_MS = 5_000L
    }
}
