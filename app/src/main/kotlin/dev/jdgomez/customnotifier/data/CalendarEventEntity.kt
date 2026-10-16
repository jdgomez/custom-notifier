package dev.jdgomez.customnotifier.data

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert

/**
 * The alert event the app created for a product. No foreign key to products: the record must outlive a deleted product
 * until its event is deleted. A null [eventId] means the event was found gone.
 */
@Entity(tableName = "calendar_event")
data class CalendarEventEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "calendar_id") val calendarId: Long,
    @ColumnInfo(name = "event_id") val eventId: Long?,
    @ColumnInfo(name = "title_name") val titleName: String,
    @ColumnInfo(name = "depletion_date") val depletionDate: String,
    @ColumnInfo(name = "event_date") val eventDate: String,
    @ColumnInfo(name = "language", defaultValue = "") val language: String,
)

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_event")
    suspend fun all(): List<CalendarEventEntity>

    @Upsert
    suspend fun put(entity: CalendarEventEntity)

    @Query("DELETE FROM calendar_event WHERE product_id = :productId")
    suspend fun remove(productId: String)
}
