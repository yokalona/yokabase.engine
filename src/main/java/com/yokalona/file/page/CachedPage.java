package com.yokalona.file.page;

import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.file.Array;
import com.yokalona.file.CachedArrayProvider;
import com.yokalona.file.exceptions.ReadOverflowException;

public class CachedPage<Type> implements Page<Type> {

    public static final int MAX_CACHE_SIZE = 1024;

    public final Page<Type> page;
    protected final CachedArrayProvider<Type> array;

    public CachedPage(Page<Type> page) {
        this.page = page;
        this.array = new CachedArrayProvider<>(Math.min(MAX_CACHE_SIZE, free()) / page.serializer().sizeOf(), page::get);
    }

    @Override
    public int
    size() {
        return page.size();
    }

    @Override
    public int
    free() {
        return page.free();
    }

    @Override
    public int
    occupied() {
        return page.occupied();
    }

    @Override
    public Type
    get(int index) {
        if (index < 0 || index >= size()) throw new ReadOverflowException(size(), index);
        return array.get(index);
    }

    @Override
    public int
    append(Type value) {
        int append = page.append(value);
        array.invalidate(page.size() - 1);
        array.length(page.size());
        return append;
    }

    @Override
    public int
    remove(int index) {
        int remove = page.remove(index);
        array.invalidate();
        array.length(remove);
        return remove;
    }

    @Override
    public void
    flush() {
        page.flush();
    }

    @Override
    public FixedSizeSerializer<Type>
    serializer() {
        return page.serializer();
    }

    @Override
    public ASPage.Configuration
    configuration() {
        return page.configuration();
    }

    @Override
    public Array<Type>
    read(Class<Type> ignore) {
        array.length(size());
        return array;
    }

    @Override
    public void
    clear() {
        page.clear();
        array.length(0);
        array.invalidate();
    }
}
