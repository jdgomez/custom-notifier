package dev.jdgomez.customnotifier

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import dev.jdgomez.customnotifier.data.CustomNotifierDatabase
import dev.jdgomez.customnotifier.data.RoomProductRepository
import dev.jdgomez.customnotifier.domain.ProductRepository
import java.time.Clock
import java.time.ZoneId

/**
 * Manual dependency injection: the application-wide dependencies, built lazily (ADR 0015).
 *
 * [clock] gives the current instant and [zone] the device's current time zone, read on every use so a
 * zone change is picked up; both can be replaced in tests.
 */
class AppContainer(
    context: Context,
    val clock: Clock = Clock.systemUTC(),
    val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val database: CustomNotifierDatabase by lazy {
        Room
            .databaseBuilder<CustomNotifierDatabase>(context, DATABASE_NAME)
            .setDriver(AndroidSQLiteDriver())
            .build()
    }

    val productRepository: ProductRepository by lazy { RoomProductRepository(database.productDao()) }

    private companion object {
        const val DATABASE_NAME = "custom-notifier.db"
    }
}
