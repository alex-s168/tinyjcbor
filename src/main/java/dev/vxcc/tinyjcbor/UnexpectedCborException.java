package dev.vxcc.tinyjcbor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class UnexpectedCborException extends RuntimeException {
    public static class WrongTag extends UnexpectedCborException {
        @Nullable public Long expected;
        public long got;

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

    public static class UnexpectedType extends UnexpectedCborException {
        @Nullable public String expected;
        @NotNull public CborType found;

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
