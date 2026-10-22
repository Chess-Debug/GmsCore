package org.microg.gms.constellation.core.verification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.microg.gms.constellation.core.proto.ServiceEntitlementRequest

/** Checks the bounded state transition from the public AOSP TS.43 client. */
class Ts43RetryPolicyTest {
    @Test
    fun expiredCachedTokenRequestsOneFullAuthentication() {
        assertTrue(Ts43RetryPolicy.shouldRefreshToken(511, hadAuthToken = true, hadTemporaryToken = false))
    }

    @Test
    fun expiredTemporaryTokenAlsoRequestsFullAuthentication() {
        assertTrue(Ts43RetryPolicy.shouldRefreshToken(511, hadAuthToken = false, hadTemporaryToken = true))
    }

    @Test
    fun expiredTokenWithoutCachedCredentialDoesNotRetry() {
        assertFalse(Ts43RetryPolicy.shouldRefreshToken(511, hadAuthToken = false, hadTemporaryToken = false))
    }

    @Test
    fun otherHttpFailuresDoNotTriggerFullAuthentication() {
        for (status in intArrayOf(200, 400, 401, 403, 429, 500, 503)) {
            assertFalse(Ts43RetryPolicy.shouldRefreshToken(status, hadAuthToken = true, hadTemporaryToken = true))
        }
    }

    @Test
    fun expiredTokensRemovedWithoutDestroyingOtherEntitlementFields() {
        val original = ServiceEntitlementRequest(
            authentication_token = "expired",
            temporary_token = "temporary",
            terminal_id = "preserved-terminal-id"
        )
        val refreshed = Ts43RetryPolicy.withoutExpiredTokens(original)

        assertEquals("", refreshed.authentication_token)
        assertEquals("", refreshed.temporary_token)
        assertEquals("preserved-terminal-id", refreshed.terminal_id)
        assertEquals("expired", original.authentication_token)
        assertEquals("temporary", original.temporary_token)
    }
}
