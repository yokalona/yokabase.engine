package com.yokalona.file;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;

public class CachedArrayProvider<Type> implements Cache<Type>, Invalidatable {

    private int length;
    private final int[] indices;
    private final Object[] cache;
    private final Function<Integer, Type> getter;

    public CachedArrayProvider(int size, Function<Integer, Type> getter) {
        assert size >= 0;

        this.length = size;
        this.getter = getter;
        this.indices = new int[size];
        this.cache = new Object[size];
        Arrays.fill(this.indices, -1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Type
    get(int index) {
        int idx = index % indices.length;
        int prior = indices[idx];
        if (prior != index) {
            cache[idx] = getter.apply(index);
            indices[idx] = index;
        }
        return (Type) cache[idx];
    }

    public void
    length(int length) {
        this.length = length;
    }

    @Override
    public int
    length() {
        return length;
    }

    @Override
    public void
    invalidate(int index) {
        if (indices[index % indices.length] != index) return;
        indices[index % indices.length] = -1;
        cache[index % indices.length] = null;
    }

    public void
    adjust(int index, int by) {
        for (int i = index; i < length; i ++) if (indices[i] == i) indices[i] += by;
    }

    @Override
    public Iterator<Type> iterator() {
        return new Iterator<>() {

            int current = -1;

            @Override
            public boolean hasNext() {
                return ++current < length;
            }

            @Override
            public Type next() {
                return get(current);
            }
        };
    }
}
