import com.code_intelligence.jazzer.junit.FuzzTest;
import dev.vxcc.tinyjcbor.Cbor;
import dev.vxcc.tinyjcbor.InvalidCborException;
import dev.vxcc.tinyjcbor.util.CborValue;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FuzzingTest {
    @FuzzTest
    void serDe(byte[] input) {
        if (input.length != 0) {
            CborValue v = null;
            try {
                var buf = ByteBuffer.wrap(input);
                v = Cbor.decode(buf);
            } catch (InvalidCborException ignored) {}
            if (v != null) {
                assertEquals(v, v.asDecoder().read());
            }
        }
    }
}
