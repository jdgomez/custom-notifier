package dev.jdgomez.customnotifier.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** The [Activity] this context belongs to, or null when there is none. */
internal tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
