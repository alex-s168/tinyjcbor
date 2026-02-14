package dev.vxcc.tinyjcbor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class UnexpectedCborException extends RuntimeException {
    public static class WrongTag extends UnexpectedCborException {
        @Nullable public final Long expected;
        public final long got;

        public WrongTag(@Nullable Long expected, long got) {
            this.expected = expected;
            this.got = got;
        }

        @Override
        public String toString() {
            var sb = new StringBuilder();
            sb.append("Unexpected CBOR tag! Got: ");
            sb.append(got);
            if (expected != null) {
                sb.append(", expected: ");
                sb.append(expected);
            } else {
                sb.append(", expected other tag");
            }
            sb.append('!');
            return sb.toString();
        }
    }

    public static class ExpectedEndOfArray extends UnexpectedCborException {
        public final long expectedLength;

        public ExpectedEndOfArray(long expectedLength) {
            this.expectedLength = expectedLength;
        }

        @Override
        public String toString() {
            return "Expect array to end after " + expectedLength + " elements! (Too many elements)";
        }
    }


    public static class UnexpectedEndOfArray extends UnexpectedCborException {
        public final long gotLength;

        public UnexpectedEndOfArray(long gotLength) {
            this.gotLength = gotLength;
        }

        @Override
        public String toString() {
            return "Didn't expect array to end after " + gotLength + " elements! (Too few elements)";
        }
    }

    public static class UnexpectedType extends UnexpectedCborException {
        @Nullable public final String expected;
        @NotNull public final CborType found;

        public UnexpectedType(@Nullable String expected, @NotNull CborType found) {
            this.expected = expected;
            this.found = found;
        }

        @Override
        public String toString() {
            var sb = new StringBuilder();
            sb.append("Unexpected CBOR type! Got: ");
            sb.append(found);
            if (expected != null) {
                sb.append(", expected: ");
                sb.append(expected);
            } else {
                sb.append(", expected something else");
            }
            sb.append('!');
            return sb.toString();
        }
    }
}
