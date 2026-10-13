package dev.jdgomez.customnotifier.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface AlertDeliveryDao {
    @Query("SELECT COUNT(*) > 0 FROM alert_deliveries WHERE product_id = :id")
    suspend fun isDelivered(id: String): Boolean

    @Upsert
    suspend fun record(delivery: AlertDeliveryEntity)

    @Query("DELETE FROM alert_deliveries WHERE product_id = :id")
    suspend fun clear(id: String)
}
