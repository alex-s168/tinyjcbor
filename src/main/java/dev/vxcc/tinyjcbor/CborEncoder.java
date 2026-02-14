package dev.vxcc.tinyjcbor;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Standard CBOR encoder.
 * @since 1.0.0-rc.1
 */
public final class CborEncoder {
    @NotNull
    public final CborRawEncoder unsafe;

    /**
     *
     * @param byteOrder Byte order to use for integers
     * @param out the output stream to serialize to.<br>
     *            You should always buffer this if you write directly to IO!
     * @since 1.0.0-rc.1
     */
    public CborEncoder(@NotNull ByteOrder byteOrder, @NotNull OutputStream out) {
        this.unsafe = new CborRawEncoder(byteOrder, out);
    }

    /**
     * Write an unsigned integer
     * @param i treated as unsigned integer
     * @throws IOException when writing to the {@code OutputStream} fails
     * @since 1.0.0-rc.1
     */
    public void writeUnsigned(long i) throws IOException {
        unsafe.writeUInt(i);
    }

    /**
     * Write a positive or negative integer
     * @throws IOException when writing to the {@code OutputStream} fails
     * @since 1.0.0-rc.1
     */
    public void writeSigned(long i) throws IOException {
        unsafe.writeSInt(i);
    }

    /**
     * Write a CBOR simple value
     * @param b a byte
     * @throws IOException when writing to the {@code OutputStream} fails
     * @since 1.0.0-rc.1
     */
    public void writeSimple(int b) throws IOException {
        if ((int) (byte) b != b)
            throw new IllegalArgumentException();
        unsafe.writeSimple((byte) b);
    }

    /**
     * Write a standard boolean value
     * @throws IOException when writing to the {@code OutputStream} fails
     * @since 1.0.0-rc.1
     */
    public void writeBool(boolean b) throws IOException {
        unsafe.writeSimple((byte)(b ? 21 : 20));
    }

    /**
     * Write a standard null value
     * @throws IOException when writing to the {@code OutputStream} fails
     * @since 1.0.0-rc.1
     */
    public void writeNull() throws IOException {
        unsafe.writeSimple((byte) 22);
    }

    /**
     * Write a standard CBOR undefined value
     * @throws IOException when writing to the {@code OutputStream} fails
     * @since 1.0.0-rc.1
     */
    public void writeUndefined() throws IOException {
        unsafe.writeSimple((byte) 23);
    }

    /**
     * Write a 16-bit float
     * @param s IEEE754 16-bit float
     * @throws IOException when writing to the {@code OutputStream} fails
     * @since 1.0.0-rc.1
     */
    public void writeFloat16(short s) throws IOException {
        unsafe.writeF16(s);
    }

    /**
     * Write a 32-bit float
     * @throws IOException when writing to the {@code OutputStream} fails
     * @since 1.0.0-rc.1
     */
    public void writeFloat32(float f) throws IOException {
        unsafe.writeF32(Float.floatToIntBits(f));
    }

    /**
     * Write a 64-bit float
     * @throws IOException when writing to the {@code OutputStream} fails
     * @since 1.0.0-rc.1
     */
    public void writeFloat64(double d) throws IOException {
        unsafe.writeF64(Double.doubleToLongBits(d));
    }

    /**
     * Write a CBOR tag.
     * <p>This has to be followed by another CBOR item.
     * @param t treated as unsigned integer
     * @throws IOException when writing to the {@code OutputStream} fails
     * @since 1.0.0-rc.1
     */
    public void writeTag(long t) throws IOException {
        unsafe.writeTag(t);
    }

    /**
     * Write a byte string (byte array)
     * @throws IOException when writing to the {@code OutputStream} fails
     * @see #writeChunkedByteString()
     * @since 1.0.0-rc.1
     */
    public void writeByteString(byte @NotNull[] array, int off, int length) throws IOException {
        unsafe.writeBeginFinite(2, length);
        unsafe.out.write(array, off, length);
    }

    /**
     * Write a byte string (byte array)
     * @throws IOException when writing to the {@code OutputStream} fails
     * @see #writeChunkedByteString()
     * @since 1.0.0-rc.1
     */
    public void writeByteString(byte @NotNull[] array) throws IOException {
        writeByteString(array, 0, array.length);
    }

    /**
     * Write text (utf8)
     * @throws IOException when writing to the {@code OutputStream} fails
     * @see #writeChunkedText()
     * @since 1.0.0-rc.1
     */
    public void writeTextUtf8(byte @NotNull[] array, int off, int length) throws IOException {
        unsafe.writeBeginFinite(3, length);
        unsafe.out.write(array, off, length);
    }

    /**
     * Write text (utf8)
     * @throws IOException when writing to the {@code OutputStream} fails
     * @see #writeChunkedText()
     * @since 1.0.0-rc.1
     */
    public void writeTextUtf8(byte @NotNull[] array) throws IOException {
        writeTextUtf8(array, 0, array.length);
    }

    /**
     * Write text (utf8)
     * @throws IOException when writing to the {@code OutputStream} fails
     * @see #writeChunkedText()
     * @since 1.0.0-rc.1
     */
    public void writeText(String s) throws IOException {
        writeTextUtf8(s.getBytes(StandardCharsets.UTF_8));
    }

    @NotNull private final ChunkedByteStringWriter chunkedByteStringWriter = new ChunkedByteStringWriter();
    @NotNull private final ChunkedTextWriter chunkedTextWriter = new ChunkedTextWriter();

    /**
     * Begin writing a chunked byte string (byte array) (unknown length at this time)
     * <p>Don't forget to call {@code .end()}!
     * <br><br>
     *
     * Example:
     * <pre><code>
     *     var x = enc.writeChunkedByteString();
     *     x.writeChunk(new byte[]{ 1, 2, 3 });
     *     x.writeChunk(new byte[]{ 4, 5 });
     *     x.end();
     * </code></pre>
     *
     * @throws IOException when writing to the {@code OutputStream} fails
     * @since 1.0.0-rc.1
     */
    @CheckReturnValue
    public ChunkedByteStringWriter writeChunkedByteString() throws IOException {
        unsafe.writeBeginIndefinite(2);
        chunkedByteStringWriter.init();
        return chunkedByteStringWriter;
    }

    /**
     * Begin writing chunked text (utf8) (unknown length at this time)
     * <p>Don't forget to call {@code .end()}!
     * <br><br>
     *
     * Example:
     * <pre><code>
     *     var x = enc.writeChunkedText();
     *     x.writeRawUtf8Chunk(new byte[]{ 65, 66, 67 });
     *     x.writeChunk("xyz");
     *     x.end();
     * </code></pre>
     *
     * @throws IOException when writing to the {@code OutputStream} fails
     * @since 1.0.0-rc.1
     */
    @CheckReturnValue
    public ChunkedTextWriter writeChunkedText() throws IOException {
        unsafe.writeBeginIndefinite(3);
        chunkedTextWriter.init();
        return chunkedTextWriter;
    }

    /**
     * Chunked byte string (byte array) writer
     * <p>Don't forget to call {@code .end()}!
     *
     * @see #writeChunkedText()
     *
     * @since 1.0.0-rc.1
     */
    public final class ChunkedByteStringWriter {
        private boolean end = true;

        private void init() {
            if (!end)
                throw new IllegalStateException("Forgot to call ChunkedByteStringWriter#end()");
            end = false;
        }

        private ChunkedByteStringWriter() {}

        /**
         * Write a chunk
         * @throws IllegalStateException when end has already been called
         * @throws IOException when writing to the {@code OutputStream} fails
         * @since 1.0.0-rc.1
         */
        public void writeChunk(byte @NotNull[] array, int off, int length) throws IOException {
            if (end)
                throw new IllegalStateException();
            writeByteString(array, off, length);
        }

        /**
         * Write a chunk
         * @throws IllegalStateException when end has already been called
         * @throws IOException when writing to the {@code OutputStream} fails
         * @since 1.0.0-rc.1
         */
        public void writeChunk(byte @NotNull[] array) throws IOException {
            writeChunk(array, 0, array.length);
        }

        /**
         * Write the terminator of this item.
         * @throws IllegalStateException when this has already been called
         * @throws IOException when writing to the {@code OutputStream} fails
         * @since 1.0.0-rc.1
         */
        public void end() throws IOException {
            if (end)
                throw new IllegalStateException();
            unsafe.writeBreak();
            end = false;
        }
    }

    /**
     * Chunked text (utf8) writer
     * <p>Don't forget to call {@code .end()}!
     *
     * @see #writeChunkedText()
     *
     * @since 1.0.0-rc.1
     */
    public final class ChunkedTextWriter {
        private boolean end = true;

        private void init() {
            if (!end)
                throw new IllegalStateException("Forgot to call ChunkedUtf8Writer#end()");
            end = false;
        }

        private ChunkedTextWriter() {}

        /**
         * Write a chunk of raw utf8 bytes (unchecked)
         * @throws IllegalStateException when end has already been called
         * @throws IOException when writing to the {@code OutputStream} fails
         * @since 1.0.0-rc.1
         */
        public void writeRawUtf8Chunk(byte @NotNull[] array, int off, int length) throws IOException {
            if (end)
                throw new IllegalStateException();
            writeTextUtf8(array, off, length);
        }

        /**
         * Write a chunk of raw utf8 bytes (unchecked)
         * @throws IllegalStateException when end has already been called
         * @throws IOException when writing to the {@code OutputStream} fails
         * @since 1.0.0-rc.1
         */
        public void writeRawUtf8Chunk(byte @NotNull[] array) throws IOException {
            writeRawUtf8Chunk(array, 0, array.length);
        }

        /**
         * Write a chunk
         * @throws IllegalStateException when end has already been called
         * @throws IOException when writing to the {@code OutputStream} fails
         * @since 1.0.0-rc.1
         */
        public void writeChunk(String s) throws IOException {
            writeRawUtf8Chunk(s.getBytes(StandardCharsets.UTF_8));
        }

        /**
         * Write the terminator of this item.
         * @throws IllegalStateException when this has already been called
         * @throws IOException when writing to the {@code OutputStream} fails
         * @since 1.0.0-rc.1
         */
        public void end() throws IOException {
            if (end)
                throw new IllegalStateException();
            unsafe.writeBreak();
            end = false;
        }
    }

    /**
     * Begin writing an array of the given length
     * <br><br>
     *
     * Example:
     * <pre><code>
     *     encoder.writeArray(3);
     *     encoder.writeSigned(1);
     *     encoder.writeSigned(2);
     *     encoder.writeSigned(3);
     * </code></pre>
     *
     * @throws IOException when writing to the {@code OutputStream} fails
     * @see #writeArray()
     * @since 1.0.0-rc.1
     */
    public void writeArray(long len) throws IOException {
        unsafe.writeBeginFinite(4, len);
    }

    /**
     * Begin writing a map of the given number of items
     * <br><br>
     *
     * Example:
     * <pre><code>
     *     encoder.writeMap(2);
     *     encoder.writeText("username");
     *     encoder.writeText("Max");
     *     encoder.writeText("password");
     *     encoder.writeText("1234");
     * </code></pre>
     *
     * @throws IOException when writing to the {@code OutputStream} fails
     * @see #writeMap()
     * @since 1.0.0-rc.1
     */
    public void writeMap(long numPairs) throws IOException {
        unsafe.writeBeginFinite(5, numPairs);
    }

    @NotNull
    private final CborEncoder.IndefiniteWriter indefiniteWriter = new IndefiniteWriter();

    /**
     * Begin writing an array of currently unknown length
     * <p>Do not forget to call {@code .end()}!
     * <br><br>
     *
     * Example:
     * <pre><code>
     *     var map = encoder.writeArray();
     *     encoder.writeSigned(1);
     *     encoder.writeSigned(2);
     *     encoder.writeSigned(3);
     *     map.end();
     * </code></pre>
     *
     * @throws IOException when writing to the {@code OutputStream} fails
     * @see #writeArray(long)
     * @since 1.0.0-rc.1
     */
    @CheckReturnValue
    public IndefiniteWriter writeArray() throws IOException {
        unsafe.writeBeginIndefinite(4);
        indefiniteWriter.init();
        return indefiniteWriter;
    }

    /**
     * Begin writing a map of currently unknown length
     * <p>Do not forget to call {@code .end()}!
     * <br><br>
     *
     * Example:
     * <pre><code>
     *     var map = encoder.writeMap();
     *     encoder.writeText("username");
     *     encoder.writeText("Max");
     *     encoder.writeText("password");
     *     encoder.writeText("1234");
     *     map.end();
     * </code></pre>
     *
     * @throws IOException when writing to the {@code OutputStream} fails
     * @see #writeMap(long)
     * @since 1.0.0-rc.1
     */
    @CheckReturnValue
    public IndefiniteWriter writeMap() throws IOException {
        unsafe.writeBeginIndefinite(5);
        indefiniteWriter.init();
        return indefiniteWriter;
    }

    /**
     * Don't forget to call {@code .end()}!
     * @since 1.0.0-rc.1
     */
    public final class IndefiniteWriter {
        private boolean end = true;

        private void init() {
            if (!end)
                throw new IllegalStateException("Forgot to call IndefiniteWriter#end()");
            end = false;
        }

        private IndefiniteWriter() {}

        /**
         * @throws IllegalStateException when end has already been called
         * @throws IOException when writing to the {@code OutputStream} fails
         * @since 1.0.0-rc.1
         */
        public void end() throws IOException {
            if (end)
                throw new IllegalStateException();
            unsafe.writeBreak();
            end = false;
        }
    }
}
