package org.microg.gms.constellation.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.microg.gms.constellation.core.proto.VerifiedPhoneNumber

class CachedGpnvFastPathTest {
    @Test
    fun cachedFastPathRequiresRcsCachedStateAndAtMostOneTarget() {
        assertTrue(shouldTryCachedGpnvFastPath("RCS", 1, 1))
        assertTrue(shouldTryCachedGpnvFastPath("RCS", 1, 0))
        assertTrue(!shouldTryCachedGpnvFastPath("RCS", 0, 1))
        assertTrue(!shouldTryCachedGpnvFastPath(null, 1, 1))
        assertTrue(!shouldTryCachedGpnvFastPath("OTHER", 1, 1))
        assertTrue(!shouldTryCachedGpnvFastPath("RCS", 1, 2))
    }

    @Test
    fun exactPhoneMatchWinsWhenTargetIsKnown() {
        val first = VerifiedPhoneNumber(phone_number = "+441", id_token = "jwt-first")
        val exact = VerifiedPhoneNumber(phone_number = "+442", id_token = "jwt-exact")
        assertEquals(exact, findMatchingVerifiedNumber(listOf(first, exact), "+442"))
    }

    @Test
    fun wrongPhoneIsRejectedWhenTargetIsKnown() {
        val first = VerifiedPhoneNumber(phone_number = "+441", id_token = "jwt-first")
        val second = VerifiedPhoneNumber(phone_number = "+442", id_token = "jwt-second")
        assertNull(findMatchingVerifiedNumber(listOf(first, second), "+449"))
    }

    @Test
    fun firstRecordIsRetainedWhenNoTargetIsKnown() {
        val first = VerifiedPhoneNumber(phone_number = "+441", id_token = "jwt-first")
        val second = VerifiedPhoneNumber(phone_number = "+442", id_token = "jwt-second")
        assertEquals(first, findMatchingVerifiedNumber(listOf(first, second), null))
    }

    @Test
    fun emptyResponseHasNoMatch() {
        assertNull(findMatchingVerifiedNumber(emptyList(), "+441"))
    }

    @Test
    fun cachedResultPreservesResolvedSimSlotAndJwt() {
        val result = VerifiedPhoneNumber(phone_number = "+441", id_token = "jwt")
            .toPhoneNumberVerification(simSlot = 2)
        assertEquals(2, result.simSlot)
        assertEquals("jwt", result.verificationToken)
    }
}
