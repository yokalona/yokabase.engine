package com.yokalona.file.page;

import com.yokalona.annotations.TestOnly;
import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.array.serializers.primitives.CompactLongSerializer;
import com.yokalona.file.Array;
import com.yokalona.file.exceptions.*;
import com.yokalona.array.serializers.VariableSizeSerializer;
import com.yokalona.file.headers.CRC;
import com.yokalona.file.headers.Fixed;
import com.yokalona.file.headers.Header;

import java.util.ArrayList;
import java.util.List;

public class VSPage<Type> implements Page<Type> {
    public static final int MAX_VS_PAGE_SIZE = 4 * 1024 * 1024;

    private static final long HEADLINE = 6220403751627599921L;

    private final DataSpace<Type> dataSpace;
    private final MASpace availabilitySpace;
    private final Configuration configuration;
    private final VariableSizeSerializer<Type> serializer;
    private final Header[] headers;

    private VSPage(VariableSizeSerializer<Type> serializer, DataSpace<Type> dataSpace,
                   MASpace availabilitySpace, Configuration configuration, Header[] headers) {
        this.headers = headers;
        this.serializer = serializer;
        this.configuration = configuration;
        this.availabilitySpace = availabilitySpace;
        this.dataSpace = new CachedDataSpace<>(dataSpace);
        Header.writeHeaders(this.headers, configuration.page, configuration.offset);
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
    public FSPage.Configuration
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

    public static class Configurer {

        private int dataSpace;
        private int addressSpace;
        private final int offset;
        private final byte[] page;
        private int availabilitySpace;
        private final List<Header> headers = new ArrayList<>();

        private Configurer(byte[] page, int offset, int addressSpace) {
            assert page != null;
            this.page = page;
            this.offset = offset;
            this.addressSpace = addressSpace;
        }


        @TestOnly
        public static Configurer
        create(int length) {
            return new Configurer(new byte[length], 0, length);
        }

        @TestOnly
        public static Configurer
        create(byte[] page) {
            return new Configurer(page, 0, page.length);
        }

        public static Configurer
        create(byte[] page, int offset, int addressSpace) {
            return new Configurer(page, offset, addressSpace);
        }

        public Configurer
        distribute(float factor) {
            this.availabilitySpace = (int) (addressSpace * factor);
            this.dataSpace = addressSpace - availabilitySpace;
            return this;
        }

        public Configurer
        addressSpace(int addressSpace) {
            this.addressSpace = addressSpace;
            return this;
        }

        public Configurer
        availabilitySpace(int availabilitySpace) {
            this.availabilitySpace = availabilitySpace;
            return this;
        }

        public Configurer
        dataSpace(int dataSpace) {
            this.dataSpace = dataSpace;
            return this;
        }

        public Configurer
        addHeader(Header header) {
            headers.add(header);
            return this;
        }

        public <Type> VSPage<Type>
        read(VariableSizeSerializer<Type> serializer, Header... headers) {
            Fixed<Long> headline = new Fixed<>(new CompactLongSerializer(Long.BYTES));
            Fixed<Integer> availabilitySpace = new Fixed<>(new CompactIntegerSerializer(Integer.BYTES));
            Fixed<Integer> dataSpace = new Fixed<>(new CompactIntegerSerializer(Integer.BYTES));
            CRC crc = new CRC();

            Header[] required = {headline, availabilitySpace, dataSpace, crc};

            Header[] read = Header.join(required, headers);

            int headerOffset = Header.initHeaders(read);
            Header.readHeaders(page, offset, read);

            MASpace availability = MASpace.read(page, offset + headerOffset, page.length);
            DataSpace<Type> data = new CachedDataSpace<>(IndexedDataSpace.read(serializer, dataSpace.value(), page, offset + availabilitySpace.value() + headerOffset));

            return new VSPage<>(serializer, data, availability,
                    new Configuration(page, offset, availabilitySpace.value(), dataSpace.value()), read);
        }

        public <Type> VSPage<Type>
        vspage(VariableSizeSerializer<Type> serializer) {
            if (offset < 0) throw new NegativeOffsetException();
            if (availabilitySpace < 32) throw new PageIsTooSmallException();
            if (dataSpace < 512) throw new PageIsTooSmallException();
            int size = offset + availabilitySpace + dataSpace;
            if (page.length < size || size > MAX_VS_PAGE_SIZE) throw new PageIsTooLargeException(size);

            Header[] required = new Header[]{
                    new Fixed<>(HEADLINE, new CompactLongSerializer(Long.BYTES)),
                    new Fixed<>(availabilitySpace, new CompactIntegerSerializer(Integer.BYTES)),
                    new Fixed<>(dataSpace, new CompactIntegerSerializer(Integer.BYTES)),
                    new CRC()};
            Header[] join = Header.join(required, headers.toArray(new Header[0]));
            Header.initHeaders(join);

            int headerOffset = Header.headerOffset(join);
            DataSpace<Type> dataspace = IndexedDataSpace.Configurer.create(FSPage.Configurer
                            .create(page, offset + availabilitySpace + headerOffset)
                            .length(dataSpace - headerOffset))
                    .dataspace(serializer);

            MASpace maspace = MASpace.Configurer
                    .create(page, offset + headerOffset)
                    .length(availabilitySpace)
                    .addressSpace(page.length)
                    .maspace(dataspace.occupied());
            return new VSPage<>(serializer, dataspace, maspace, new Configuration(page, offset, availabilitySpace, dataSpace), join);
        }

    }

    public record Configuration(byte[] page, int offset, int availabilitySpace, int dataSpace) {
    }
}
