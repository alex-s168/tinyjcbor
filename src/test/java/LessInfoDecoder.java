import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import dev.vxcc.tinyjcbor.serde.CborItemDecoder;
import org.jetbrains.annotations.NotNull;

public class LessInfoDecoder<T> implements CborItemDecoder<T> {
    @NotNull
    private final CborItemDecoder<T> impl;
    
    public LessInfoDecoder(@NotNull CborItemDecoder<T> impl) {
        this.impl = impl;
    }

    @Override
    public T next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
        return impl.next(decoder);
    }
}
