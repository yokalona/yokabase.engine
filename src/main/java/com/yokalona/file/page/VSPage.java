package com.yokalona.file.page;

import com.yokalona.array.serializers.VariableSizeSerializer;
import com.yokalona.file.exceptions.*;

public class VSPage<Type> implements Page<Type> {
    public static final int MAX_VS_PAGE_SIZE = 4 * 1024 * 1024;

    private static final long CRC = 5928236041702360388L;

    private int free;
    private final DataSpace<Type> dataSpace;
    private final Configuration configuration;
    private final VariableSizeSerializer<Type> serializer;
    private final MergeAvailabilitySpace availabilitySpace;

    public VSPage(VariableSizeSerializer<Type> serializer, Configuration configuration) {
        this.serializer = serializer;
        this.configuration = configuration;
        this.dataSpace = new DataSpace<>(serializer, new ASPage.Configuration(configuration.page, configuration.offset + configuration.availabilitySpace, configuration.dataSpace));
        this.availabilitySpace = new MergeAvailabilitySpace(new MergeAvailabilitySpace.Configuration(
                configuration.page, configuration.offset, configuration.availabilitySpace, configuration.page.length));
        this.free = configuration.dataSpace - dataSpace.occupied();
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
        int size = serializer.sizeOf(configuration.page, address);
        int next = serializer.sizeOf(value);
        if (size < next) throw new WriteOverflowException("New record is to large");
        if (next < size) availabilitySpace.free0(size - next, address + next);
        dataSpace.set(index, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public int
    append(Type value) {
        int size = serializer.sizeOf(value);
        if (free > size + dataSpace.pointerSize() && !availabilitySpace.fits(size)) availabilitySpace.defragmentation();
        else if (free < size + dataSpace.pointerSize()) throw new NoFreeSpaceLeftException();
        int address = availabilitySpace.alloc(size);
        if (address < 0) {
            defragmentation((Class<Type>) value.getClass());
            address = availabilitySpace.alloc(size);
            if (address < 0) throw new NullPointerException();
        }
        availabilitySpace.reduce(dataSpace.pointerSize());
        this.free -= (size + dataSpace.pointerSize());
        return dataSpace.insert(address, value);
    }

    @Override
    public int
    remove(int index) {
        int address = dataSpace.address(index);
        int size = serializer.sizeOf(configuration.page, address);
        this.availabilitySpace.free0(size, address);
        int count = dataSpace.remove(index);
        this.availabilitySpace.reduce(-dataSpace.pointerSize());
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
                && availabilitySpace.fits(size);
    }

    public void
    defragmentation(Class<Type> type) {
        Type[] array = dataSpace.read(type);
        dataSpace.clear();
        int max = availabilitySpace.maxAddress();
        for (Type data : array) dataSpace.insert(max = max - serializer.sizeOf(data), data);
        availabilitySpace.freeImmediately(max);
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
        return configuration.availabilitySpace + configuration.dataSpace - free;
    }

    public record Configuration(byte[] page, int offset, int availabilitySpace, int dataSpace) {
        public Configuration {
            if (offset < 0) throw new NegativeOffsetException();
            if (availabilitySpace < 128) throw new PageIsTooSmallException();
            if (dataSpace < 1024) throw new PageIsTooSmallException();
            int size = offset + availabilitySpace + dataSpace;
            if (page.length < size) throw new PageIsTooLargeException(size);
        }
    }
}
