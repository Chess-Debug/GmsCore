/*
 * SPDX-FileCopyrightText: 2026 microG contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.constellation.core.verification.ts43;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Test;

/** Published RFC 2202 section 3 known-answer tests, without simulated SIM responses. */
public final class EapAkaMacTest {
    private static byte[] decodeHex(String text) {
        byte[] result = new byte[text.length() / 2];
        for (int i = 0; i < result.length; ++i) {
            result[i] = (byte) Integer.parseInt(text.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }

    @Test
    public void hmacSha1Rfc2202Case1() {
        byte[] key = new byte[20];
        Arrays.fill(key, (byte) 0x0b);
        assertArrayEquals(
                decodeHex("b617318655057264e28bc0b6fb378c8ef146be00"),
                EapAkaMac.hmacSha1(key, "Hi There".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    public void hmacSha1Rfc2202Case2() {
        assertArrayEquals(
                decodeHex("effcdf6ae5eb2fa2d27416d5f184df9c259a7c79"),
                EapAkaMac.hmacSha1(
                        "Jefe".getBytes(StandardCharsets.US_ASCII),
                        "what do ya want for nothing?".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    public void missingEapPacketIsRejected() {
        byte[] key = new byte[20];
        assertFalse(EapAkaMac.verifyChallenge(key, null, 8));
        assertFalse(EapAkaMac.verifyChallenge(key, new byte[0], 8));
    }

    @Test
    public void rfc4187UsimSuccessLengthBounds() {
        // Length-only checks from RFC 4187 section 10.8, not simulated USIM credentials.
        assertTrue(EapAkaMac.hasValidUsimSuccessLengths(new byte[4], new byte[16], new byte[16]));
        assertTrue(EapAkaMac.hasValidUsimSuccessLengths(new byte[16], new byte[16], new byte[16]));
        assertFalse(EapAkaMac.hasValidUsimSuccessLengths(new byte[3], new byte[16], new byte[16]));
        assertFalse(EapAkaMac.hasValidUsimSuccessLengths(new byte[17], new byte[16], new byte[16]));
        assertFalse(EapAkaMac.hasValidUsimSuccessLengths(new byte[4], new byte[15], new byte[16]));
        assertFalse(EapAkaMac.hasValidUsimSuccessLengths(new byte[4], new byte[16], new byte[15]));
    }

    @Test
    public void rfc4187AutsLengthIsExactlyFourteen() {
        // RFC 4187 section 10.9, structural length checks only.
        assertTrue(EapAkaMac.hasValidAutsLength(new byte[14]));
        assertFalse(EapAkaMac.hasValidAutsLength(new byte[13]));
        assertFalse(EapAkaMac.hasValidAutsLength(new byte[15]));
        assertFalse(EapAkaMac.hasValidAutsLength(null));
    }
}
