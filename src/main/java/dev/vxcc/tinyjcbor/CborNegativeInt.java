package dev.vxcc.tinyjcbor;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

/**
 * A negative integer in the range -2^64..-1 inclusive, as used by CBOR.
 *
 * @since 1.1.0
 */
public final class CborNegativeInt {
    private static final BigInteger U64_MAX_NEG = BigInteger.valueOf(2).pow(64).negate();

   /**
    * The value of the item is -1 minus the argument.
    * For example, the integer -500 would be represented as 499 in decimal.
    * @since 1.1.0
    */
    public final long _raw;

    private CborNegativeInt(long raw) {
        this._raw = raw;
    }

    public static @NotNull CborNegativeInt fromRaw(long raw) {
        return new CborNegativeInt(raw);
    }

    public static @NotNull CborNegativeInt fromSignedLong(long v) {
        if (v >= 0)
            throw new IllegalArgumentException();
        return new CborNegativeInt(-v - 1);
    }

    public static @NotNull CborNegativeInt from(@NotNull BigInteger b) {
        if (b.signum() >= 0 || b.compareTo(U64_MAX_NEG) <= 0)
            throw new IllegalArgumentException();
        return new CborNegativeInt(b.negate().subtract(BigInteger.ONE).longValue());
    }

    public @NotNull BigInteger toBigInt() {
        return IntShit.bigIntFromUL(_raw).add(BigInteger.ONE).negate();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CborNegativeInt i)
            return _raw == i._raw;
        return false;
    }

    @Override
    public @NotNull String toString() {
        return toBigInt().toString();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(_raw);
    }
}
