package com.yokalona.file.page;

import com.yokalona.array.serializers.Serializer;
import com.yokalona.file.Cache;
import com.yokalona.file.CachedArrayProvider;

public class CachedDataSpace<Type> extends DataSpace<Type> {

    public static int MAX_CACHE_SIZE = ASPage.MAX_CACHE_SIZE;
    private final CachedArrayProvider<Type> array;

    public CachedDataSpace(Serializer<Type> serializer, ASPage.Configuration configuration) {
        super(serializer, configuration);
        this.array = new CachedArrayProvider<>(MAX_CACHE_SIZE, this::read);
    }

    @Override
    public Type
    get(int index) {
        return array.get(index);
    }

    @Override
    public void
    set(int index, Type value) {
        super.set(index, value);
        array.invalidate(index);
    }

    @Override
    public int
    insert(int address, Type value) {
        int insert = super.insert(address, value);
        array.invalidate(insert - 1);
        array.length(insert);
        return insert;
    }

    @Override
    public int
    remove(int index) {
        int remove = super.remove(index);
        array.length(remove);
        array.adjust(index, -1);
        return remove;
    }

    @Override
    public void
    clear() {
        super.clear();
        array.length(0);
    }

    public Cache<Type>
    read() {
        return array;
    }
}
