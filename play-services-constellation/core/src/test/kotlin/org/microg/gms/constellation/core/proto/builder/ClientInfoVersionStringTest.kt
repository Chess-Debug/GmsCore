package org.microg.gms.constellation.core.proto.builder

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.microg.gms.common.Constants
import org.microg.gms.constellation.core.proto.ClientInfo

class ClientInfoVersionStringTest {
    @Test
    fun nonSyncRpcKeepsPackageVersionString() {
        assertEquals("microg-test", reportedGmsCoreVersionString("getConsent", "microg-test"))
    }

    @Test
    fun syncRpcUsesGmsVersionString() {
        assertEquals(
            "26.34.33 (260400-975859951)",
            reportedGmsCoreVersionString("sync", "microg-test")
        )
    }

    @Test
    fun syncField5DoesNotChangeField4NumericVersion() {
        val info = ClientInfo(
            gmscore_version_number = Constants.GMS_VERSION_CODE / 1000,
            gmscore_version = reportedGmsCoreVersionString("sync", "microg-test")
        )
        assertEquals(Constants.GMS_VERSION_CODE / 1000, info.gmscore_version_number)
    }

    @Test
    fun gmsVersionStringHasExactField5WireBytes() {
        val expected = byteArrayOf(
            0x2a, 0x1b,
            0x32, 0x36, 0x2e, 0x33, 0x34, 0x2e, 0x33, 0x33, 0x20, 0x28,
            0x32, 0x36, 0x30, 0x34, 0x30, 0x30, 0x2d, 0x39, 0x37, 0x35,
            0x38, 0x35, 0x39, 0x39, 0x35, 0x31, 0x29
        )
        assertArrayEquals(
            expected,
            ClientInfo.ADAPTER.encode(
                ClientInfo(gmscore_version = "26.34.33 (260400-975859951)")
            )
        )
    }
}
