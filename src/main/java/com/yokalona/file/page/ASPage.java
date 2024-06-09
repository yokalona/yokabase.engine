package com.yokalona.file.page;

import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.file.Header;
import com.yokalona.file.exceptions.NegativePageSizeException;
import com.yokalona.file.exceptions.PageIsToLargeException;
import com.yokalona.file.exceptions.ReadOverflowException;
import com.yokalona.file.exceptions.WriteOverflowException;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.Iterator;

public class ASPage<Type> implements Page<Type>, Iterable<Type> {

    private int size = 0;
    private final int space;
    private Header[] headers;
    private final int offset;
    private final byte[] page;
    private final FixedSizeSerializer<Type> serializer;

    private ASPage(int space, int offset, byte[] page, FixedSizeSerializer<Type> serializer) {
        this.page = page;
        this.space = space;
        this.offset = offset;
        this.serializer = serializer;
    }

    public static <Type> ASPage<Type>
    create(int size, FixedSizeSerializer<Type> serializer) {
        if (size <= 0) throw new NegativePageSizeException();
        else if (size > 128) throw new PageIsToLargeException(size * 1024);
        return create(size * 1024, 0, serializer, new byte[size * 1024]);
    }

    public static <Type> ASPage<Type>
    create(int space, int offset, FixedSizeSerializer<Type> serializer, byte[] page) {
        return new ASPage<>(space, offset, page, serializer);
    }

    @Override
    public synchronized Type
    get(int index) {
        if (outbound(index)) throw new ReadOverflowException(size, index);
        int offset = offset(index);
        return deserialize(offset);
    }

    @SuppressWarnings("unchecked")
    public synchronized Type[]
    read(Class<Type> type) {
        Type[] array = (Type[]) Array.newInstance(type, size);
        for (int index = 0; index < size; index ++) {
            array[index] = deserialize(offset(index));
        }
        return array;
    }

    public synchronized void
    set(int index, Type value) {
        if (outbound(index)) throw new WriteOverflowException(size, index);
        int offset = offset(index);
        serialize(value, offset);
    }

    public synchronized void
    insert(int index, Type value) {
        if (0 > index || index > size || spills()) throw new WriteOverflowException(size, index);
        int offset = offset(index);
        System.arraycopy(page, offset, page, offset(index + 1), (size - index) * serializer.sizeOf());
        serialize(value, offset);
        serializeLength(++size);
    }

    @Override
    public synchronized int
    append(Type value) {
        if (spills()) throw new WriteOverflowException(free());
        int offset = offset(size);
        serialize(value, offset);
        return serializeLength(++size);
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
        System.arraycopy(page, offset(index + 1), page, offset(index), (size - index - 1) * serializer.sizeOf());
        return serializeLength(--size);
    }

    public synchronized void
    clear() {
        serializeLength(this.size = 0);
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
        return space - occupied();
    }

    public int
    space() {
        return space;
    }

    @Override
    public int
    occupied() {
        return Short.BYTES + size * serializer.sizeOf();
    }

    public int
    offset() {
        return offset;
    }

    private Type
    deserialize(int offset) {
        return serializer.deserialize(page, offset);
    }

    @Override
    public Iterator<Type>
    iterator() {
        return new Iterator<>() {
            private int current = -1;

            @Override
            public boolean hasNext() {
                if (current + 1 < size) {
                    current ++;
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
                ASPage.this.remove(current);
            }
        };
    }

    public boolean
    spills() {
        return free() < serializer.sizeOf();
    }

    private int
    offset(int index) {
        return offset + Short.BYTES + index * serializer.sizeOf();
    }

    private boolean
    outbound(int index) {
        return 0 > index || index >= size;
    }

    private void
    serialize(Type value, int offset) {
        serializer.serialize(value, page, offset);
    }

    private int
    serializeLength(int value) {
        for (int position = Short.BYTES - 1; position >= 0; position--) {
            page[offset + position] = (byte) (value & 0xFF);
            value = (short) (value >> 8);
        }
        return value;
    }
}
