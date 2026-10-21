package dev.jdgomez.customnotifier

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule

/** How long every instrumented UI wait may take: the CI emulator is slow, and a passing wait returns early. */
const val UI_TIMEOUT_MS = 30_000L

/** Waits until the Settings switch is enabled, which it is only once the stored setting has loaded: a tap before would be ignored. */
fun ComposeTestRule.awaitSettingsSwitchEnabled() {
    waitUntil(UI_TIMEOUT_MS) {
        onAllNodes(hasText("Add alerts to my calendar") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
    }
}
