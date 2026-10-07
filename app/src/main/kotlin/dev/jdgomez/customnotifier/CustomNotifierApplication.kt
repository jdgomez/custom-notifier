package dev.jdgomez.customnotifier

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras

/** Owns the [AppContainer] for the life of the process. */
class CustomNotifierApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** The [AppContainer] of the application that creates this ViewModel. */
fun CreationExtras.customNotifierContainer(): AppContainer = (this[APPLICATION_KEY] as CustomNotifierApplication).container
