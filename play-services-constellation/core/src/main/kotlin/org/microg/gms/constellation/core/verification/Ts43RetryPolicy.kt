package org.microg.gms.constellation.core.verification

import org.microg.gms.constellation.core.proto.ServiceEntitlementRequest

/**
 * AOSP ImsEntitlementApi treats HTTP 511 as expired authorization and
 * retries without the cached token. The caller performs one fresh EAP-AKA
 * exchange; an error from that second request is not retried again.
 */
internal object Ts43RetryPolicy {
    private const val HTTP_TOKEN_EXPIRED = 511

    fun shouldRefreshToken(httpStatus: Int, hadAuthToken: Boolean, hadTemporaryToken: Boolean): Boolean =
        httpStatus == HTTP_TOKEN_EXPIRED && (hadAuthToken || hadTemporaryToken)

    fun withoutExpiredTokens(request: ServiceEntitlementRequest): ServiceEntitlementRequest =
        request.copy(authentication_token = "", temporary_token = "")
}
