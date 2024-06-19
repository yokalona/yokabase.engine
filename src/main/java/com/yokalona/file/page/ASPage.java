package com.yokalona.file.page;

import com.yokalona.annotations.PerformanceImpact;
import com.yokalona.annotations.TestOnly;
import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.primitives.IntegerSerializer;
import com.yokalona.array.serializers.primitives.LongSerializer;
import com.yokalona.file.Array;
import com.yokalona.file.exceptions.CRCMismatchException;
import com.yokalona.file.exceptions.NegativeOffsetException;
import com.yokalona.file.exceptions.NegativePageSizeException;
import com.yokalona.file.exceptions.PageIsTooLargeException;
import com.yokalona.file.exceptions.PageIsTooSmallException;
import com.yokalona.file.exceptions.ReadOverflowException;
import com.yokalona.file.exceptions.WriteOverflowException;

import java.util.Comparator;
import java.util.Iterator;

import static com.yokalona.file.headers.CRC64Jones.calculate;

public class ASPage<Type> implements Iterable<Type>, ArrayPage<Type> {

    public static final int MAX_AS_PAGE_SIZE = 4 * 1024 * 1024;

    private static final long CRC = 5928236041702360388L;
    private static final long START = 4707194276831113265L;
    private static final int HEADER = Long.BYTES * 2 + Integer.BYTES;

    private int size;
    private final Configuration configuration;
    private final FixedSizeSerializer<Type> serializer;

    public ASPage(FixedSizeSerializer<Type> serializer, Configuration configuration) {
        this(0, serializer, configuration);
    }

    ASPage(int size, FixedSizeSerializer<Type> serializer, Configuration configuration) {
        this.serializer = serializer;
        this.configuration = configuration;
        int offset = write(START, configuration, configuration.offset);
        offset += write(configuration.length, configuration, configuration.offset + offset);
        write(CRC, configuration, configuration.offset + offset);
        serializeSize(this.size = size);
    }

    @Override
    public synchronized Type
    get(int index) {
        if (outbound(index)) throw new ReadOverflowException(size, index);
        int offset = offset(index);
        return deserialize(offset);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized Array<Type>
    read(Class<Type> type) {
        Type[] array = (Type[]) java.lang.reflect.Array.newInstance(type, size);
        for (int i = 0; i < size; ++i) array[i] = this.get(i);
        return new Array.Indexed<>(array);
    }

    @Override
    public synchronized void
    set(int index, Type value) {
        if (outbound(index)) throw new WriteOverflowException(size, index);
        int offset = offset(index);
        serialize(value, offset);
    }

    @Override
    public synchronized void
    insert(int index, Type value) {
        if (0 > index || index > size || spills()) throw new WriteOverflowException(size, index);
        int offset = offset(index);
        System.arraycopy(configuration.page, offset, configuration.page, offset(index + 1), (size - index) * serializer.sizeOf());
        serialize(value, offset);
        serializeSize(++size);
    }

    @Override
    public synchronized int
    append(Type value) {
        if (spills()) throw new WriteOverflowException(free());
        int offset = offset(size);
        serialize(value, offset);
        return serializeSize(++size);
    }

    @Override
    public synchronized void
    swap(int left, int right) {
        if (outbound(left)) throw new WriteOverflowException(size, left);
        if (outbound(right)) throw new WriteOverflowException(size, right);
        Type temp = get(left);
        set(left, get(right));
        set(right, temp);
    }

    @Override
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
        return serializeSize(--size);
    }

    public synchronized void
    clear() {
        serializeSize(this.size = 0);
    }

    @Override
    public synchronized Type
    first() {
        if (outbound(0)) throw new ReadOverflowException(size, 0);
        int offset = offset(0);
        return deserialize(offset);
    }

    @Override
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

    @Override
    public int
    length() {
        return configuration.length;
    }

    @Override
    public int
    occupied() {
        return (offset(0) - configuration.offset) + size * serializer.sizeOf();
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
                if (current + 1 < size()) {
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

    public Configuration
    configuration() {
        return configuration;
    }

    public FixedSizeSerializer<Type>
    serializer() {
        return serializer;
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
        IntegerSerializer.INSTANCE.serializeCompact(value, Short.BYTES, configuration.page, configuration.offset + HEADER);
        return value;
    }

    @PerformanceImpact
    private void
    serializeCRC() {
        long crc = calculate(configuration.page, configuration.offset + HEADER, configuration.length - HEADER);
        // TODO: fix
        write(crc, configuration, configuration.offset + Long.BYTES + Integer.BYTES);
    }

    private static int
    deserializeSize(byte[] page, int offset) {
        return IntegerSerializer.INSTANCE.deserializeCompact(Short.BYTES, page, offset);
    }

    private static int
    write(int value, Configuration configuration, int offset) {
        return IntegerSerializer.INSTANCE.serializeCompact(value, Integer.BYTES, configuration.page, offset);
    }

    private static int
    write(long value, Configuration configuration, int offset) {
        return LongSerializer.INSTANCE.serializeCompact(value, Long.BYTES, configuration.page, offset);
    }

    public static <Type> ASPage<Type>
    read(FixedSizeSerializer<Type> serializer, byte[] page, int offset) {
        int length = IntegerSerializer.INSTANCE.deserializeCompact(page, offset + Long.BYTES);
        long expected = calculate(page, offset + HEADER, length - HEADER);
        long actual = LongSerializer.INSTANCE.deserializeCompact(page, offset + Long.BYTES + Integer.BYTES);
        if (expected != actual) throw new CRCMismatchException();
        return new ASPage<>(deserializeSize(page, offset + HEADER), serializer, new Configuration(page, offset, length));
    }

    public record Configuration(byte[] page, int offset, int length) {

        @TestOnly
        public Configuration(int length) {
            this(new byte[length], 0, length);
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
