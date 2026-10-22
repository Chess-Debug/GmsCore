/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification.ts43

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.microg.gms.constellation.core.proto.ServiceEntitlementRequest

class ServiceEntitlementBuilderTest {

    @Test(expected = IllegalArgumentException::class)
    fun buildBaseUrl_rejectsPlaintextEntitlementServerBeforeAddingCredentials() {
        val request = ServiceEntitlementRequest(authentication_token = "local-test-token")
        builder(request, deviceTerminalId = "local-test-device")
            .buildBaseUrl("http://example.com/entitlement")
    }

    @Test
    fun buildBaseUrl_preservesHttpsForEntitlementRequest() {
        val request = ServiceEntitlementRequest(temporary_token = "local-test-token")
        val url = builder(request, deviceTerminalId = "local-test-device")
            .buildBaseUrl("https://example.com/entitlement")
        assertTrue(url.isHttps)
        assertEquals("local-test-token", url.queryParameter("temporary_token"))
    }

    @Test
    fun buildBaseUrl_explicitTerminalIdOverridesDeviceFallback() {
        val request = ServiceEntitlementRequest(terminal_id = "request-terminal-id")
        val url = builder(request, deviceTerminalId = "device-imei")
            .buildBaseUrl("https://example.com/entitlement")

        assertEquals("request-terminal-id", url.queryParameter("terminal_id"))
    }

    @Test
    fun buildBaseUrl_emptyTerminalIdUsesDeviceFallback() {
        val request = ServiceEntitlementRequest(terminal_id = "")
        val url = builder(request, deviceTerminalId = "device-imei")
            .buildBaseUrl("https://example.com/entitlement")

        assertEquals("device-imei", url.queryParameter("terminal_id"))
    }

    private fun builder(
        request: ServiceEntitlementRequest,
        deviceTerminalId: String?
    ) = ServiceEntitlementBuilder(
        imsi = "",
        iccid = null,
        terminalId = deviceTerminalId,
        terminalVendor = "",
        terminalModel = "",
        terminalSoftwareVersion = "",
        groupIdLevel1 = null,
        eapId = "",
        appIds = listOf("ap2014"),
        req = request
    )
}
