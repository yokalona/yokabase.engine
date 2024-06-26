package com.yokalona.file;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;

public class CachedArrayProvider<Type> implements Array<Type>, Invalidatable {

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
        invalidate();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Type
    get(int index) {
        int idx = index % indices.length;
        int prior = indices[idx];
        if (prior != index) {
            cache[idx] = read(index);
            indices[idx] = index;
        }
        return (Type) cache[idx];
    }

    public Type
    read(int index) {
        return getter.apply(index);
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
    invalidate() {
        Arrays.fill(this.indices, -1);
    }

    @Override
    public Iterator<Type> iterator() {
        return new Iterator<>() {

            int current = -1;

            @Override
            public boolean
            hasNext() {
                return ++current < length;
            }

            @Override
            public Type
            next() {
                return get(current);
            }
        };
    }
}
