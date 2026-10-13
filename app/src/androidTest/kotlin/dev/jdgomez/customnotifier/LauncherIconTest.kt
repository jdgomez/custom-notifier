package dev.jdgomez.customnotifier

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class LauncherIconTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun applicationUsesItsOwnIconNotTheSystemDefault() {
        assertEquals(R.mipmap.ic_launcher, context.applicationInfo.icon)
        assertNotEquals(android.R.drawable.sym_def_app_icon, context.applicationInfo.icon)
    }

    @Test
    fun applicationIconIsAdaptiveWithMonochromeLayerOnApi33() {
        val icon = assertIs<AdaptiveIconDrawable>(context.packageManager.getApplicationIcon(context.packageName))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) assertNotNull(icon.monochrome)
    }
}
