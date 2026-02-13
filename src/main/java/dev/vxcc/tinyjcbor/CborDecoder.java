package dev.vxcc.tinyjcbor;

import dev.vxcc.tinyjcbor.serde.CborItemDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/** Uses the buffer's byte order as network order for integers & floats */
public final class CborDecoder {
    @NotNull
    private final ByteBuffer buffer;

    private long tokenArg;
    private int tokenMajorType = 69;
    private int tokenAdditionalInfo;
    private boolean tokenIndefiniteLength;

    public CborDecoder(@NotNull ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public static final class Snapshot {
        private int position;
        private long tokenArg;
        private int tokenMajorType;
        private int tokenAdditionalInfo;
        private boolean tokenIndefiniteLength;

        public void from(@NotNull CborDecoder decoder) {
            position = decoder.buffer.position();
            tokenArg = decoder.tokenArg;
            tokenMajorType = decoder.tokenMajorType;
            tokenAdditionalInfo = decoder.tokenAdditionalInfo;
            tokenIndefiniteLength = decoder.tokenIndefiniteLength;
        }
    }

    public void reset(@NotNull Snapshot snapshot) {
        buffer.position(snapshot.position);
        tokenArg = snapshot.tokenArg;
        tokenMajorType = snapshot.tokenMajorType;
        tokenAdditionalInfo = snapshot.tokenAdditionalInfo;
        tokenIndefiniteLength = snapshot.tokenIndefiniteLength;
    }

    public boolean hasNext() {
        return buffer.hasRemaining();
    }

    private void nextToken() throws InvalidCborException {
        byte firstByte = buffer.get();
        tokenMajorType = (firstByte & 0b11100000) >> 5;
        tokenAdditionalInfo = firstByte & 0b11111;

        tokenArg = 0;
        tokenIndefiniteLength = false;

        switch (tokenAdditionalInfo) {
            case 24: tokenArg = buffer.get(); break;
            case 25: tokenArg = buffer.getShort(); break;
            case 26: tokenArg = buffer.getInt(); break;
            case 27: tokenArg = buffer.getLong(); break;
            case 28:
            case 29:
            case 30: /* reserved */
                throw new InvalidCborException();
            case 31: /* no arg */
                switch (tokenMajorType) {
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        tokenIndefiniteLength = true;
                        break;
                    case 7: break;
                    default:
                        throw new InvalidCborException();
                }
                break;
            default: /* less than 24 */
                tokenArg = tokenAdditionalInfo;
                break;
        }
    }

    private @NotNull CborType currentTokenType() {
        return switch (tokenMajorType) {
            case 0 -> CborType.UnsignedInteger;
            case 1 -> CborType.NegativeInteger;
            case 2 -> CborType.Bytes;
            case 3 -> CborType.Utf8String;
            case 4 -> CborType.Array;
            case 5 -> CborType.Map;
            case 6 -> CborType.Tagged;
            case 7 -> switch (tokenAdditionalInfo) {
                case 25 -> CborType.Float16;
                case 26 -> CborType.Float32;
                case 27 -> CborType.Float64;
                /* 28-30 is already ruled out by [nextToken] */
                case 31 -> CborType.Break;
                default -> switch ((int) tokenArg) {
                    case 20 -> CborType.False;
                    case 21 -> CborType.True;
                    case 22 -> CborType.Null;
                    case 23 -> CborType.Undefined;
                    default -> CborType.SimpleValue;
                };
            };
            default -> throw new RuntimeException("Forgot to call nextToken()");
        };
    }

    @NotNull
    private final Snapshot peekSnapshot = new Snapshot();

    /** returns the type of the next token, without reading it */
    public @Nullable CborType peekTokenType() throws InvalidCborException {
        if (!hasNext())
            return null;
        peekSnapshot.from(this);
        nextToken();
        var ty = currentTokenType();
        reset(peekSnapshot);
        return ty;
    }

    public byte readSimple() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo > 24)
            throw new UnexpectedCborException.UnexpectedType(CborType.SimpleValue.name(), currentTokenType());
        return (byte) tokenArg;
    }

    public long readUInt() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 0)
            throw new UnexpectedCborException.UnexpectedType(CborType.UnsignedInteger.name(), currentTokenType());
        return tokenArg;
    }

    public long readInt() throws UnexpectedCborException {
        nextToken();
        return switch (tokenMajorType) {
            case 0 -> tokenArg;
            case 1 -> -1 - tokenArg;
            default ->
                    throw new UnexpectedCborException.UnexpectedType(CborType.UnsignedInteger.name() + " or " + CborType.NegativeInteger.name(), currentTokenType());
        };
    }

    public short readFloat16() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo != 25)
            throw new UnexpectedCborException.UnexpectedType(CborType.Float16.name(), currentTokenType());
        return (short) tokenArg;
    }

    public float readFloat32() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo != 26)
            throw new UnexpectedCborException.UnexpectedType(CborType.Float32.name(), currentTokenType());
        return Float.intBitsToFloat((int) tokenArg);
    }

    public double readFloat64() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo != 27)
            throw new UnexpectedCborException.UnexpectedType(CborType.Float64.name(), currentTokenType());
        return Double.longBitsToDouble(tokenArg);
    }

    public long readTag() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 6)
            throw new UnexpectedCborException.UnexpectedType(CborType.Tagged.name(), currentTokenType());
        return tokenArg;
    }

    public boolean readBool() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo > 24 || !(tokenArg == 20 || tokenArg == 21))
            throw new UnexpectedCborException.UnexpectedType(CborType.True.name() + " or " + CborType.False.name(), currentTokenType());
        return tokenArg == 21;
    }

    /**
     * expects simple value = 23
     */
    public void readUndefined() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo > 24 || tokenArg != 23)
            throw new UnexpectedCborException.UnexpectedType(CborType.Undefined.name(), currentTokenType());
    }

    /**
     * expects simple value = 22
     */
    public void readNull() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo > 24 || tokenArg != 22)
            throw new UnexpectedCborException.UnexpectedType(CborType.Undefined.name(), currentTokenType());
    }

    /**
     * @return the known length of the array, or [Long.MIN_VALUE] if it's an indefinite length array (terminated by break)
     */
    public long readArray() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 4)
            throw new UnexpectedCborException.UnexpectedType(CborType.Array.name(), currentTokenType());
        if (tokenIndefiniteLength)
            return Long.MIN_VALUE;
        return tokenArg;
    }

    /**
     * @return the known number of pairs in the map, or [Long.MIN_VALUE] if it's an indefinite length map (terminated by break)
     */
    public long readMap() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 5)
            throw new UnexpectedCborException.UnexpectedType(CborType.Map.name(), currentTokenType());
        if (tokenIndefiniteLength)
            return Long.MIN_VALUE;
        return tokenArg;
    }

    private final IndefiniteByteArrayReader indefiniteByteArrayReader = new IndefiniteByteArrayReader();
    private final FiniteByteArrayReader finiteByteArrayReader = new FiniteByteArrayReader();

    public ByteArrayReader getBytes() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 2)
            throw new UnexpectedCborException.UnexpectedType(CborType.Bytes.name(), currentTokenType());
        if (tokenIndefiniteLength) {
            indefiniteByteArrayReader.init(2, CborType.Bytes.name());
            return indefiniteByteArrayReader;
        }
        finiteByteArrayReader.init(tokenArg);
        return finiteByteArrayReader;
    }

    private final ByteArrayReaderInputStream byteArrayReaderInputStream = new ByteArrayReaderInputStream();

    public Reader getUtf8() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 3)
            throw new UnexpectedCborException.UnexpectedType(CborType.Utf8String.name(), currentTokenType());

        ByteArrayReader byteReader;
        if (tokenIndefiniteLength) {
            indefiniteByteArrayReader.init(3, CborType.Utf8String.name());
            byteReader = indefiniteByteArrayReader;
        } else {
            finiteByteArrayReader.init(tokenArg);
            byteReader = finiteByteArrayReader;
        }

        return new InputStreamReader(byteReader.inputStream(), StandardCharsets.UTF_8);
    }

    public final static class ByteArrayReaderInputStream extends InputStream {
        private ByteArrayReader reader;

        private void init(@NotNull ByteArrayReader reader) {
            this.reader = reader;
        }

        private ByteArrayReaderInputStream() {}

        @Override
        public int read() {
            if (!reader.hasNext())
                return -1;
            return reader.nextByte() & 0xFF;
        }

        @Override
        public int read(byte @NotNull [] b) {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte @NotNull [] b, int off, int len) {
            if (!reader.hasNext())
                return -1;
            return reader.next(b, off, len);
        }

        @Override
        public int available() {
            return reader.guessRemainingLength();
        }
    }

    public sealed abstract class ByteArrayReader implements Iterator<Byte> permits FiniteByteArrayReader, IndefiniteByteArrayReader {
        @Override
        public final Byte next() {
            return nextByte();
        }

        public abstract byte nextByte();

        /** reads up to [length] next bytes into [dst] at [offset], and returns how many bytes have been read */
        public abstract int next(byte[] dst, int offset, int length);

        /** reads up to [length] next bytes into [dst] */
        public final int next(byte[] dst) {
            return next(dst, 0, dst.length);
        }

        /** this will only return 0 if the array is at the end! otherwise will always return at least 1 */
        public int guessRemainingLength() {
            return 1;
        }

        public void skipToEnd() {
            byte[] buf = new byte[512];
            while (hasNext()) {
                next(buf);
            }
        }

        public ByteBuffer readAll(ByteBuffer dst) {
            byte[] buf = new byte[512];
            while (hasNext()) {
                dst.put(buf, 0, next(buf));
            }
            return dst;
        }

        public void readAll(OutputStream dst) throws IOException {
            byte[] buf = new byte[512];
            while (hasNext()) {
                dst.write(buf, 0, next(buf));
            }
        }

        public byte[] readAll() {
            var out = new ByteArrayOutputStream();
            byte[] buf = new byte[512];
            while (hasNext()) {
                out.write(buf, 0, next(buf));
            }
            return out.toByteArray();
        }

        public final ByteArrayReaderInputStream inputStream() {
            byteArrayReaderInputStream.init(this);
            return byteArrayReaderInputStream;
        }
    }

    private final class FiniteByteArrayReader extends ByteArrayReader {
        private long remaining;

        public void init(long length) {
            remaining = length;
        }

        @Override
        public boolean hasNext() {
            return remaining != 0;
        }

        @Override
        public byte nextByte() {
            if (remaining == 0)
                throw new NoSuchElementException();
            remaining -= 1;
            return buffer.get();
        }

        @Override
        public int next(byte[] dst, int offset, int length) {
            if (!hasNext())
                return 0;
            int read = length;
            if (read > remaining)
                read = guessRemainingLength();
            remaining -= read;
            buffer.get(dst, offset, read);
            return read;
        }

        @Override
        public int guessRemainingLength() {
            if (remaining < 0 || remaining > Integer.MAX_VALUE)
                return Integer.MAX_VALUE;
            return (int) remaining;
        }
    }

    private final class IndefiniteByteArrayReader extends ByteArrayReader {
        private boolean _hasFinite;
        private int majorType;
        @Nullable private String expectedName;
        private boolean _end;

        public void init(int majorType, @Nullable String expectedName) {
            _hasFinite = false;
            this.majorType = majorType;
            this.expectedName = expectedName;
            this._end = false;
        }

        private @Nullable FiniteByteArrayReader chunkReader() {
            if (_end)
                return null;
            if (_hasFinite && finiteByteArrayReader.hasNext())
                return finiteByteArrayReader;
            _hasFinite = false;

            if (peekTokenType() == CborType.Break) {
                nextToken();
                _end = true;
                return null;
            }

            nextToken();

            if (tokenMajorType != majorType || tokenIndefiniteLength)
                throw new UnexpectedCborException.UnexpectedType(expectedName, currentTokenType());
            finiteByteArrayReader.init(tokenArg);
            _hasFinite = true;
            return finiteByteArrayReader;
        }

        @Override
        public byte nextByte() {
            var chunk = chunkReader();
            if (chunk == null)
                throw new NoSuchElementException();
            return chunk.nextByte();
        }

        @Override
        public int next(byte[] dst, int offset, int length) {
            var chunk = chunkReader();
            if (chunk == null)
                return 0;
            return chunk.next(dst, offset, length);
        }

        @Override
        public boolean hasNext() {
            return chunkReader() != null;
        }
    }

    /**
     * this should always follow a call to [nextToken].
     * reads the whole array, including the Break, if indefinite
     */
    public <T> @NotNull Iterator<T> readArray(@NotNull CborItemDecoder<T> elementDecoder) throws UnexpectedCborException {
        long length = readArray();
        if (length == Long.MIN_VALUE) {
            return new IndefiniteLengthArrayIterator<>(elementDecoder);
        }
        return new FiniteLengthArrayIterator<>(elementDecoder, length);
    }

    /**
     * this should always follow a call to [nextToken].
     * reads the whole map, including the Break, if indefinite
     */
    public <K, V, T> @NotNull Iterator<T> readMap(@NotNull CborItemDecoder<K> keyDecoder,
                                                  @NotNull CborItemDecoder<V> valDecoder,
                                                  @NotNull BiFunction<K, V, T> makeItem) throws UnexpectedCborException {
        long length = readMap();
        if (length == Long.MIN_VALUE) {
            return new IndefiniteLengthMapIterator<>(keyDecoder, valDecoder, makeItem);
        }
        return new FiniteLengthMapIterator<>(keyDecoder, valDecoder, makeItem, length);
    }

    /**
     * this should always follow a call to [nextToken].
     * reads the whole map, including the Break, if indefinite
     */
    public <K, V> void readMap(@NotNull CborItemDecoder<K> keyDecoder,
                               @NotNull CborItemDecoder<V> valDecoder,
                               @NotNull BiConsumer<K, V> each) throws UnexpectedCborException {

        var iter = readMap(keyDecoder, valDecoder, (a, b) -> {
            each.accept(a, b);
            return null;
        });
        while (iter.hasNext())
            iter.next();
    }

    private final class IndefiniteLengthArrayIterator<T> implements Iterator<T> {
        @NotNull private final CborItemDecoder<T> elementDecoder;

        public IndefiniteLengthArrayIterator(@NotNull CborItemDecoder<T> elementDecoder) {
            this.elementDecoder = elementDecoder;
        }

        @Override
        public boolean hasNext() {
            if (peekTokenType() == CborType.Break) {
                nextToken();
                return false;
            }
            return true;
        }

        @Override
        public T next() {
            if (peekTokenType() == CborType.Break)
                throw new NoSuchElementException();
            return elementDecoder.next(CborDecoder.this);
        }
    }

    private final class FiniteLengthArrayIterator<T> implements Iterator<T>
    {
        @NotNull private final CborItemDecoder<T> elementDecoder;
        private long remaining;

        public FiniteLengthArrayIterator(@NotNull CborItemDecoder<T> elementDecoder,
                                         long length) {
            this.elementDecoder = elementDecoder;
            this.remaining = length;
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        public T next() {
            if (remaining <= 0)
                throw new NoSuchElementException();
            remaining -= 1;
            return elementDecoder.next(CborDecoder.this);
        }
    }

    private final class IndefiniteLengthMapIterator<K, V, T> implements Iterator<T> {
        @NotNull private final CborItemDecoder<K> keyDecoder;
        @NotNull private final CborItemDecoder<V> valDecoder;
        @NotNull private final BiFunction<K, V, T> makeItem;

        public IndefiniteLengthMapIterator(@NotNull CborItemDecoder<K> keyDecoder,
                                           @NotNull CborItemDecoder<V> valDecoder,
                                           @NotNull BiFunction<K, V, T> makeItem) {
            this.keyDecoder = keyDecoder;
            this.valDecoder = valDecoder;
            this.makeItem = makeItem;
        }

        @Override
        public boolean hasNext() {
            if (peekTokenType() == CborType.Break) {
                nextToken(); // Consume the break token
                return false;
            }
            return true;
        }

        @Override
        public T next() {
            if (peekTokenType() == CborType.Break)
                throw new NoSuchElementException();

            K k = keyDecoder.next(CborDecoder.this);
            try {
                if (peekTokenType() == CborType.Break)
                    throw new InvalidCborException();
            } catch (InvalidCborException e) {
                throw new IllegalStateException("CBOR parsing error", e);
            }
            V v = valDecoder.next(CborDecoder.this);
            return makeItem.apply(k, v);
        }
    }

    private final class FiniteLengthMapIterator<K, V, T> implements Iterator<T> {
        @NotNull private final CborItemDecoder<K> keyDecoder;
        @NotNull private final CborItemDecoder<V> valDecoder;
        @NotNull private final BiFunction<K, V, T> makeItem;
        private long remaining;

        public FiniteLengthMapIterator(@NotNull CborItemDecoder<K> keyDecoder,
                                       @NotNull CborItemDecoder<V> valDecoder,
                                       @NotNull BiFunction<K, V, T> makeItem,
                                       long length) {
            this.keyDecoder = keyDecoder;
            this.valDecoder = valDecoder;
            this.makeItem = makeItem;
            this.remaining = length;
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        public T next() {
            if (remaining <= 0)
                throw new NoSuchElementException();
            remaining -= 1;
            K k = keyDecoder.next(CborDecoder.this);
            V v = valDecoder.next(CborDecoder.this);
            return makeItem.apply(k, v);
        }
    }
}
