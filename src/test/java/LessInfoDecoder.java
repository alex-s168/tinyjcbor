import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import dev.vxcc.tinyjcbor.serde.CborDeserializer;
import org.jetbrains.annotations.NotNull;

public class LessInfoDecoder<T> implements CborDeserializer<T> {
    @NotNull
    private final CborDeserializer<T> impl;
    
    public LessInfoDecoder(@NotNull CborDeserializer<T> impl) {
        this.impl = impl;
    }

    @Override
    public T next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
        return impl.next(decoder);
    }
}
