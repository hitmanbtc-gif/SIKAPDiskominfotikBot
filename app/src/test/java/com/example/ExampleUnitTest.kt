package com.example

import com.example.data.model.ActivityClassification
import com.example.data.model.DiskominfotikCatalog
import com.example.data.model.JabatanLevel
import com.example.data.model.ReportStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `diskominfotik catalog provides all 5 organizational units and sub-kegiatan`() {
        assertEquals(5, DiskominfotikCatalog.units.size)
        DiskominfotikCatalog.units.forEach { unit ->
            assertTrue(unit.subKegiatans.isNotEmpty())
        }
    }

    @Test
    fun `enum deserialization safely falls back on unknown values`() {
        assertEquals(ActivityClassification.RUTIN, ActivityClassification.fromRaw("UNKNOWN_VALUE"))
        assertEquals(ActivityClassification.ARAHAN_PIMPINAN, ActivityClassification.fromRaw("ARAHAN_PIMPINAN"))
        assertEquals(JabatanLevel.PELAKSANA, JabatanLevel.fromRaw(null))
        assertEquals(JabatanLevel.KEPALA_DINAS, JabatanLevel.fromRaw("KEPALA_DINAS"))
        assertEquals(ReportStatus.DRAFT, ReportStatus.fromRaw("LEGACY"))
        assertEquals(ReportStatus.APPROVED_FINAL, ReportStatus.fromRaw("APPROVED_FINAL"))
    }
}
