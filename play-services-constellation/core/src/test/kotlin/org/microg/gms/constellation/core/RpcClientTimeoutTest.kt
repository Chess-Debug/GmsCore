/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class RpcClientTimeoutTest {
    @Test
    fun usesStockSixtySecondTimeoutsAcrossAllOperations() {
        val client = newConstellationHttpClient()
        val expected = TimeUnit.SECONDS.toMillis(CONSTELLATION_RPC_TIMEOUT_SECONDS).toInt()

        assertArrayEquals(
            intArrayOf(expected, expected, expected, expected),
            intArrayOf(
                client.connectTimeoutMillis,
                client.readTimeoutMillis,
                client.writeTimeoutMillis,
                client.callTimeoutMillis
            )
        )
    }
}
