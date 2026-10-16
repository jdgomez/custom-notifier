package dev.jdgomez.customnotifier.data

import dev.jdgomez.customnotifier.domain.AlertEvent
import dev.jdgomez.customnotifier.domain.CalendarEventRecord
import dev.jdgomez.customnotifier.domain.CalendarEventRecords
import dev.jdgomez.customnotifier.domain.CalendarId
import dev.jdgomez.customnotifier.domain.DepletionDate
import dev.jdgomez.customnotifier.domain.EventId
import dev.jdgomez.customnotifier.domain.LanguageTag
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.domain.ProductName
import java.time.LocalDate
import java.util.UUID

/** [CalendarEventRecords] over Room. */
class RoomCalendarEventRecords(
    private val dao: CalendarEventDao,
) : CalendarEventRecords {
    override suspend fun all(): List<CalendarEventRecord> =
        dao.all().map {
            CalendarEventRecord(
                ProductId(UUID.fromString(it.productId)),
                CalendarId(it.calendarId),
                it.eventId?.let(::EventId),
                AlertEvent(
                    ProductName(it.titleName),
                    DepletionDate(LocalDate.parse(it.depletionDate)),
                    LocalDate.parse(it.eventDate),
                    LanguageTag(it.language),
                ),
            )
        }

    override suspend fun put(record: CalendarEventRecord) =
        dao.put(
            CalendarEventEntity(
                record.productId.value.toString(),
                record.calendarId.value,
                record.eventId?.value,
                record.content.productName.value,
                record.content.depletionDate.date
                    .toString(),
                record.content.date.toString(),
                record.content.language.value,
            ),
        )

    override suspend fun remove(productId: ProductId) = dao.remove(productId.value.toString())
}
