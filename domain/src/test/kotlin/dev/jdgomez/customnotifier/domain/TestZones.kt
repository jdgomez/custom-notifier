package dev.jdgomez.customnotifier.domain

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.zone.ZoneOffsetTransition
import java.time.zone.ZoneRules
import java.time.zone.ZoneRulesProvider
import java.util.TreeMap

/**
 * Test-only time zones whose clocks change at 09:00 on 2026-04-10, which no real zone does.
 * Registered once per JVM on first use.
 */
object TestZones {
    private const val GAP_ID = "Test/Gap0900"
    private const val OVERLAP_ID = "Test/Overlap0900"

    private val utc = ZoneOffset.UTC
    private val plusOne = ZoneOffset.ofHours(1)

    private val rules =
        mapOf(
            // 09:00 -> 10:00: local 09:00 does not exist.
            GAP_ID to
                ZoneRules.of(
                    utc,
                    utc,
                    emptyList(),
                    listOf(ZoneOffsetTransition.of(LocalDateTime.of(2026, 4, 10, 9, 0), utc, plusOne)),
                    emptyList(),
                ),
            // 10:00 -> 09:00: local 09:00 occurs twice.
            OVERLAP_ID to
                ZoneRules.of(
                    plusOne,
                    plusOne,
                    emptyList(),
                    listOf(ZoneOffsetTransition.of(LocalDateTime.of(2026, 4, 10, 10, 0), plusOne, utc)),
                    emptyList(),
                ),
        )

    init {
        ZoneRulesProvider.registerProvider(
            object : ZoneRulesProvider() {
                override fun provideZoneIds() = rules.keys

                override fun provideRules(
                    zoneId: String,
                    forCaching: Boolean,
                ) = rules.getValue(zoneId)

                override fun provideVersions(zoneId: String) = TreeMap(mapOf("test" to rules.getValue(zoneId)))
            },
        )
    }

    val gap: ZoneId by lazy { ZoneId.of(GAP_ID) }
    val overlap: ZoneId by lazy { ZoneId.of(OVERLAP_ID) }
}
