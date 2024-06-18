package com.yokalona.file.page;

import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.file.Cache;
import com.yokalona.file.exceptions.*;
import com.yokalona.tree.TestHelper;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static com.yokalona.file.page.ASPage.MAX_AS_PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.*;

class ASPageTest {

    @Test
    void testCreate() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        assertEquals(0, page.size());
        assertEquals(8174, page.free());
    }

    @Test
    void testCreateThrows() {
        assertThrows(PageIsTooLargeException.class, () -> new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(4 * 1024 + 1)));
        assertThrows(PageIsTooSmallException.class, () -> new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(0)));
        assertThrows(NegativePageSizeException.class, () -> new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(new byte[4096], 0, -1)));
        assertThrows(NegativeOffsetException.class, () -> new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(new byte[4096], -1, 4096)));
        assertThrows(PageIsTooLargeException.class, () -> new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(new byte[4096], 4096, 1)));
        assertThrows(PageIsTooLargeException.class, () -> new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(new byte[MAX_AS_PAGE_SIZE + 1], 0, MAX_AS_PAGE_SIZE + 1)));
    }

    @Test
    void testAppend() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index);
            assertEquals(index, page.get(index++));
        }
        assertEquals(0, page.free());
        assertEquals(4087, page.size());
    }

    @Test
    void testAppendThrows() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index ++);
        }
        assertThrows(WriteOverflowException.class, () -> page.append(1200));
    }

    @Test
    void testGet() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index ++);
        }
        while (index > 0) {
            assertEquals(--index, page.get(index));
        }
    }

    @Test
    void testGetThrows() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index ++);
        }
        assertThrows(ReadOverflowException.class, () -> page.get(-1));
        assertThrows(ReadOverflowException.class, () -> page.get(8012));
    }

    @Test
    void testInsert() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
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
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        assertThrows(WriteOverflowException.class, () -> page.insert(-1, 0));
        assertThrows(WriteOverflowException.class, () -> page.insert(9999, 0));
        assertThrows(WriteOverflowException.class, () -> page.insert(44, 0));
    }

    @Test
    void testSet() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
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
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index);
        }
        assertThrows(WriteOverflowException.class, () -> page.set(-1, 0));
        assertThrows(WriteOverflowException.class, () -> page.set(9999, 0));
    }

    @Test
    void testSwap() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
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
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
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
    void testRemove() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        assertEquals(4087, page.size());
        page.remove(44);
        assertEquals(4086, page.size());
        index --;
        while (index-- > 0) {
            if (index < 44) assertEquals(index, page.get(index));
            else assertEquals(index + 1, page.get(index));
        }
    }

    @Test
    void testRemoveThrows() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        assertThrows(WriteOverflowException.class, () -> page.remove(-1));
        assertThrows(WriteOverflowException.class, () -> page.remove(0));
        assertThrows(WriteOverflowException.class, () -> page.remove(999));
        page.append(44);
        page.remove(0);
        assertThrows(WriteOverflowException.class, () -> page.remove(0));
    }

    @Test
    void testWrite() {
        byte[] array = new byte[8 * 1024];
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(array, 0, array.length));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        page.flush();
        page = ASPage.read(new CompactIntegerSerializer(2), new ASPage.Configuration(array, 0, array.length));
        while (index-- > 0) {
            assertEquals(index, page.get(index));
        }
    }

    @Test
    void testWriteThrows() {
        byte[] array = new byte[8 * 1024];
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(array, 0, array.length));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        page.flush();
        array[18] = 0x7F;
        assertThrows(CRCMismatchException.class, () -> ASPage.read(new CompactIntegerSerializer(2), new ASPage.Configuration(array, 0, array.length)));
    }

    @Test
    void testClear() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        page.clear();
        assertEquals(0, page.size());
        assertThrows(ReadOverflowException.class, () -> page.get(0));
    }

    @Test
    void testFirst() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        assertEquals(0, page.first());
    }

    @Test
    void testFirstThrows() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        assertThrows(ReadOverflowException.class, page::first);
    }

    @Test
    void testLast() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        assertEquals(4086, page.last());
    }

    @Test
    void testLastThrows() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        assertThrows(ReadOverflowException.class, page::last);
    }

    @Test
    void testRead() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        Cache<Integer> read = page.read();
        for (int i = 0; i < read.length(); i++) {
            assertEquals(i, read.get(i));
        }
    }

    @Test
    void testFind() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        Cache<Integer> read = page.read();
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
    void testOffset() {
        ASPage.Configuration configuration = new ASPage.Configuration(8);
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), configuration);
        assertEquals(configuration.offset(), page.offset());
    }

    @Test
    void testLength() {
        ASPage.Configuration configuration = new ASPage.Configuration(8);
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), configuration);
        assertEquals(configuration.length(), page.length());
    }

    @Test
    void testIterator() {
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        Cache<Integer> read = page.read();
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
        ASPage<Integer> page = new ASPage<>(new CompactIntegerSerializer(2), new ASPage.Configuration(8));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        Iterator<Integer> iterator = page.iterator();
        assertThrows(IllegalStateException.class, iterator::remove);
        assertTrue(iterator.hasNext());
        iterator.remove();
        assertEquals(--index, page.size());
        assertThrows(IllegalStateException.class, iterator::remove);
    }

}