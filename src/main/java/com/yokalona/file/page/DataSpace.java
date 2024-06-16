package com.yokalona.file.page;

import com.yokalona.annotations.PerformanceImpact;
import com.yokalona.array.serializers.Serializer;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.file.AddressTools;

import java.lang.reflect.Array;

public class DataSpace<Type> {

    private final byte significantBytes;
    private final ASPage<Integer> index;
    private final Serializer<Type> serializer;
    private final ASPage.Configuration configuration;

    public DataSpace(Serializer<Type> serializer, ASPage.Configuration configuration) {
        this.serializer = serializer;
        this.configuration = configuration;
        this.significantBytes = AddressTools.significantBytes(configuration.offset() + configuration.length());
        this.index = new ASPage<>(new CompactIntegerSerializer(significantBytes), configuration);
    }

    public byte
    pointerSize() {
        return significantBytes;
    }

    public Type
    get(int index) {
        Integer address = this.index.get(index);
        return serializer.deserialize(configuration.page(), address);
    }

    public int
    address(int index) {
        return this.index.get(index);
    }

    public void
    set(int index, Type value) {
        Integer address = this.index.get(index);
        serializer.serialize(value, configuration.page(), address);
    }

    public int
    insert(int address, Type value) {
        serializer.serialize(value, configuration.page(), address);
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

    @PerformanceImpact
    @SuppressWarnings("unchecked")
    public Type[]
    read(Class<Type> type) {
        Type[] array = (Type[]) Array.newInstance(type, this.index.size());
        for (int i = 0; i < this.index.size(); ++i) array[i] = this.get(i);
        return array;
    }

    public void
    clear() {
        this.index.clear();
    }

    public int
    occupied() {
        return this.index.occupied();
    }
}
