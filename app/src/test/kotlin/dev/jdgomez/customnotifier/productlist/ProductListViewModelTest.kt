package dev.jdgomez.customnotifier.productlist

import android.content.Intent
import dev.jdgomez.customnotifier.alerts.NotificationAccess
import dev.jdgomez.customnotifier.alerts.NotificationAccessState
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
    private val access = FakeNotificationAccess()
    private var reconciles = 0

    @After
    fun resetMain() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): ProductListViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return ProductListViewModel(repository, clock, { zone }, access) { reconciles++ }
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

    @Test
    fun bannerOnlyWithProductsAndWhileAlertsAreOff() =
        runTest {
            access.state = NotificationAccessState.PermissionMissing(canAsk = true)
            val viewModel = viewModel()
            val state = collect(viewModel)
            assertEquals(ProductListState.Empty, state())

            repository.products.value = listOf(testProduct("Vitamin D", start))
            runCurrent()
            assertEquals(true, assertIs<ProductListState.Products>(state()).alertsOff)

            access.state = NotificationAccessState.Allowed
            viewModel.refreshAccess()
            runCurrent()
            assertEquals(false, assertIs<ProductListState.Products>(state()).alertsOff)
        }

    @Test
    fun reconcilesOnlyWhenAccessTurnsAllowed() =
        runTest {
            access.state = NotificationAccessState.ChannelOff
            val viewModel = viewModel()
            viewModel.refreshAccess()
            assertEquals(0, reconciles)
            access.state = NotificationAccessState.Allowed
            viewModel.refreshAccess()
            assertEquals(1, reconciles)
            viewModel.refreshAccess()
            assertEquals(1, reconciles)
        }

    @Test
    fun deniedResultIsRecordedAndGrantedIsNot() =
        runTest {
            access.state = NotificationAccessState.PermissionMissing(canAsk = true)
            val viewModel = viewModel()
            viewModel.onPermissionResult(granted = false, explanationBefore = true, explanationAfter = false)
            assertEquals(listOf(true to false), access.denials)
            viewModel.onPermissionResult(granted = true, explanationBefore = true, explanationAfter = false)
            assertEquals(1, access.denials.size)
        }

    @Test
    fun silentDenialIsReportedSoTheScreenOpensTheSettings() =
        runTest {
            access.state = NotificationAccessState.PermissionMissing(canAsk = true)
            val viewModel = viewModel()
            assertEquals(false, viewModel.onPermissionResult(granted = false, explanationBefore = true, explanationAfter = true))
            access.silent = true
            assertEquals(true, viewModel.onPermissionResult(granted = false, explanationBefore = false, explanationAfter = false))
            assertEquals(false, viewModel.onPermissionResult(granted = true, explanationBefore = false, explanationAfter = false))
        }

    @Test
    fun comingBackFromTheSystemSettingsForgetsTheDenial() =
        runTest {
            val viewModel = viewModel()
            viewModel.refreshAccess()
            assertEquals(0, access.cleared)
            viewModel.refreshAccess(backFromSystemSettings = true)
            assertEquals(1, access.cleared)
        }
}

private class FakeNotificationAccess : NotificationAccess {
    var state: NotificationAccessState = NotificationAccessState.Allowed
    val denials = mutableListOf<Pair<Boolean, Boolean>>()
    var silent = false
    var cleared = 0

    override fun current() = state

    override fun recordDenied(
        explanationBefore: Boolean,
        explanationAfter: Boolean,
    ): Boolean {
        denials += explanationBefore to explanationAfter
        return silent
    }

    override fun clearPermanentDenial() {
        cleared++
    }

    override fun settingsIntent(state: NotificationAccessState) = Intent()
}
