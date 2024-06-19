package com.yokalona.file.page;

import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.primitives.IntegerSerializer;
import com.yokalona.file.Array;
import com.yokalona.file.exceptions.*;
import com.yokalona.file.headers.CRC64Jones;
import com.yokalona.array.serializers.VariableSizeSerializer;
import com.yokalona.array.serializers.primitives.LongSerializer;

public class VSPage<Type> implements Page<Type> {
    public static final int MAX_VS_PAGE_SIZE = 4 * 1024 * 1024;

    private static final long CRC = 5928236041702360388L;
    private static final long START = 6220403751627599921L;
    private static final int HEADER = Long.BYTES * 2 + Integer.BYTES * 2;

    private final Configuration configuration;
    private final DataSpace<Type> dataSpace;
    private final VariableSizeSerializer<Type> serializer;
    private final MergeAvailabilitySpace availabilitySpace;

    public VSPage(VariableSizeSerializer<Type> serializer, Configuration configuration) {
        this.serializer = serializer;
        this.configuration = configuration;
        this.dataSpace = new CachedDataSpace<>(
                new IndexedDataSpace<>(serializer,
                        new ASPage.Configuration(configuration.page, configuration.offset + configuration.availabilitySpace, configuration.dataSpace)));
        this.availabilitySpace = new MergeAvailabilitySpace(new MergeAvailabilitySpace.Configuration(
                // TODO: fix configuration.page.length as it might be wrong
                configuration.page, configuration.offset + HEADER, configuration.availabilitySpace, configuration.page.length));
        int offset = write(START, configuration.page, configuration.offset);
        offset += write(configuration.availabilitySpace, configuration.page, configuration.offset + offset);
        offset += write(configuration.dataSpace, configuration.page, configuration.offset + offset);
        write(CRC, configuration.page, configuration.offset + offset);
    }

    private VSPage(VariableSizeSerializer<Type> serializer, CachedDataSpace<Type> dataSpace,
                   MergeAvailabilitySpace availabilitySpace, Configuration configuration) {
        this.dataSpace = dataSpace;
        this.serializer = serializer;
        this.configuration = configuration;
        this.availabilitySpace = availabilitySpace;
    }

    public static <Type> VSPage<Type>
    read(VariableSizeSerializer<Type> serializer, byte[] page, int offset) {
        int availability = IntegerSerializer.INSTANCE.deserializeCompact(page, offset + Long.BYTES);
        int data = IntegerSerializer.INSTANCE.deserializeCompact(page, offset + Long.BYTES + Integer.BYTES);
        MergeAvailabilitySpace availabilitySpace = MergeAvailabilitySpace.read(page, offset + HEADER, page.length);
        CachedDataSpace<Type> dataSpace = new CachedDataSpace<>(IndexedDataSpace.read(serializer, page, offset + availability));
        return new VSPage<>(serializer, dataSpace, availabilitySpace, new Configuration(page, offset, availability, data));
    }

    @Override
    public Type
    get(int index) {
        return dataSpace.get(index);
    }

    public Array<Type>
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
        if (availabilitySpace.available() > size + dataSpace.pointerSize() && !availabilitySpace.fits(size)) availabilitySpace.defragmentation();
        else if (availabilitySpace.available() < size + dataSpace.pointerSize()) throw new NoFreeSpaceLeftException();

        availabilitySpace.reduce(dataSpace.pointerSize());
        Array<Integer> read = dataSpace.addresses();
        // TODO: HOAR ALG or use in memory Min Stack
        int min = Integer.MAX_VALUE;
        for (Integer address : read) min = Math.min(min, address);
        if (min <= availabilitySpace.beginning()) defragmentation((Class<Type>) value.getClass());

        int address = availabilitySpace.alloc(size);
        if (address < 0) {
            defragmentation((Class<Type>) value.getClass());
            address = availabilitySpace.alloc(size);
            if (address < 0)
                throw new NullPointerException("No free space of size " + size + " is available, only  " + availabilitySpace.available() + " left");
        }

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
        return count;
    }

    @Override
    public void flush() {
        long crc = CRC64Jones.calculate(configuration.page, configuration.offset + HEADER, configuration.offset + configuration.dataSpace + configuration.availabilitySpace);
        write(crc, configuration.page, configuration.offset + Long.BYTES + Integer.BYTES * 2);
        availabilitySpace.flush();
        dataSpace.flush();
    }

    @Override
    public FixedSizeSerializer<Type>
    serializer() {
        return null;
    }

    @Override
    public ASPage.Configuration
    configuration() {
        return null;
    }

    @Override
    public Array<Type>
    read() {
        return null;
    }

    @Override
    public void
    clear() {

    }

    public boolean
    fits(Type value) {
        int size = serializer.sizeOf(value);
        return fits(size);
    }

    public boolean
    fits(int size) {
        if (availabilitySpace.available() < size + dataSpace.pointerSize()) return false;
        availabilitySpace.reduce(dataSpace.pointerSize());
        boolean fits = availabilitySpace.fits(size);
        availabilitySpace.reduce(-dataSpace.pointerSize());
        return fits;
    }

    public void
    defragmentation(Class<Type> type) {
        Array<Type> array = dataSpace.read(type);
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
        return availabilitySpace.available();
    }

    @Override
    public int
    occupied() {
        return configuration.availabilitySpace + configuration.dataSpace - availabilitySpace.available();
    }

    private static int
    write(int value, byte[] page, int offset) {
        return IntegerSerializer.INSTANCE.serializeCompact(value, Integer.BYTES, page, offset);
    }

    private static int
    write(long value, byte[] page, int offset) {
        return LongSerializer.INSTANCE.serializeCompact(value, Long.BYTES, page, offset);
    }

    public record Configuration(byte[] page, int offset, int availabilitySpace, int dataSpace) {

        public Configuration(byte[] page, int offset, float distribution) {
            this(page, offset, (int) (page.length * distribution), page.length - (int) (page.length * distribution));
        }

        public Configuration {
            if (offset < 0) throw new NegativeOffsetException();
            if (availabilitySpace < 128) throw new PageIsTooSmallException();
            if (dataSpace < 1024) throw new PageIsTooSmallException();
            int size = offset + availabilitySpace + dataSpace;
            if (page.length < size || size > MAX_VS_PAGE_SIZE) throw new PageIsTooLargeException(size);
        }
    }
}
