package dev.jdgomez.customnotifier

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assume.assumeFalse

/**
 * The path of the committed screenshot reference [name] (without extension). Spanish references end in `_es`; English ones
 * keep their names. Spanish is captured in the light theme only, so a dark capture is skipped there.
 */
fun referencePath(
    name: String,
    darkTheme: Boolean = false,
): String {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val spanish =
        context.resources.configuration.locales[0]
            .language == "es"
    assumeFalse("Spanish references are light theme only", spanish && darkTheme)
    return "src/test/screenshots/$name${if (spanish) "_es" else ""}.png"
}
