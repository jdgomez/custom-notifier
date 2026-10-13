package dev.jdgomez.customnotifier.data

import androidx.room3.Dao
import androidx.room3.Query

@Dao
interface AlertDeliveryDao {
    @Query("SELECT COUNT(*) > 0 FROM alert_deliveries WHERE product_id = :id")
    suspend fun isDelivered(id: String): Boolean

    @Query(
        "INSERT OR IGNORE INTO alert_deliveries (product_id) " +
            "SELECT :id WHERE EXISTS (SELECT 1 FROM products WHERE id = :id)",
    )
    suspend fun record(id: String)

    @Query("DELETE FROM alert_deliveries WHERE product_id = :id")
    suspend fun clear(id: String)
}
