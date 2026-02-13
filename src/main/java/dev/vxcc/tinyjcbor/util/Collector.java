package dev.vxcc.tinyjcbor.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

@FunctionalInterface
public interface Collector<I, O> {
    @NotNull O collect(@NotNull Iterator<I> iter);
}
