package org.microg.gms.constellation.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.microg.gms.constellation.core.proto.AsterismClient
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.ConsentVersion
import org.microg.gms.constellation.core.proto.GaiaConsent
import org.microg.gms.constellation.core.proto.GetConsentResponse
import org.microg.gms.constellation.core.proto.RcsConsent

class RcsConsentGateTest {
    @Test
    fun regularRcsVerificationRequiresConsent() {
        assertTrue(requiresConsumerConsent(null, AsterismClient.RCS))
    }

    @Test
    fun oneTimeVerificationDoesNotRequireConsumerConsentGate() {
        assertFalse(requiresConsumerConsent("True", AsterismClient.RCS))
    }

    @Test
    fun unknownClientDoesNotUseRcsConsumerConsentGate() {
        assertFalse(requiresConsumerConsent(null, AsterismClient.UNKNOWN_CLIENT))
    }

    @Test
    fun topLevelRcsConsentAllowsVerification() {
        val response = GetConsentResponse(
            rcs_consent = RcsConsent(
                consent = Consent.CONSENTED,
                consent_version = ConsentVersion.RCS_CONSENT
            )
        )

        assertTrue(hasRequiredConsumerConsent(response, AsterismClient.RCS))
    }

    @Test
    fun matchingGaiaConsentAllowsVerification() {
        val response = GetConsentResponse(
            gaia_consents = listOf(
                GaiaConsent(
                    asterism_client = AsterismClient.RCS,
                    consent = Consent.CONSENTED,
                    consent_version = ConsentVersion.RCS_CONSENT
                )
            )
        )

        assertTrue(hasRequiredConsumerConsent(response, AsterismClient.RCS))
    }

    @Test
    fun unrelatedGaiaConsentDoesNotAllowVerification() {
        val response = GetConsentResponse(
            gaia_consents = listOf(
                GaiaConsent(
                    asterism_client = AsterismClient.CONSTELLATION,
                    consent = Consent.CONSENTED,
                    consent_version = ConsentVersion.CONSENT_VERSION_UNSPECIFIED
                )
            )
        )

        assertFalse(hasRequiredConsumerConsent(response, AsterismClient.RCS))
    }

    @Test
    fun noConsentDoesNotAllowVerification() {
        assertFalse(hasRequiredConsumerConsent(GetConsentResponse(), AsterismClient.RCS))
    }
}
