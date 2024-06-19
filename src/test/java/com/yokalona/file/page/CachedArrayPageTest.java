package com.yokalona.file.page;

import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.file.Array;
import com.yokalona.file.exceptions.ReadOverflowException;
import com.yokalona.file.exceptions.WriteOverflowException;
import com.yokalona.tree.TestHelper;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class CachedArrayPageTest {

    @Test
    void testSet() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index ++);
        }
        page.set(44, 8890);
        while (index-- > 0) {
            if (index == 44) assertEquals(8890, page.get(44));
            else assertEquals(index, page.get(index));
        }
    }

    @Test
    void testSetThrows() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index);
        }
        assertThrows(WriteOverflowException.class, () -> page.set(-1, 0));
        assertThrows(WriteOverflowException.class, () -> page.set(9999, 0));
    }

    @Test
    void testSwap() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index ++);
        }
        page.swap(44, 45);
        while (index-- > 0) {
            if (index == 44) assertEquals(45, page.get(44));
            else if (index == 45) assertEquals(44, page.get(index));
            else assertEquals(index, page.get(index));
        }
    }

    @Test
    void testSwapThrows() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index ++);
        }
        assertThrows(WriteOverflowException.class, () -> page.swap(-1, 45));
        assertThrows(WriteOverflowException.class, () -> page.swap(44, -1));
        assertThrows(WriteOverflowException.class, () -> page.swap(-1, -1));
        assertThrows(WriteOverflowException.class, () -> page.swap(44, 9999));
        assertThrows(WriteOverflowException.class, () -> page.swap(9999, 44));
        assertThrows(WriteOverflowException.class, () -> page.swap(9999, 9999));
    }

    @Test
    void testInsert() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index);
            assertEquals(index, page.get(index++));
        }
        page.remove(page.size() - 1);
        page.insert(44, 8890);
        while (index-- > 0) {
            if (index == 44) assertEquals(8890, page.get(44));
            else if (index < 44) assertEquals(index, page.get(index));
            else assertEquals(index - 1, page.get(index));
        }
    }

    @Test
    void testInsertThrows() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index++);
        }
        assertThrows(WriteOverflowException.class, () -> page.insert(-1, 0));
        assertThrows(WriteOverflowException.class, () -> page.insert(9999, 0));
        assertThrows(WriteOverflowException.class, () -> page.insert(44, 0));
    }

    @Test
    void testFirst() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index++);
        }
        assertEquals(0, page.first());
    }

    @Test
    void testFirstThrows() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        assertThrows(ReadOverflowException.class, page::first);
    }

    @Test
    void testLast() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index++);
        }
        assertEquals(4084, page.last());
    }

    @Test
    void testLastThrows() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        assertThrows(ReadOverflowException.class, page::last);
    }

    @Test
    void testFind() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index++);
        }
        Array<Integer> read = page.read(Integer.class);
        int [] indexes = new int [read.length()];
        TestHelper.shuffle(indexes);
        for (int ind : indexes) {
            Integer datum = read.get(ind);
            assertEquals(datum, page.find(datum, Integer::compare));
        }
        page.remove(44);
        assertEquals(44, -(page.find(44, Integer::compare) + 1));
    }

    @Test
    void testIterator() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index++);
        }
        Array<Integer> read = page.read(Integer.class);
        Integer[] array = new Integer[read.length()];
        for (int ind = 0; ind < array.length; ind++) {array[ind] = read.get(ind);}
        Iterator<Integer> iterator = page.iterator();
        index = 0;
        while (iterator.hasNext()) {
            Integer value = iterator.next();
            iterator.remove();
            assertEquals(array[index ++], value);
        }
    }

    @Test
    void testIteratorThrows() {
        ArrayPage<Integer> page = new CachedArrayPage<>(new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8 * 1024)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index++);
        }
        Iterator<Integer> iterator = page.iterator();
        assertThrows(IllegalStateException.class, iterator::remove);
        assertTrue(iterator.hasNext());
        iterator.remove();
        assertEquals(--index, page.size());
        assertThrows(IllegalStateException.class, iterator::remove);
    }

    @Test
    void testRepeatedGet() {
        class CountedASPage<Type> extends ASPage<Type> {
            public int count = 0;
            public CountedASPage(FixedSizeSerializer<Type> serializer, Configuration configuration) {
                super(serializer, configuration);
            }

            @Override
            public synchronized Type get(int index) {
                count ++;
                return super.get(index);
            }
        }

        byte[] array = new byte[1024];
        CountedASPage<Integer> counter = new CountedASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(array, 0, array.length));
        ArrayPage<Integer> page = new CachedArrayPage<>(counter);
        int index = 0;
        while (page.free() >= 2) {
            page.append(index);
        }

        page.get(0);
        page.get(0);
        page.get(0);
        page.get(0);
        assertEquals(1, counter.count);

        Iterator<Integer> iterator = page.iterator();
        while (iterator.hasNext()) {
            Integer ignore = iterator.next();
        }
        assertEquals(page.size(), counter.count);
    }
}