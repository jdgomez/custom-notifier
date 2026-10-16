package dev.jdgomez.customnotifier.calendar

import android.provider.CalendarContract.Events
import androidx.test.core.app.ApplicationProvider
import dev.jdgomez.customnotifier.CustomNotifierApplication
import dev.jdgomez.customnotifier.domain.AlertEvent
import dev.jdgomez.customnotifier.domain.DepletionDate
import dev.jdgomez.customnotifier.domain.LanguageTag
import dev.jdgomez.customnotifier.domain.ProductName
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class EventValuesTest {
    private val app: CustomNotifierApplication = ApplicationProvider.getApplicationContext()

    private fun event(language: String) =
        AlertEvent(
            ProductName("Coffee"),
            DepletionDate(LocalDate.parse("2026-03-11")),
            LocalDate.parse("2026-03-01"),
            LanguageTag(language),
        )

    @Test
    fun `the texts are in the language of the event whatever the device language`() {
        val spanish = app.eventValues(event("es"))
        assertEquals("Comprar Coffee", spanish.getAsString(Events.TITLE))
        assertEquals("Se agota el 11 mar 2026. Añadido por Custom Notifier.", spanish.getAsString(Events.DESCRIPTION))

        val english = app.eventValues(event("en"))
        assertEquals("Buy Coffee", english.getAsString(Events.TITLE))
        assertEquals("Runs out Mar 11, 2026. Added by Custom Notifier.", english.getAsString(Events.DESCRIPTION))
    }
}
