package com.yokalona.file.page;

import com.yokalona.array.serializers.VariableSizeSerializer;
import com.yokalona.array.serializers.primitives.LongSerializer;
import com.yokalona.file.Cache;
import com.yokalona.file.CachedArrayProvider;
import com.yokalona.file.exceptions.NegativeOffsetException;
import com.yokalona.file.exceptions.NoFreeSpaceLeftException;
import com.yokalona.file.exceptions.PageIsTooLargeException;
import com.yokalona.file.exceptions.PageIsTooSmallException;
import com.yokalona.file.exceptions.WriteOverflowException;
import com.yokalona.file.headers.CRC64Jones;

public class VSPage<Type> implements Page<Type> {
    public static final int MAX_VS_PAGE_SIZE = 4 * 1024 * 1024;

    private static final long CRC = 5928236041702360388L;
    private static final long START = 6220403751627599921L;
    private static final int HEADER = Long.BYTES * 2;

    private int free;
    private final CachedDataSpace<Type> dataSpace;
    private final Configuration configuration;
    private final VariableSizeSerializer<Type> serializer;
    private final MergeAvailabilitySpace availabilitySpace;

    public VSPage(VariableSizeSerializer<Type> serializer, Configuration configuration) {
        this.serializer = serializer;
        this.configuration = configuration;
        this.dataSpace = new CachedDataSpace<>(serializer, new ASPage.Configuration(configuration.page, configuration.offset + configuration.availabilitySpace, configuration.dataSpace));
        this.availabilitySpace = new MergeAvailabilitySpace(new MergeAvailabilitySpace.Configuration(
                configuration.page, configuration.offset + HEADER, configuration.availabilitySpace, configuration.page.length));
        this.free = configuration.dataSpace - dataSpace.occupied();
        write(START, configuration.page, configuration.offset);
        write(CRC, configuration.page, configuration.offset + Long.BYTES);
    }

    @Override
    public Type
    get(int index) {
        return dataSpace.get(index);
    }

    public Cache<Type>
    read(Class<Type> type) {
        return dataSpace.read();
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

        availabilitySpace.reduce(dataSpace.pointerSize());
        Cache<Integer> read = dataSpace.addresses();
        // TODO: HOAR ALG, min([]) or use in memory Min Stack
        int min = Integer.MAX_VALUE;
        for (Integer address : read) min = Math.min(min, address);
        if (min <= availabilitySpace.beginning()) defragmentation((Class<Type>) value.getClass());

        int address = availabilitySpace.alloc(size);
        if (address < 0) {
            defragmentation((Class<Type>) value.getClass());
            address = availabilitySpace.alloc(size);
            if (address < 0) throw new NullPointerException("No free space of size " + size + " is available, only  " + free + " left");
        }

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
        long crc = CRC64Jones.calculate(configuration.page, configuration.offset + HEADER, configuration.offset + configuration.dataSpace + configuration.availabilitySpace);
        write(crc, configuration.page, configuration.offset + Long.BYTES);
        availabilitySpace.flush();
        dataSpace.flush();
    }

    public boolean
    fits(Type value) {
        int size = serializer.sizeOf(value);
        return fits(size);
    }

    public boolean
    fits(int size) {
        if (this.free < size + dataSpace.pointerSize()) return false;
        availabilitySpace.reduce(dataSpace.pointerSize());
        boolean fits = availabilitySpace.fits(size);
        availabilitySpace.reduce(-dataSpace.pointerSize());
        return fits;
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

    private static void
    write(long crc, byte[] page, int offset) {
        LongSerializer.INSTANCE.serializeCompact(crc, Long.BYTES, page, offset);
    }

    public record Configuration(byte[] page, int offset, int availabilitySpace, int dataSpace) {
        public Configuration {
            if (offset < 0) throw new NegativeOffsetException();
            if (availabilitySpace < 128) throw new PageIsTooSmallException();
            if (dataSpace < 1024) throw new PageIsTooSmallException();
            int size = offset + availabilitySpace + dataSpace;
//            if (page.length < size || size > MAX_VS_PAGE_SIZE) throw new PageIsTooLargeException(size);
        }
    }
}
