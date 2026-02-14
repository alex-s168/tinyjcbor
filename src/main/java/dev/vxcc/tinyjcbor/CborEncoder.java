package dev.vxcc.tinyjcbor;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class CborEncoder {
    @NotNull
    public final CborRawEncoder unsafe;

    public CborEncoder(@NotNull ByteOrder byteOrder, @NotNull OutputStream out) {
        this.unsafe = new CborRawEncoder(byteOrder, out);
    }

    public void writeUnsigned(long i) throws IOException {
        unsafe.writeUInt(i);
    }

    public void writeSigned(long i) throws IOException {
        unsafe.writeSInt(i);
    }

    public void writeSimple(int b) throws IOException {
        if ((int) (byte) b != b)
            throw new IllegalArgumentException();
        unsafe.writeSimple((byte) b);
    }

    public void writeBool(boolean b) throws IOException {
        unsafe.writeSimple((byte)(b ? 21 : 20));
    }

    public void writeNull() throws IOException {
        unsafe.writeSimple((byte) 22);
    }

    public void writeUndefined() throws IOException {
        unsafe.writeSimple((byte) 23);
    }

    public void writeFloat16(short s) throws IOException {
        unsafe.writeF16(s);
    }

    public void writeFloat32(float f) throws IOException {
        unsafe.writeF32(Float.floatToIntBits(f));
    }

    public void writeFloat64(double d) throws IOException {
        unsafe.writeF64(Double.doubleToLongBits(d));
    }

    public void writeTag(long t) throws IOException {
        unsafe.writeTag(t);
    }

    public void writeByteString(byte @NotNull[] array, int off, int length) throws IOException {
        unsafe.writeBeginFinite(2, length);
        unsafe.out.write(array, off, length);
    }

    public void writeByteString(byte @NotNull[] array) throws IOException {
        writeByteString(array, 0, array.length);
    }

    public void writeTextUtf8(byte @NotNull[] array, int off, int length) throws IOException {
        unsafe.writeBeginFinite(3, length);
        unsafe.out.write(array, off, length);
    }

    public void writeTextUtf8(byte @NotNull[] array) throws IOException {
        writeTextUtf8(array, 0, array.length);
    }

    public void writeText(String s) throws IOException {
        writeTextUtf8(s.getBytes(StandardCharsets.UTF_8));
    }

    @NotNull private final ChunkedByteStringWriter chunkedByteStringWriter = new ChunkedByteStringWriter();
    @NotNull private final ChunkedTextWriter chunkedTextWriter = new ChunkedTextWriter();

    @CheckReturnValue
    public ChunkedByteStringWriter writeChunkedByteString() throws IOException {
        unsafe.writeBeginIndefinite(2);
        chunkedByteStringWriter.init();
        return chunkedByteStringWriter;
    }

    @CheckReturnValue
    public ChunkedTextWriter writeChunkedText() throws IOException {
        unsafe.writeBeginIndefinite(3);
        chunkedTextWriter.init();
        return chunkedTextWriter;
    }

    public final class ChunkedByteStringWriter {
        private boolean end = true;

        private void init() {
            if (!end)
                throw new IllegalStateException("Forgot to call ChunkedByteStringWriter#end()");
            end = false;
        }

        private ChunkedByteStringWriter() {}

        public void writeChunk(byte @NotNull[] array, int off, int length) throws IOException {
            if (end)
                throw new IllegalStateException();
            writeByteString(array, off, length);
        }

        public void writeChunk(byte @NotNull[] array) throws IOException {
            writeChunk(array, 0, array.length);
        }

        public void end() throws IOException {
            if (end)
                throw new IllegalStateException();
            unsafe.writeBreak();
            end = false;
        }
    }

    public final class ChunkedTextWriter {
        private boolean end = true;

        private void init() {
            if (!end)
                throw new IllegalStateException("Forgot to call ChunkedUtf8Writer#end()");
            end = false;
        }

        private ChunkedTextWriter() {}

        public void writeRawUtf8Chunk(byte @NotNull[] array, int off, int length) throws IOException {
            if (end)
                throw new IllegalStateException();
            writeTextUtf8(array, off, length);
        }

        public void writeRawUtf8Chunk(byte @NotNull[] array) throws IOException {
            writeRawUtf8Chunk(array, 0, array.length);
        }

        public void writeChunk(String s) throws IOException {
            writeRawUtf8Chunk(s.getBytes(StandardCharsets.UTF_8));
        }

        public void end() throws IOException {
            if (end)
                throw new IllegalStateException();
            unsafe.writeBreak();
            end = false;
        }
    }

    public void writeArray(long len) throws IOException {
        unsafe.writeBeginFinite(4, len);
    }

    public void writeMap(long len) throws IOException {
        unsafe.writeBeginFinite(5, len);
    }

    @NotNull
    private final CborEncoder.IndefiniteWriter indefiniteWriter = new IndefiniteWriter();

    @CheckReturnValue
    public IndefiniteWriter writeArray() throws IOException {
        unsafe.writeBeginIndefinite(4);
        indefiniteWriter.init();
        return indefiniteWriter;
    }

    @CheckReturnValue
    public IndefiniteWriter writeMap() throws IOException {
        unsafe.writeBeginIndefinite(5);
        indefiniteWriter.init();
        return indefiniteWriter;
    }

    public final class IndefiniteWriter {
        private boolean end = true;

        private void init() {
            if (!end)
                throw new IllegalStateException("Forgot to call IndefiniteWriter#end()");
            end = false;
        }

        private IndefiniteWriter() {}

        public void end() throws IOException {
            if (end)
                throw new IllegalStateException();
            unsafe.writeBreak();
            end = false;
        }
    }
}
