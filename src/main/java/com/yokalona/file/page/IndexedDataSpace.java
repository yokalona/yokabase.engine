package com.yokalona.file.page;

import com.yokalona.annotations.PerformanceImpact;
import com.yokalona.array.serializers.Serializer;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.file.Array;

import static com.yokalona.file.AddressTools.significantBytes;

public class IndexedDataSpace<Type> implements DataSpace<Type> {

    public final Page<Integer> index;
    private final Serializer<Type> serializer;

    private IndexedDataSpace(Serializer<Type> serializer, Page<Integer> index) {
        this.serializer = serializer;
        this.index = new CachedPage<>(index);
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
        int significantBytes = significantBytes(offset + length);
        return new IndexedDataSpace<>(serializer, FSPage.Configurer.create(page, offset)
                .read(new CompactIntegerSerializer(significantBytes)));
    }

    public static class Configurer {
        private final FSPage.Configurer aspage;

        public Configurer(FSPage.Configurer aspage) {
            this.aspage = aspage;
        }

        public static Configurer
        create(FSPage.Configurer aspage) {
            return new Configurer(aspage);
        }

        public <Type> DataSpace<Type>
        dataspace(Serializer<Type> serializer) {
            return new IndexedDataSpace<>(serializer, aspage.fspage(new CompactIntegerSerializer(significantBytes(aspage.space()))));
        }

        public <Type> DataSpace<Type>
        read(Serializer<Type> serializer) {
            return new IndexedDataSpace<>(serializer, aspage.read(new CompactIntegerSerializer(significantBytes(aspage.space()))));
        }
    }

}
