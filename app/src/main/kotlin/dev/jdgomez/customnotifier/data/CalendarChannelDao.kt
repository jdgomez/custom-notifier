package dev.jdgomez.customnotifier.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarChannelDao {
    @Query("SELECT * FROM calendar_channel WHERE id = 0")
    fun observe(): Flow<CalendarChannelEntity?>

    @Upsert
    suspend fun save(entity: CalendarChannelEntity)
}
