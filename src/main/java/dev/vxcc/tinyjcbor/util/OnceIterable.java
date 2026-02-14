package dev.vxcc.tinyjcbor.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * A Iterable wrapper around an Iterator, that makes sure it's only converted to an iterator once
 * @since 1.0.0-rc.2
 */
public final class OnceIterable<T> implements Iterable<T> {
    @NotNull private final Iterator<T> iter;
    private boolean converted;

    public OnceIterable(@NotNull Iterator<T> iter) {
        this.iter = iter;
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        if (converted)
            throw new IllegalStateException("OnceIterable#iterator() has been called more than once");
        converted = true;
        return iter;
    }
}
