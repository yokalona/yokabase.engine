package com.yokalona.file.page;

import com.yokalona.annotations.TestOnly;
import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.array.serializers.primitives.CompactLongSerializer;
import com.yokalona.array.serializers.primitives.IntegerSerializer;
import com.yokalona.file.Array;
import com.yokalona.file.exceptions.NegativeOffsetException;
import com.yokalona.file.exceptions.NegativePageSizeException;
import com.yokalona.file.exceptions.PageIsTooLargeException;
import com.yokalona.file.exceptions.PageIsTooSmallException;
import com.yokalona.file.exceptions.ReadOverflowException;
import com.yokalona.file.exceptions.WriteOverflowException;
import com.yokalona.file.headers.Fixed;
import com.yokalona.file.headers.Header;

import java.util.Comparator;
import java.util.Iterator;

public class ASPage<Type> implements Iterable<Type>, ArrayPage<Type> {

    public static final int MAX_AS_PAGE_SIZE = 4 * 1024 * 1024;

    private static final long HEADLINE = 4707194276831113265L;

    private int size;
    private final Header[] headers;
    private final Configuration configuration;
    private final FixedSizeSerializer<Type> serializer;

    public ASPage(FixedSizeSerializer<Type> serializer, Configuration configuration, Header ... headers) {
        this(0, serializer, configuration, headers);
    }

    ASPage(int size, FixedSizeSerializer<Type> serializer, Configuration configuration, Header ... headers) {
        this.serializer = serializer;
        this.configuration = configuration;
        Header[] required = new Header[]{
                new Fixed<>(HEADLINE, new CompactLongSerializer(Long.BYTES)),
                new Fixed<>(configuration.length, new CompactIntegerSerializer(Integer.BYTES))};
        initHeaders(this.headers = join(required, headers));
        writeHeaders(configuration, this.headers);
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
        writeHeaders(configuration, headers);
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

    private void
    writeHeaders(Configuration configuration, Header[] headers) {
        int offset = configuration.offset();
        for (Header header : headers) {
            header.write(configuration().page, offset);
            offset += header.length();
        }
    }

    private int
    offset(int index) {
        return configuration.offset + headerOffset() + Short.BYTES + index * serializer.sizeOf();
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
        IntegerSerializer.INSTANCE.serializeCompact(value, Short.BYTES, configuration.page, configuration.offset + headerOffset());
        return value;
    }

    private int
    headerOffset() {
        int offset = 0;
        for (Header header : headers) offset += header.length();
        return offset;
    }

    private void
    initHeaders(Header[] headers) {
        int headerOffset = headerOffset();
        for (Header header : headers) header.offset(headerOffset);
    }

    private static int
    deserializeSize(byte[] page, int offset) {
        return IntegerSerializer.INSTANCE.deserializeCompact(Short.BYTES, page, offset);
    }

    public static <Type> ASPage<Type>
    read(FixedSizeSerializer<Type> serializer, byte[] page, int offset, Header ... headers) {
        Header headline = new Fixed<>(new CompactLongSerializer(Long.BYTES));
        Fixed<Integer> length = new Fixed<>(new CompactIntegerSerializer(Integer.BYTES));

        Header [] read = join(new Header[]{headline, length}, headers);

        int headerOffset = headerOffset(read);
        for (Header header : read) header.offset(headerOffset);
        readHeaders(page, offset, read);

        return new ASPage<>(deserializeSize(page, offset + headerOffset(read)),
                serializer, new Configuration(page, offset, length.value()), headers);
    }

    private static Header[]
    join(Header[] required, Header[] headers) {
        Header[] result = new Header[required.length + headers.length];
        System.arraycopy(required, 0, result, 0, required.length);
        System.arraycopy(headers, 0, result, required.length, headers.length);
        return result;
    }

    private static int
    headerOffset(Header[] headers) {
        int offset = 0;
        for (Header header : headers) offset += header.length();
        return offset;
    }

    private static void
    readHeaders(byte[] page, int offset, Header[] headers) {
        for (Header header : headers) {
            header.read(page, offset);
            offset += header.length();
        }
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
