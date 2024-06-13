package com.yokalona.file.page;

import com.yokalona.array.serializers.VariableSizeSerializer;
import com.yokalona.file.exceptions.NoFreeSpaceAvailableException;
import com.yokalona.file.exceptions.WriteOverflowException;
import com.yokalona.file.headers.CRC64Jones;

public class VSPage<Type> implements Page<Type> {
    private int free;
    private final int total;
    private final byte[] space;
    private final DataSpace<Type> dataSpace;
    private final MergeAvailabilitySpace pointers;
    private final VariableSizeSerializer<Type> serializer;
    private final CRC64Jones crc64JonesHeader = new CRC64Jones();

    public VSPage(VariableSizeSerializer<Type> serializer, byte[] space, int offset, int size, float delimiter) {
        this.space = space;
        this.serializer = serializer;
        int available = (int) (size * delimiter);
        this.total = size - available;
        this.pointers = new MergeAvailabilitySpace(available, size, space, 8 + offset);
        this.dataSpace = new DataSpace<>(space, offset + available, this.total, serializer);
        this.free = this.total - dataSpace.pointerSize();
    }

    @Override
    public Type
    get(int index) {
        return dataSpace.get(index);
    }

    public Type[]
    read(Class<Type> type) {
        return dataSpace.read(type);
    }

    public void
    set(int index, Type value) {
        int address = dataSpace.address(index);
        int size = serializer.sizeOf(dataSpace.pointerSize(), space, address);
        int next = serializer.sizeOf(value);
        if (size < next) throw new WriteOverflowException("New record is to large");
        if (next < size) pointers.free0(size - next, address + next);
        dataSpace.set(index, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public int
    append(Type value) {
        int size = serializer.sizeOf(value);
        if (free < size + dataSpace.pointerSize()) throw new NoFreeSpaceAvailableException();
        int address = pointers.alloc(size);
        if (address < 0) {
            defragmentation((Class<Type>) value.getClass());
            address = pointers.alloc(size);
            if (address < 0) throw new NullPointerException();
        }
        pointers.reduce(dataSpace.pointerSize());
        this.free -= (size + dataSpace.pointerSize());
        return dataSpace.insert(address, value);
    }

    @Override
    public int
    remove(int index) {
        int address = dataSpace.address(index);
        int size = serializer.sizeOf(dataSpace.pointerSize(), space, address);
        this.pointers.free0(size, address);
        int count = dataSpace.remove(index);
        this.pointers.reduce(-dataSpace.pointerSize());
        this.free += size + dataSpace.pointerSize();
        return count;
    }

    @Override
    public void flush() {

    }

    public boolean
    fits(Type value) {
        int size = serializer.sizeOf(value);
        return fits(size);
    }

    public boolean
    fits(int size) {
        return this.free > size + dataSpace.pointerSize()
                && pointers.fits(size);
    }

    public void
    defragmentation(Class<Type> type) {
        Type[] array = dataSpace.read(type);
        dataSpace.clear();
        int max = pointers.maxAddress();
        for (Type data : array) dataSpace.insert(max = max - serializer.sizeOf(data), data);
        pointers.free(max);
    }

    @Override
    public int
    size() {
        return dataSpace.size();
    }

    @Override
    public int
    free() {
        return free;
    }

    @Override
    public int
    occupied() {
        return total - free;
    }

    public static class Configurer<Type> {
        private int size;
        private int offset;
        private byte[] space;
        private float delimiter = .1f;
        private final VariableSizeSerializer<Type> serializer;

        public Configurer(VariableSizeSerializer<Type> serializer) {
            this.serializer = serializer;
        }

        public Configurer<Type>
        on(byte[] space, int offset) {
            this.space = space;
            this.offset = offset;
            return this;
        }

        public Configurer<Type>
        ofSize(int size) {
            this.size = size;
            return this;
        }

        public Configurer<Type>
        delimiter(float delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public VSPage<Type>
        configure() {
            if (space == null && offset == 0) space = new byte[size];
            if (space == null) throw new NullPointerException();
            if (size == 0) size = space.length - offset;
            if (0 > offset || offset > space.length) throw new IllegalArgumentException();
            if (offset + size > space.length) throw new IllegalArgumentException();
            if (delimiter < 0) throw new IllegalArgumentException();
            return new VSPage<>(serializer, space, offset, size, delimiter);
        }
    }
}
