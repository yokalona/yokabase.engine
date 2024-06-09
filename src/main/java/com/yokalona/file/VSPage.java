package com.yokalona.file;

import com.yokalona.array.serializers.VariableSizeSerializer;

public class VSPage<Type> implements Page {
    private int free;
    private final int total;
    private final byte[] space;
    private final DataSpace<Type> dataSpace;
    private final AvailabilitySpace pointers;
    private final VariableSizeSerializer<Type> serializer;

    private VSPage(VariableSizeSerializer<Type> serializer, byte[] space, int offset, int size, float delimiter) {
        this.space = space;
        this.serializer = serializer;
        int available = (int) (size * delimiter);
        this.total = size - available;
        this.pointers = new AvailabilitySpace(available, size, space, offset);
        this.dataSpace = new DataSpace<>(space, offset + available, this.total, serializer);
        this.free = this.total - dataSpace.pointerSize();
    }

    public Type
    get(int index) {
        return dataSpace.get(index);
    }

    public int
    append(Type value) {
        int size = serializer.sizeOf(value);
        if (free < size + dataSpace.pointerSize()) throw new NoFreeSpaceAvailableException();
        int address = pointers.alloc(size);
        if (address < 0) throw new NullPointerException();
        this.free -= (size + dataSpace.pointerSize());
        return dataSpace.insert(address, value);
    }

    public int
    remove(int index) {
        int address = dataSpace.address(index);
        int size = serializer.sizeOf(space, address);
        this.pointers.free(size, address);
        this.free += size + dataSpace.pointerSize();
        return dataSpace.remove(index);
    }

    public boolean
    fits(int size) {
        return this.free > size + dataSpace.pointerSize();
    }

    @Override
    public int size() {
        return dataSpace.size();
    }

    @Override
    public int free() {
        return free;
    }

    @Override
    public int occupied() {
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

        Configurer<Type>
        on(byte[] space, int offset) {
            this.space = space;
            this.offset = offset;
            return this;
        }

        Configurer<Type>
        ofSize(int size) {
            this.size = size;
            return this;
        }

        Configurer<Type>
        delimiter(float delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public VSPage<Type>
        configure() {
            if (size == 0) size = this.space.length - offset;
            if (0 > offset || offset > space.length) throw new IllegalArgumentException();
            if (offset + size > space.length) throw new IllegalArgumentException();
            if (delimiter < 0) throw new IllegalArgumentException();
            return new VSPage<>(serializer, space, offset, size, delimiter);
        }
    }
}
