package dev.jdgomez.customnotifier.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

/** A product whose current alert was delivered; removed with the product. */
@Entity(
    tableName = "alert_deliveries",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AlertDeliveryEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: String,
)
