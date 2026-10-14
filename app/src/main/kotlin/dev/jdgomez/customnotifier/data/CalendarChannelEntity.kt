package dev.jdgomez.customnotifier.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** The calendar channel setting: a single row (`id` 0); no row or a null [calendarId] means the channel is off. */
@Entity(tableName = "calendar_channel")
data class CalendarChannelEntity(
    @PrimaryKey val id: Int = SINGLE_ROW,
    @ColumnInfo(name = "calendar_id") val calendarId: Long?,
) {
    companion object {
        const val SINGLE_ROW = 0
    }
}
