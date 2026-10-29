package dev.vxcc.tinyjcbor;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public final class CborInt {
    private static final BigInteger U64_MAX = BigInteger.valueOf(2).pow(64);

    public final boolean _negative;
    /** If _negative: See CborNegativeInt#_raw, otherwise, it's a standard unsigned long */
    public final long _raw;

    private CborInt(boolean negative, long raw) {
        this._negative = negative;
        this._raw = raw;
    }

    public static @NotNull CborInt fromUnsignedLong(long v) {
        return new CborInt(false, v);
    }

    public static @NotNull CborInt fromLong(long v) {
        if (v >= 0)
            return new CborInt(false, v);
        return from(CborNegativeInt.fromSignedLong(v));
    }

    public static @NotNull CborInt from(@NotNull CborNegativeInt i) {
        return new CborInt(true, i._raw);
    }

    public static @NotNull CborInt from(@NotNull BigInteger b) {
        if (b.signum() >= 0) {
            if (b.compareTo(U64_MAX) >= 0)
                throw new ArithmeticException("does not fit");
            return new CborInt(false, b.longValue());
        }
        return from(CborNegativeInt.from(b));
    }

    public @NotNull BigInteger toBigInt() {
        if (_negative)
            return CborNegativeInt.fromRaw(_raw).toBigInt();
        return IntShit.bigIntFromUL(_raw);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CborInt i)
            return _negative == i._negative && _raw == i._raw;
        if (obj instanceof CborNegativeInt i)
            return _negative && _raw == i._raw;
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
