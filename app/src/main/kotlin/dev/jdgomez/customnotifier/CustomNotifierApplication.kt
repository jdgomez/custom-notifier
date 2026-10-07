package dev.jdgomez.customnotifier

import android.app.Application

/** Owns the [AppContainer] for the life of the process. */
class CustomNotifierApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
