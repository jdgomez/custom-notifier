package dev.jdgomez.customnotifier.productlist

import dev.jdgomez.customnotifier.domain.ProductName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {
    private val start = Instant.parse("2026-03-05T10:00:00Z")
    private val repository = FakeRepository()
    private val clock = MutableClock(start)
    private var zone: ZoneId = ZoneOffset.UTC

    @After
    fun resetMain() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): ProductListViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return ProductListViewModel(repository, clock) { zone }
    }

    private fun TestScope.collect(viewModel: ProductListViewModel): () -> ProductListState {
        var latest: ProductListState = ProductListState.Loading
        backgroundScope.launch { viewModel.state.collect { latest = it } }
        runCurrent()
        return { latest }
    }

    private fun rows(state: ProductListState) = assertIs<ProductListState.Products>(state).rows

    @Test
    fun emptyWhenNothingIsStored() =
        runTest {
            val state = collect(viewModel())
            assertEquals(ProductListState.Empty, state())
        }

    @Test
    fun loadingBeforeAnyCollector() =
        runTest {
            assertEquals(ProductListState.Loading, viewModel().state.value)
        }

    @Test
    fun rowsKeepTheRepositoryOrder() =
        runTest {
            repository.products.value = listOf("Vitamin D", "diapers", "Detergent").map { testProduct(it, start) }
            val state = collect(viewModel())
            assertEquals(listOf("Detergent", "diapers", "Vitamin D"), rows(state()).map { it.name })
        }

    @Test
    fun scheduledDueAndRanOutStatuses() =
        runTest {
            // Depletion Mar 11 (10 units from Mar 1) with 10 days lead: alert Mar 1 09:00 is past, so due on Mar 5.
            repository.products.value =
                listOf(
                    testProduct("due", Instant.parse("2026-03-01T12:00:00Z")),
                    testProduct("scheduled", Instant.parse("2026-03-05T10:00:00Z"), leadDays = 2),
                    testProduct("ran", Instant.parse("2026-02-01T12:00:00Z")),
                )
            val byName = rows(collect(viewModel())()).associateBy { it.name }
            assertEquals(
                RowStatus.Upcoming(LocalDate.of(2026, 3, 11), null),
                byName.getValue("due").status,
            )
            assertEquals(
                RowStatus.Upcoming(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 13)),
                byName.getValue("scheduled").status,
            )
            assertEquals(RowStatus.RanOut(LocalDate.of(2026, 2, 11)), byName.getValue("ran").status)
        }

    @Test
    fun depletionDateFollowsTheDeviceZone() =
        runTest {
            // Stock reaches zero at 23:30 UTC on Mar 11; two hours ahead of UTC that is Mar 12.
            repository.products.value = listOf(testProduct("p", Instant.parse("2026-03-01T23:30:00Z")))
            zone = ZoneOffset.ofHours(2)
            val status = rows(collect(viewModel())())[0].status
            assertEquals(LocalDate.of(2026, 3, 12), assertIs<RowStatus.Upcoming>(status).depletionDate)
        }

    @Test
    fun recomputesAfterSixtySeconds() =
        runTest {
            // 8.5 units at start, 1 per day: below 8 whole units 12 hours later.
            repository.products.value = listOf(testProduct("p", start, units = 17, halves = true))
            clock.now = start.plusSeconds(43_200 - 30)
            val state = collect(viewModel())
            assertEquals(8.toBigInteger(), rows(state())[0].stock.wholeUnits)

            clock.now = start.plusSeconds(43_200 + 30)
            advanceTimeBy(59_000)
            assertEquals(8.toBigInteger(), rows(state())[0].stock.wholeUnits)
            advanceTimeBy(2_000)
            assertEquals(7.toBigInteger(), rows(state())[0].stock.wholeUnits)
        }

    @Test
    fun storedChangesAppearWithoutRelaunch() =
        runTest {
            val state = collect(viewModel())
            val first = testProduct("a", start)

            repository.save(first)
            runCurrent()
            assertEquals(listOf("a"), rows(state()).map { it.name })

            repository.save(first.rename(ProductName("b")))
            runCurrent()
            assertEquals(listOf("b"), rows(state()).map { it.name })

            repository.delete(first.id)
            runCurrent()
            assertEquals(ProductListState.Empty, state())
        }
}
