package dev.vxcc.tinyjcbor;

import java.math.BigInteger;

final class IntShit {
    public static BigInteger bigIntFromUL(long l) {
        return new BigInteger(
                1,
                new byte[] {
                        (byte) ((l >> 56) & 0xFFL),
                        (byte) ((l >> 48) & 0xFFL),
                        (byte) ((l >> 40) & 0xFFL),
                        (byte) ((l >> 32) & 0xFFL),
                        (byte) ((l >> 24) & 0xFFL),
                        (byte) ((l >> 16) & 0xFFL),
                        (byte) ((l >> 8 ) & 0xFFL),
                        (byte) ((l      ) & 0xFFL)
                }
        );
    }
}
