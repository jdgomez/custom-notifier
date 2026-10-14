package dev.jdgomez.customnotifier.permissions

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Android gives no "permanently denied" signal: a denial after which the rationale flag turned from true to false is stored here,
 * so the app can send the user to the system settings instead of showing a dialog that never appears. Writes only on change.
 */
class PermanentDenial(
    private val prefs: SharedPreferences,
) {
    val isRecorded: Boolean get() = prefs.getBoolean(KEY, false)

    /** Records a denied request: [explanationBefore] and [explanationAfter] are the rationale flags around the request. */
    fun record(
        explanationBefore: Boolean,
        explanationAfter: Boolean,
    ) {
        if (explanationBefore && !explanationAfter && !isRecorded) prefs.edit { putBoolean(KEY, true) }
    }

    /** Forgets the denial: access was granted, Android shows a rationale again, or the user came back from the system settings. */
    fun clear() {
        if (isRecorded) prefs.edit { remove(KEY) }
    }

    private companion object {
        const val KEY = "permanently_denied"
    }
}
