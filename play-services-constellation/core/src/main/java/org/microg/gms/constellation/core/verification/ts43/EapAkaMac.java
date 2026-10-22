/*
 * SPDX-FileCopyrightText: 2026 microG contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.constellation.core.verification.ts43;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** RFC 4187 EAP-AKA message authentication, with the AT_MAC field zeroed for hashing. */
public final class EapAkaMac {
    private EapAkaMac() {}

    public static byte[] hmacSha1(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean verifyChallenge(byte[] key, byte[] packet, int macValueOffset) {
        if (key == null || packet == null || macValueOffset < 8
                || macValueOffset > packet.length - 16) {
            return false;
        }

        byte[] receivedMac = Arrays.copyOfRange(packet, macValueOffset, macValueOffset + 16);
        byte[] unsignedPacket = packet.clone();
        Arrays.fill(unsignedPacket, macValueOffset, macValueOffset + 16, (byte) 0);
        byte[] calculatedMac = hmacSha1(key, unsignedPacket);
        return calculatedMac != null
                && MessageDigest.isEqual(receivedMac, Arrays.copyOf(calculatedMac, 16));
    }

    /** RFC 4187 section 10.8: AKA RES is 32–128 bits; CK and IK are 128 bits each. */
    public static boolean hasValidUsimSuccessLengths(byte[] res, byte[] ck, byte[] ik) {
        return res != null && res.length >= 4 && res.length <= 16
                && ck != null && ck.length == 16
                && ik != null && ik.length == 16;
    }

    /** RFC 4187 section 10.9: AKA synchronization failures carry exactly 112-bit AUTS. */
    public static boolean hasValidAutsLength(byte[] auts) {
        return auts != null && auts.length == 14;
    }
}
