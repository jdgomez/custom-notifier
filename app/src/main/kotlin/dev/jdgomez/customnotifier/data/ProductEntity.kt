package dev.jdgomez.customnotifier.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** The stored form of a product; every domain value maps to lossless columns. */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "unit_label") val unitLabel: String,
    @ColumnInfo(name = "package_size") val packageSize: Int,
    @ColumnInfo(name = "rate_units") val rateUnits: Int,
    @ColumnInfo(name = "rate_days") val rateDays: Int,
    @ColumnInfo(name = "stock_numerator") val stockNumerator: String,
    @ColumnInfo(name = "stock_denominator") val stockDenominator: String,
    @ColumnInfo(name = "recorded_at_epoch_second") val recordedAtEpochSecond: Long,
    @ColumnInfo(name = "recorded_at_nano") val recordedAtNano: Int,
    @ColumnInfo(name = "lead_time_days") val leadTimeDays: Int,
)
