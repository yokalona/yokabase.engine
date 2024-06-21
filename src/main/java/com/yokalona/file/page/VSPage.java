package com.yokalona.file.page;

import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.array.serializers.primitives.CompactLongSerializer;
import com.yokalona.file.Array;
import com.yokalona.file.exceptions.*;
import com.yokalona.array.serializers.VariableSizeSerializer;
import com.yokalona.file.headers.CRC;
import com.yokalona.file.headers.Fixed;
import com.yokalona.file.headers.Header;

public class VSPage<Type> implements Page<Type> {
    public static final int MAX_VS_PAGE_SIZE = 4 * 1024 * 1024;

    private static final long HEADLINE = 6220403751627599921L;

    private final DataSpace<Type> dataSpace;
    private final MASpace availabilitySpace;
    private final Configuration configuration;
    private final VariableSizeSerializer<Type> serializer;
    private final Header[] headers;

    public VSPage(VariableSizeSerializer<Type> serializer, Configuration configuration, Header... headers) {
        Header[] required = new Header[]{
                new Fixed<>(HEADLINE, new CompactLongSerializer(Long.BYTES)),
                new Fixed<>(configuration.availabilitySpace, new CompactIntegerSerializer(Integer.BYTES)),
                new Fixed<>(configuration.dataSpace, new CompactIntegerSerializer(Integer.BYTES)),
                new CRC()};
        Header.initHeaders(this.headers = Header.join(required, headers));

        this.serializer = serializer;
        this.configuration = configuration;
        this.dataSpace = IndexedDataSpace.Configurer.create(ASPage.Configurer.create(configuration.page, configuration.offset + configuration.availabilitySpace + Header.headerOffset(this.headers))
                        .length(configuration.dataSpace - Header.headerOffset(this.headers)))
                .dataspace(serializer);
        this.availabilitySpace = new MASpace(new MASpace.Configuration(
                configuration.page, configuration.offset + Header.headerOffset(this.headers), configuration.availabilitySpace, configuration.page.length));
    }

    private VSPage(VariableSizeSerializer<Type> serializer, DataSpace<Type> dataSpace,
                   MASpace availabilitySpace, Configuration configuration, Header[] headers) {
        this.headers = headers;
        this.serializer = serializer;
        this.configuration = configuration;
        this.availabilitySpace = availabilitySpace;
        this.dataSpace = new CachedDataSpace<>(dataSpace);
        Header.writeHeaders(this.headers, configuration.page, configuration.offset);
    }

    public static <Type> VSPage<Type>
    read(VariableSizeSerializer<Type> serializer, byte[] page, int offset, Header... headers) {
        Fixed<Long> headline = new Fixed<>(new CompactLongSerializer(Long.BYTES));
        Fixed<Integer> availabilitySpace = new Fixed<>(new CompactIntegerSerializer(Integer.BYTES));
        Fixed<Integer> dataSpace = new Fixed<>(new CompactIntegerSerializer(Integer.BYTES));
        CRC crc = new CRC();

        Header[] read = Header.join(new Header[]{headline, availabilitySpace, dataSpace, crc}, headers);

        int headerOffset = Header.initHeaders(read);
        Header.readHeaders(page, offset, read);

        MASpace availability = MASpace.read(page, offset + headerOffset, page.length);
        DataSpace<Type> data = new CachedDataSpace<>(IndexedDataSpace.read(serializer, dataSpace.value(), page, offset + availabilitySpace.value() + headerOffset));

        return new VSPage<>(serializer, data, availability,
                new Configuration(page, offset, availabilitySpace.value(), dataSpace.value()), read);
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
        if (availabilitySpace.available() > size + dataSpace.pointerSize() && !availabilitySpace.fits(size))
            availabilitySpace.defragmentation();
        else if (availabilitySpace.available() < size + dataSpace.pointerSize()) throw new NoFreeSpaceLeftException();

        if (!availabilitySpace.reduce(dataSpace.pointerSize())) {
            defragmentation((Class<Type>) value.getClass());
            if (!availabilitySpace.reduce(dataSpace.pointerSize()))
                throw new NoFreeSpaceLeftException();
        }

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
                throw new NoFreeSpaceLeftException("No free space of size " + size + " is available, only  " + availabilitySpace.available() + " left");
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
        Header.writeHeaders(this.headers, configuration.page, configuration.offset);
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
        return availabilitySpace.fits(size + dataSpace.pointerSize());
    }

    public void
    defragmentation(Class<Type> type) {
        Array<Type> array = dataSpace.read(type);
        dataSpace.clear();
        int max = availabilitySpace.maxAddress();
        for (Type data : array) dataSpace.insert(max -= serializer.sizeOf(data), data);
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

    public record Configuration(byte[] page, int offset, int availabilitySpace, int dataSpace) {

        public Configuration(byte[] page, int offset, float distribution) {
            this(page, offset, (int) (page.length * distribution), page.length - (int) (page.length * distribution));
        }

        public Configuration {
            if (offset < 0) throw new NegativeOffsetException();
            if (availabilitySpace < 32) throw new PageIsTooSmallException();
            if (dataSpace < 512) throw new PageIsTooSmallException();
            int size = offset + availabilitySpace + dataSpace;
            if (page.length < size || size > MAX_VS_PAGE_SIZE) throw new PageIsTooLargeException(size);
        }
    }
}
