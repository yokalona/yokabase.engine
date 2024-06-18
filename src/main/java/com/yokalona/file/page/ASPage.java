package com.yokalona.file.page;

import com.yokalona.annotations.PerformanceImpact;
import com.yokalona.annotations.TestOnly;
import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.primitives.IntegerSerializer;
import com.yokalona.array.serializers.primitives.LongSerializer;
import com.yokalona.file.Cache;
import com.yokalona.file.CachedArrayProvider;
import com.yokalona.file.exceptions.CRCMismatchException;
import com.yokalona.file.exceptions.NegativeOffsetException;
import com.yokalona.file.exceptions.NegativePageSizeException;
import com.yokalona.file.exceptions.PageIsTooLargeException;
import com.yokalona.file.exceptions.PageIsTooSmallException;
import com.yokalona.file.exceptions.ReadOverflowException;
import com.yokalona.file.exceptions.WriteOverflowException;
import com.yokalona.file.headers.CRC64Jones;

import java.util.Comparator;
import java.util.Iterator;

public class ASPage<Type> implements Page<Type>, Iterable<Type> {

    public static final int MAX_AS_PAGE_SIZE = 4 * 1024 * 1024;

    public static final int MAX_CACHE_SIZE = MAX_AS_PAGE_SIZE;

    private static final long CRC = 5928236041702360388L;
    private static final long START = 4707194276831113265L;
    private static final int HEADER = Long.BYTES * 2;

    private int size;
    private final Configuration configuration;
    private final FixedSizeSerializer<Type> serializer;

    private final CachedArrayProvider<Type> array;

    public ASPage(FixedSizeSerializer<Type> serializer, Configuration configuration) {
        this(0, serializer, configuration);
    }

    private ASPage(int size, FixedSizeSerializer<Type> serializer, Configuration configuration) {
        this.serializer = serializer;
        this.configuration = configuration;
        this.array = new CachedArrayProvider<>(Math.min(MAX_CACHE_SIZE, free()) / serializer.sizeOf(), this::read);
        write(START, configuration, configuration.offset);
        write(CRC, configuration, configuration.offset + Long.BYTES);
        serializeSize(this.size = size);
    }

    @Override
    public synchronized Type
    get(int index) {
        if (outbound(index)) throw new ReadOverflowException(size, index);
        return array.get(index);
    }

    Type
    read(int index) {
        int offset = offset(index);
        return deserialize(offset);
    }

    public synchronized Cache<Type>
    read() {
        return array;
    }

    public synchronized void
    set(int index, Type value) {
        if (outbound(index)) throw new WriteOverflowException(size, index);
        int offset = offset(index);
        serialize(value, offset);
        array.invalidate(index);
    }

    public synchronized void
    insert(int index, Type value) {
        if (0 > index || index > size || spills()) throw new WriteOverflowException(size, index);
        int offset = offset(index);
        System.arraycopy(configuration.page, offset, configuration.page, offset(index + 1), (size - index) * serializer.sizeOf());
        serialize(value, offset);
        serializeSize(++size);
        array.adjust(index, +1);
    }

    @Override
    public synchronized int
    append(Type value) {
        if (spills()) throw new WriteOverflowException(free());
        int offset = offset(size);
        serialize(value, offset);
        array.invalidate(size);
        return serializeSize(++size);
    }

    public synchronized void
    swap(int left, int right) {
        if (outbound(left)) throw new WriteOverflowException(size, left);
        if (outbound(right)) throw new WriteOverflowException(size, right);
        Type temp = get(left);
        set(left, get(right));
        set(right, temp);
    }

    public synchronized int
    find(Type value, Comparator<Type> comparator) {
        int left = 0, right = size - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            int compare = comparator.compare(get(mid), value);
            if (compare > 0) right = mid - 1;
            else if (compare < 0) left = mid + 1;
            else return mid;
        }
        return -(left + 1);
    }

    @Override
    public synchronized int
    remove(int index) {
        if (outbound(index)) throw new WriteOverflowException(size, index);
        System.arraycopy(configuration.page, offset(index + 1), configuration.page, offset(index), (size - index - 1) * serializer.sizeOf());
        array.adjust(index, -1);
        return serializeSize(--size);
    }

    public synchronized void
    clear() {
        serializeSize(this.size = 0);
    }

    public synchronized Type
    first() {
        if (outbound(0)) throw new ReadOverflowException(size, 0);
        int offset = offset(0);
        return deserialize(offset);
    }

    public synchronized Type
    last() {
        if (outbound(size - 1)) throw new ReadOverflowException(size, size - 1);
        int offset = offset(size - 1);
        return deserialize(offset);
    }

    @Override
    public int
    size() {
        return size;
    }

    @Override
    public synchronized int
    free() {
        return configuration.length - occupied();
    }

    public int
    length() {
        return configuration.length;
    }

    @Override
    public int
    occupied() {
        return (offset(0) - configuration.offset) + size * serializer.sizeOf();
    }

    public int
    offset() {
        return configuration.offset;
    }

    private Type
    deserialize(int offset) {
        return serializer.deserialize(configuration.page, offset);
    }

    @Override
    public Iterator<Type>
    iterator() {
        return new Iterator<>() {
            private int current = -1;
            private boolean switched = false;

            @Override
            public boolean hasNext() {
                switched = true;
                if (current + 1 < size) {
                    current++;
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public Type next() {
                return get(current);
            }

            @Override
            public void remove() {
                if (!switched) throw new IllegalStateException();
                ASPage.this.remove(current--);
                switched = false;
            }
        };
    }

    @Override
    public void
    flush() {
        serializeCRC();
    }

    public boolean
    spills() {
        return free() < serializer.sizeOf();
    }

    private int
    offset(int index) {
        return configuration.offset + HEADER + Short.BYTES + index * serializer.sizeOf();
    }

    private boolean
    outbound(int index) {
        return 0 > index || index >= size;
    }

    private void
    serialize(Type value, int offset) {
        serializer.serialize(value, configuration.page, offset);
    }

    private int
    serializeSize(int value) {
        array.length(value);
        IntegerSerializer.INSTANCE.serializeCompact(value, Short.BYTES, configuration.page, configuration.offset + HEADER);
        return value;
    }

    @PerformanceImpact
    private void
    serializeCRC() {
        long crc = calculateCRC(configuration.page, configuration.offset, configuration.length);
        write(crc, configuration, configuration.offset + Long.BYTES);
    }

    private static long
    calculateCRC(byte[] page, int offset, int length) {
        return CRC64Jones.calculate(page, offset + HEADER, length - HEADER);
    }

    public static <Type> ASPage<Type>
    read(FixedSizeSerializer<Type> serializer, Configuration configuration) {
        long expected = calculateCRC(configuration.page, configuration.offset, configuration.length);
        long actual = LongSerializer.INSTANCE.deserializeCompact(configuration.page, configuration.offset + Long.BYTES);
        if (expected != actual) throw new CRCMismatchException();
        return new ASPage<>(deserializeSize(configuration.page, configuration.offset + HEADER), serializer, configuration);
    }

    private static int
    deserializeSize(byte[] page, int offset) {
        return IntegerSerializer.INSTANCE.deserializeCompact(Short.BYTES, page, offset);
    }

    private static void
    write(long crc, Configuration configuration, int offset) {
        LongSerializer.INSTANCE.serializeCompact(crc, Long.BYTES, configuration.page, offset);
    }

    public record Configuration(byte[] page, int offset, int length) {

        @TestOnly
        public Configuration(int lengthKb) {
            this(new byte[lengthKb * 1024], 0, lengthKb * 1024);
        }

        public Configuration(byte[] page) {
            this(page, 0, page.length);
        }

        public Configuration {
            if (offset < 0) throw new NegativeOffsetException();
            if (length == 0) throw new PageIsTooSmallException();
            if (length < 0) throw new NegativePageSizeException();
            if (offset + length > page.length || length > MAX_AS_PAGE_SIZE) throw new PageIsTooLargeException(length);
        }
    }
}
