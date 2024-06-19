package com.yokalona.file.page;

import com.yokalona.file.Array;
import com.yokalona.file.CachedArrayProvider;

public class CachedDataSpace<Type> implements DataSpace<Type> {

    public static int MAX_CACHE_SIZE = 2;

    private final DataSpace<Type> dataSpace;
    private final CachedArrayProvider<Type> array;

    public CachedDataSpace(DataSpace<Type> dataSpace) {
        this.dataSpace = dataSpace;
        this.array = new CachedArrayProvider<>(MAX_CACHE_SIZE, this.dataSpace::get);
    }

    @Override
    public byte
    pointerSize() {
        return dataSpace.pointerSize();
    }

    @Override
    public Type
    get(int index) {
        return array.get(index);
    }

    @Override
    public int
    address(int index) {
        return dataSpace.address(index);
    }

    @Override
    public void
    set(int index, Type value) {
        dataSpace.set(index, value);
        array.invalidate(index);
    }

    @Override
    public int
    insert(int address, Type value) {
        int insert = dataSpace.insert(address, value);
        array.invalidate(dataSpace.size() - 1);
        array.length(insert);
        return insert;
    }

    @Override
    public int
    remove(int index) {
        int remove = dataSpace.remove(index);
        array.length(remove);
        array.invalidate();
        return remove;
    }

    @Override
    public int
    size() {
        return dataSpace.size();
    }

    @Override
    public void
    clear() {
        dataSpace.clear();
        array.length(0);
        for (int i = 0; i < 10000; i ++) array.invalidate(i);
    }

    @Override
    public int
    occupied() {
        return dataSpace.occupied();
    }

    @Override
    public void
    flush() {
        dataSpace.flush();
    }

    public Array<Type>
    read(Class<Type> ignore) {
        return dataSpace.read(ignore);
    }

    @Override
    public Array<Integer>
    addresses() {
        return dataSpace.addresses();
    }

}
