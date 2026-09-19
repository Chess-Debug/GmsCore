/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DroidGuardVmCacheTest {

    @Test
    fun cacheFolderMatchesStockGms() {
        assertEquals("dg_cache", DG_CACHE_FOLDER_NAME)
        assertNotEquals("cache_dg", DG_CACHE_FOLDER_NAME)
    }

    @Test
    fun formatVmCacheKey_uppercasesLowercaseHex() {
        assertEquals(
            "A1B2C3D4E5F6789012345678ABCDEF012345",
            formatVmCacheKey("a1b2c3d4e5f6789012345678abcdef012345")
        )
    }

    @Test
    fun formatVmCacheKey_preservesUppercaseHex() {
        val hex = "DEADBEEFCAFEBABE"
        assertEquals(hex, formatVmCacheKey(hex))
    }

    @Test
    fun reusableVmCacheKey_normalizesPersistedLowercaseKeyBeforeValidation() {
        var validatedKey: String? = null
        val reusableKey = reusableVmCacheKey("deadbeef") { key ->
            validatedKey = key
            key == "DEADBEEF"
        }

        assertEquals("DEADBEEF", validatedKey)
        assertEquals("DEADBEEF", reusableKey)
    }

    @Test
    fun reusableVmCacheKey_preservesValidExistingCacheEntry() {
        assertEquals(
            "DEADBEEF",
            reusableVmCacheKey("DEADBEEF") { it == "DEADBEEF" }
        )
    }

    @Test
    fun reusableVmCacheKey_rejectsMissingCacheEntry() {
        assertNull(reusableVmCacheKey("deadbeef") { false })
    }
}
