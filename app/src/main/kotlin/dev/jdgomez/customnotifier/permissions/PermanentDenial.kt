package dev.jdgomez.customnotifier.permissions

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Android gives no "permanently denied" signal, so it is derived from state: whether the user was asked before ([ASKED]) and the
 * rationale flags around each request. Two stored facts, written only on change:
 * - a denial after which the rationale turned from true to false is a denial for good;
 * - a request denied with no rationale before or after, when the user was asked before, got no dialog from Android (the
 *   permission is blocked): that is a denial for good too, and [record] reports it so the caller can send the user to the settings.
 * A first-ever dismissed dialog matches neither, so it is asked again.
 */
class PermanentDenial(
    private val prefs: SharedPreferences,
) {
    val isRecorded: Boolean get() = prefs.getBoolean(PERMANENT, false)

    /**
     * Records a denied request: [explanationBefore] and [explanationAfter] are the rationale flags around the request.
     * Returns true when Android showed no dialog for it (the user must go to the system settings).
     */
    fun record(
        explanationBefore: Boolean,
        explanationAfter: Boolean,
    ): Boolean {
        val askedBefore = prefs.getBoolean(ASKED, false)
        val explained = explanationBefore || explanationAfter
        if (explained && !askedBefore) prefs.edit { putBoolean(ASKED, true) }
        val forGood = !explanationAfter && (explanationBefore || askedBefore)
        if (forGood && !isRecorded) prefs.edit { putBoolean(PERMANENT, true) }
        return !explanationBefore && !explanationAfter && askedBefore
    }

    /** Forgets the denial for good: Android shows a rationale again, or the user came back from the system settings. */
    fun clear() {
        if (isRecorded) prefs.edit { remove(PERMANENT) }
    }

    /** Forgets everything: the permission was granted, so the next request starts from scratch. */
    fun reset() {
        if (isRecorded || prefs.getBoolean(ASKED, false)) prefs.edit { remove(PERMANENT).remove(ASKED) }
    }

    private companion object {
        const val PERMANENT = "permanently_denied"
        const val ASKED = "asked_before"
    }
}
