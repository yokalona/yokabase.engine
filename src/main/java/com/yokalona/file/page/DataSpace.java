package com.yokalona.file.page;

import com.yokalona.array.serializers.Serializer;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.file.AddressTools;
import com.yokalona.file.exceptions.OffsetException;
import com.yokalona.file.exceptions.PageIsToLargeException;

import java.lang.reflect.Array;

public class DataSpace<Type> {

    private final byte[] space;
    private final byte significantBytes;
    private final ASPage<Integer> index;
    private final Serializer<Type> serializer;

    public DataSpace(byte[] space, int offset, int size, Serializer<Type> serializer) {
        if (size > space.length + offset) throw new PageIsToLargeException(size);
        if (offset < 0 || offset + size > space.length) throw new OffsetException(offset, space.length);
        this.space = space;
        this.serializer = serializer;
        this.significantBytes = AddressTools.significantBytes(size);
        this.index = ASPage.create(size, offset, new CompactIntegerSerializer(significantBytes), space);
    }

    public byte
    pointerSize() {
        return significantBytes;
    }

    public Type
    get(int index) {
        Integer address = this.index.get(index);
        return serializer.deserialize(space, address);
    }

    public int
    address(int index) {
        return this.index.get(index);
    }

    public void
    set(int index, Type value) {
        Integer address = this.index.get(index);
        serializer.serialize(value, space, address);
    }

    public int
    insert(int address, Type value) {
        serializer.serialize(value, space, address);
        return this.index.append(address);
    }

    public int
    remove(int index) {
        return this.index.remove(index);
    }

    public int
    size() {
        return this.index.size();
    }

    @SuppressWarnings("unchecked")
    public Type[]
    read(Class<Type> type) {
        Type[] array = (Type[]) Array.newInstance(type, this.index.size());
        for (int i = 0; i < this.index.size(); ++i) {
            array[i] = this.get(i);
        }
        return array;
    }

    public void
    clear() {
        this.index.clear();
    }
}
