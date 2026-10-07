package dev.jdgomez.customnotifier.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProductValuesTest {
    private fun assertRejects(
        field: String,
        block: () -> Unit,
    ) {
        val error = assertFailsWith<IllegalArgumentException> { block() }
        assertTrue(error.message!!.contains(field), "message '${error.message}' should name $field")
    }

    @Test
    fun `name and unit label are trimmed`() {
        assertEquals("Vitamin D", ProductName(" Vitamin D ").value)
        assertEquals("pill", UnitLabel("\tpill\n").value)
    }

    @Test
    fun `blank name or unit label is rejected naming the field`() {
        assertRejects("name") { ProductName("") }
        assertRejects("name") { ProductName("   ") }
        assertRejects("unitLabel") { UnitLabel("") }
        assertRejects("unitLabel") { UnitLabel("  ") }
    }

    @Test
    fun `package size must be positive`() {
        assertEquals(90, PackageSize(90).units)
        assertRejects("packageSize") { PackageSize(0) }
        assertRejects("packageSize") { PackageSize(-1) }
    }

    @Test
    fun `consumption rate needs positive units and days`() {
        assertEquals(3, ConsumptionRate(3, 2).units)
        assertRejects("consumptionRate.units") { ConsumptionRate(0, 1) }
        assertRejects("consumptionRate.units") { ConsumptionRate(-1, 1) }
        assertRejects("consumptionRate.days") { ConsumptionRate(1, 0) }
        assertRejects("consumptionRate.days") { ConsumptionRate(1, -2) }
    }

    @Test
    fun `lead time accepts 0 to 365 days and rejects anything else naming the lead time`() {
        assertEquals(0, Rule(LeadTime(0)).leadTime.days)
        assertEquals(365, Rule(LeadTime(365)).leadTime.days)
        assertRejects("leadTime") { LeadTime(-1) }
        assertRejects("leadTime") { LeadTime(366) }
    }
}
