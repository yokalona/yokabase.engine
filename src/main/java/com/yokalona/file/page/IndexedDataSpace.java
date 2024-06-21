package com.yokalona.file.page;

import com.yokalona.annotations.PerformanceImpact;
import com.yokalona.array.serializers.Serializer;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.array.serializers.primitives.IntegerSerializer;
import com.yokalona.file.Array;
import com.yokalona.file.headers.CRC;
import com.yokalona.file.headers.Header;

import static com.yokalona.file.AddressTools.significantBytes;

public class IndexedDataSpace<Type> implements DataSpace<Type> {

    public final Page<Integer> index;
    private final Serializer<Type> serializer;

    public IndexedDataSpace(Serializer<Type> serializer, ASPage.Configuration configuration) {
        this(serializer, new ASPage<>(
                new CompactIntegerSerializer(significantBytes(configuration.offset() + configuration.length())),
                configuration));
    }

    IndexedDataSpace(Serializer<Type> serializer, ASPage<Integer> index) {
        this.index = new CachedPage<>(index);
        this.serializer = serializer;
    }

    @Override
    public byte
    pointerSize() {
        return (byte) index.serializer().sizeOf();
    }

    @Override
    public Type
    get(int index) {
        Integer address = this.index.get(index);
        return serializer.deserialize(this.index.configuration().page(), address);
    }

    @Override
    public int
    address(int index) {
        return this.index.get(index);
    }

    @Override
    public void
    set(int index, Type value) {
        Integer address = this.index.get(index);
        serializer.serialize(value, this.index.configuration().page(), address);
    }

    @Override
    public int
    insert(int address, Type value) {
        serializer.serialize(value, this.index.configuration().page(), address);
        return this.index.append(address);
    }

    @Override
    public int
    remove(int index) {
        return this.index.remove(index);
    }

    @Override
    public int
    size() {
        return this.index.size();
    }

    @Override
    @PerformanceImpact
    @SuppressWarnings("unchecked")
    public Array<Type>
    read(Class<Type> type) {
        Type[] array = (Type[]) java.lang.reflect.Array.newInstance(type, this.index.size());
        for (int i = 0; i < this.index.size(); ++i) array[i] = this.get(i);
        return new Array.Indexed<>(array);
    }

    @Override
    public Array<Integer>
    addresses() {
        return this.index.read(Integer.class);
    }

    @Override
    public void
    clear() {
        this.index.clear();
    }

    @Override
    public int
    occupied() {
        return this.index.occupied();
    }

    @Override
    public void
    flush() {
        this.index.flush();
    }

    public static <Type> IndexedDataSpace<Type>
    read(Serializer<Type> serializer, int length, byte[] page, int offset) {
//        int length = IntegerSerializer.INSTANCE.deserializeCompact(page, offset + Long.BYTES);
        int significantBytes = significantBytes(offset + length);
        return new IndexedDataSpace<>(serializer,
                ASPage.read(new CompactIntegerSerializer(significantBytes), page, offset));
    }

}
