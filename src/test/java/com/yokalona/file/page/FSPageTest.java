package com.yokalona.file.page;

import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.array.serializers.primitives.IntegerSerializer;
import com.yokalona.file.Array;
import com.yokalona.file.exceptions.*;
import com.yokalona.file.headers.CRC;
import com.yokalona.tree.TestHelper;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.yokalona.file.page.FSPage.MAX_AS_PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.*;

class FSPageTest {

    public static final Random RANDOM = new Random();

    @Test
    void testCreate() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        assertEquals(0, page.size());
        assertEquals(8170, page.free());
    }

    @Test
    void testCreateThrows() {
        assertThrows(PageIsTooLargeException.class, () -> FSPage.Configurer.create(MAX_AS_PAGE_SIZE + 1)
                        .fspage(new CompactIntegerSerializer(2)));
        assertThrows(PageIsTooSmallException.class, () -> FSPage.Configurer.create(0)
                .fspage(new CompactIntegerSerializer(2)));
        assertThrows(NegativePageSizeException.class, () -> FSPage.Configurer.create(8 * 1024)
                .length(-1)
                .fspage(new CompactIntegerSerializer(2)));
        assertThrows(NegativeOffsetException.class, () -> FSPage.Configurer.create(new byte[8 * 1024], -1)
                        .fspage(new CompactIntegerSerializer(2)));
        assertThrows(PageIsTooLargeException.class, () -> FSPage.Configurer.create(new byte[8 * 1024], 8096)
                        .fspage(new CompactIntegerSerializer(2)));
        assertThrows(PageIsTooLargeException.class, () -> FSPage.Configurer.create(new byte[MAX_AS_PAGE_SIZE], 0)
                .length(MAX_AS_PAGE_SIZE + 1)
                .fspage(new CompactIntegerSerializer(2)));
    }

    @Test
    void testAppend() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index);
            assertEquals(index, page.get(index++));
        }
        assertEquals(0, page.free());
        assertEquals(4085, page.size());
    }

    @Test
    void testAppendThrows() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        assertThrows(WriteOverflowException.class, () -> page.append(1200));
    }

    @Test
    void testGet() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        while (index > 0) {
            assertEquals(--index, page.get(index));
        }
    }

    @Test
    void testGetThrows() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        assertThrows(ReadOverflowException.class, () -> page.get(-1));
        assertThrows(ReadOverflowException.class, () -> page.get(8012));
    }

    @Test
    void testInsert() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
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
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
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
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        page.set(44, 8890);
        while (index-- > 0) {
            if (index == 44) assertEquals(8890, page.get(44));
            else assertEquals(index, page.get(index));
        }
    }

    @Test
    void testSetThrows() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index);
        }
        assertThrows(WriteOverflowException.class, () -> page.set(-1, 0));
        assertThrows(WriteOverflowException.class, () -> page.set(9999, 0));
    }

    @Test
    void testSwap() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
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
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
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
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        assertEquals(4085, page.size());
        page.remove(44);
        assertEquals(4084, page.size());
        index--;
        while (index-- > 0) {
            if (index < 44) assertEquals(index, page.get(index));
            else assertEquals(index + 1, page.get(index));
        }
    }

    @Test
    void testRemoveThrows() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
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
        FSPage<Integer> page = FSPage.Configurer.create(array)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        page.flush();
        page = FSPage.Configurer.create(array)
                .addHeader(new CRC())
                .read(new CompactIntegerSerializer(2));
        while (index-- > 0) {
            assertEquals(index, page.get(index));
        }
    }

    @Test
    void testWriteThrows() {
        byte[] array = new byte[8 * 1024];
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        page.flush();
        byte prior = array[44];
        array[44] = 0x7F;
        assertThrows(CRCMismatchException.class, () -> FSPage.Configurer.create(array).addHeader(new CRC()).read(new CompactIntegerSerializer(2)));
        array[44] = prior;
        FSPage.Configurer.create(array).addHeader(new CRC()).read(new CompactIntegerSerializer(2));
    }

    @Test
    void testClear() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
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
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        assertEquals(0, page.first());
    }

    @Test
    void testFirstThrows() {
        ArrayPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        assertThrows(ReadOverflowException.class, page::first);
    }

    @Test
    void testLast() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        assertEquals(4088, page.last());
    }

    @Test
    void testLastThrows() {
        ArrayPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        assertThrows(ReadOverflowException.class, page::last);
    }

    @Test
    void testRead() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        Array<Integer> read = page.read(Integer.class);
        for (int i = 0; i < read.length(); i++) {
            assertEquals(i, read.get(i));
        }
    }

    @Test
    void testFind() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        Array<Integer> read = page.read(Integer.class);
        int[] indexes = new int[read.length()];
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
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
        int index = 0;
        while (!page.spills()) {
            page.append(index++);
        }
        Array<Integer> read = page.read(Integer.class);
        Integer[] array = new Integer[read.length()];
        for (int ind = 0; ind < array.length; ind++) {
            array[ind] = read.get(ind);
        }
        Iterator<Integer> iterator = page.iterator();
        index = 0;
        while (iterator.hasNext()) {
            Integer value = iterator.next();
            iterator.remove();
            assertEquals(array[index++], value);
        }
    }

    @Test
    void testIteratorThrows() {
        FSPage<Integer> page = FSPage.Configurer.create(8 * 1024)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2));
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

    @Test
    void testRabbitAndTheHat() {
        for (int iteration = 0; iteration < 1000; iteration++) {
            ArrayPage<Integer> page = new CachedArrayPage<>(FSPage.Configurer.create(8 * 1024)
                    .fspage(IntegerSerializer.INSTANCE));
            int index = 0;
            List<Integer> integers = new ArrayList<>();
            page.append(index);
            integers.add(index);
            for (int subiteration = 0; subiteration < 100; subiteration++) {
                while (page.free() >= page.serializer().sizeOf()) {
                    int idx = RANDOM.nextInt(page.size());
                    int action = RANDOM.nextInt(4);
                    switch (action) {
                        case 0: {
                            page.append(index);
                            integers.add(index++);
                        } break;
                        case 1: {
                            page.insert(idx, index);
                            integers.add(idx, index++);
                        } break;
                        case 2: {
                            page.set(idx, index);
                            integers.set(idx, index++);
                        }
                        case 3: {
                            int idx2 = RANDOM.nextInt(page.size());
                            page.swap(idx, idx2);
                            Collections.swap(integers, idx, idx2);
                        } break;
                    }
                }
                assertEquals(integers.size(), page.size());
                for (int i = 0; i < integers.size(); i += 2) {
                    int idx = RANDOM.nextInt(page.size());
                    page.remove(idx);
                    integers.remove(idx);
                }
            }
            assertEquals(integers.size(), page.size());
            for (int i = 0; i < integers.size(); i++) {
                assertEquals(integers.get(i), page.get(i));
            }
        }
    }

}