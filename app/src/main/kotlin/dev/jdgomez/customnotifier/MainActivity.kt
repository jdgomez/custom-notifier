package dev.jdgomez.customnotifier

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.jdgomez.customnotifier.productlist.ProductListRoute
import dev.jdgomez.customnotifier.ui.CustomNotifierTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transparent scrims: the library default paints a translucent white
        // navigation bar below API 29, which clashes with the app surface.
        // The auto style picks the icon color from the system dark theme, like the app theme does.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            CustomNotifierTheme {
                ProductListRoute()
            }
        }
    }
}
