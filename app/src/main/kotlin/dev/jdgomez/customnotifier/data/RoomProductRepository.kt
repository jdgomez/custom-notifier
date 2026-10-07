package dev.jdgomez.customnotifier.data

import dev.jdgomez.customnotifier.domain.ConsumptionRate
import dev.jdgomez.customnotifier.domain.LeadTime
import dev.jdgomez.customnotifier.domain.PackageSize
import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.domain.ProductName
import dev.jdgomez.customnotifier.domain.ProductRepository
import dev.jdgomez.customnotifier.domain.Quantity
import dev.jdgomez.customnotifier.domain.Rule
import dev.jdgomez.customnotifier.domain.Stock
import dev.jdgomez.customnotifier.domain.UnitLabel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

/** [ProductRepository] over Room. Domain validation runs on read, so a corrupted row fails loudly. */
class RoomProductRepository(
    private val dao: ProductDao,
) : ProductRepository {
    override fun observeAll(): Flow<List<Product>> = dao.observeAll().map { rows -> rows.map { it.toDomain() }.sortedWith(byNameThenId) }

    override suspend fun find(id: ProductId): Product? = dao.find(id.value.toString())?.toDomain()

    override suspend fun save(product: Product) = dao.save(product.toEntity())

    override suspend fun delete(id: ProductId) = dao.delete(id.value.toString())
}

private val byNameThenId =
    compareBy<Product, String>(String.CASE_INSENSITIVE_ORDER) { it.name.value }
        .thenBy { it.id.value.toString() }

private fun Product.toEntity() =
    ProductEntity(
        id = id.value.toString(),
        name = name.value,
        unitLabel = unitLabel.value,
        packageSize = packageSize.units,
        rateUnits = consumptionRate.units,
        rateDays = consumptionRate.days,
        stockNumerator = stock.units.numerator.toString(),
        stockDenominator = stock.units.denominator.toString(),
        recordedAtEpochSecond = stock.recordedAt.epochSecond,
        recordedAtNano = stock.recordedAt.nano,
        leadTimeDays = rule.leadTime.days,
    )

private fun ProductEntity.toDomain() =
    Product(
        id = ProductId(UUID.fromString(id)),
        name = ProductName(name),
        unitLabel = UnitLabel(unitLabel),
        packageSize = PackageSize(packageSize),
        consumptionRate = ConsumptionRate(rateUnits, rateDays),
        stock =
            Stock(
                Quantity.of(BigInteger(stockNumerator), BigInteger(stockDenominator)),
                Instant.ofEpochSecond(recordedAtEpochSecond, recordedAtNano.toLong()),
            ),
        rule = Rule(LeadTime(leadTimeDays)),
    )
