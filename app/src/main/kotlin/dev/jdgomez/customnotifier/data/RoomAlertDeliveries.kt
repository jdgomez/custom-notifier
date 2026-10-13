package dev.jdgomez.customnotifier.data

import dev.jdgomez.customnotifier.domain.AlertDeliveries
import dev.jdgomez.customnotifier.domain.ProductId

/** [AlertDeliveries] over Room. */
class RoomAlertDeliveries(
    private val dao: AlertDeliveryDao,
) : AlertDeliveries {
    override suspend fun isDelivered(id: ProductId) = dao.isDelivered(id.value.toString())

    override suspend fun record(id: ProductId) = dao.record(AlertDeliveryEntity(id.value.toString()))

    override suspend fun clear(id: ProductId) = dao.clear(id.value.toString())
}
