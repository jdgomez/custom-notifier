package dev.jdgomez.customnotifier

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import dev.jdgomez.customnotifier.alerts.NotificationAccess
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The app with its own language set to Spanish (Android 13 and later): creating a product shows Spanish texts. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class SpanishTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val locales get() = context.getSystemService(LocaleManager::class.java)
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun chooseSpanish() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, NotificationAccess.PERMISSION)
        locales.applicationLocales = LocaleList.forLanguageTags("es")
        composeRule.waitUntil(UI_TIMEOUT_MS) {
            context.resources.configuration.locales[0]
                .language == "es"
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun restoreLanguage() {
        scenario?.close()
        locales.applicationLocales = LocaleList.getEmptyLocaleList()
    }

    private fun awaitText(text: String) =
        composeRule.waitUntil(UI_TIMEOUT_MS) { composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    private fun type(
        tag: String,
        text: String,
    ) = composeRule.onNodeWithTag(tag).performScrollTo().performTextReplacement(text)

    @Test
    fun createdProductIsShownInSpanish() {
        awaitText("Aún no hay productos")
        composeRule.onNodeWithText("Añadir producto", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Nuevo producto").assertIsDisplayed()
        type("Name", "Café")
        type("Unit", "pastillas")
        type("PackageSize", "90")
        type("ConsumptionUnits", "1")
        type("UnitsNow", "30")
        composeRule.onNodeWithText("Guardar").performClick()

        composeRule.waitUntil(UI_TIMEOUT_MS) {
            composeRule.onAllNodes(hasText("Se agota el", substring = true) and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Café").assertIsDisplayed()
    }
}
