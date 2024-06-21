package com.yokalona.file.page;

import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.file.Array;
import com.yokalona.file.exceptions.*;
import com.yokalona.file.headers.CRC;
import org.junit.jupiter.api.Test;

import static com.yokalona.file.page.FSPage.MAX_AS_PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.*;

class CachedPageTest {

    @Test
    void testCreate() {
        Page<Integer> page = new CachedPage<>(FSPage.Configurer.create(8 * 1024).addHeader(new CRC()).fspage(new CompactIntegerSerializer(2)));
        assertEquals(0, page.size());
        assertEquals(8170, page.free());
    }

    @Test
    void testCreateThrows() {
        assertThrows(PageIsTooLargeException.class, () -> new CachedPage<>(FSPage.Configurer.create(4 * 1024 * 1024 + 1)
                .fspage(new CompactIntegerSerializer(2))));
        assertThrows(PageIsTooSmallException.class, () -> new CachedPage<>(FSPage.Configurer.create(0)
                .fspage(new CompactIntegerSerializer(2))));
        assertThrows(NegativePageSizeException.class, () -> new CachedPage<>(FSPage.Configurer.create(8 * 1024)
                .length(-1)
                .fspage(new CompactIntegerSerializer(2))));
        assertThrows(NegativeOffsetException.class, () -> new CachedPage<>(FSPage.Configurer.create(new byte[8 * 1024], -1)
                .fspage(new CompactIntegerSerializer(2))));
        assertThrows(PageIsTooLargeException.class, () -> new CachedPage<>(FSPage.Configurer.create(new byte[8 * 1024], 8096)
                .fspage(new CompactIntegerSerializer(2))));
        assertThrows(PageIsTooLargeException.class, () -> new CachedPage<>(FSPage.Configurer.create(new byte[MAX_AS_PAGE_SIZE], 0)
                .length(MAX_AS_PAGE_SIZE + 1)
                .fspage(new CompactIntegerSerializer(2))));
    }

    @Test
    void testAppend() {
        Page<Integer> page = new CachedPage<>(FSPage.Configurer.create(8 * 1024).addHeader(new CRC()).fspage(new CompactIntegerSerializer(2)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index);
            assertEquals(index, page.get(index++));
        }
        assertEquals(0, page.free());
        assertEquals(4085, page.size());
    }

    @Test
    void testAppendThrows() {
        Page<Integer> page = new CachedPage<>(FSPage.Configurer.create(8 * 1024).addHeader(new CRC()).fspage(new CompactIntegerSerializer(2)));
        int index = 0;
        while (page.free() >= 2) {
            page.append(index++);
        }
        assertThrows(WriteOverflowException.class, () -> page.append(1200));
    }

    @Test
    void testGet() {
        Page<Integer> page = new CachedPage<>(FSPage.Configurer.create(8 * 1024).addHeader(new CRC()).fspage(new CompactIntegerSerializer(2)));
        int index = 0;
        while (page.free() > 2) {
            page.append(index++);
        }
        while (index > 0) {
            assertEquals(--index, page.get(index));
        }
    }

    @Test
    void testGetThrows() {
        Page<Integer> page = new CachedPage<>(FSPage.Configurer.create(8 * 1024).addHeader(new CRC()).fspage(new CompactIntegerSerializer(2)));
        int index = 0;
        while (page.free() > 2) {
            page.append(index++);
        }
        assertThrows(ReadOverflowException.class, () -> page.get(-1));
        assertThrows(ReadOverflowException.class, () -> page.get(8012));
    }

    @Test
    void testRemove() {
        Page<Integer> page = new CachedPage<>(FSPage.Configurer.create(8 * 1024).addHeader(new CRC()).fspage(new CompactIntegerSerializer(2)));
        int index = 0;
        while (page.free() >= 2) {
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
        Page<Integer> page = new CachedPage<>(FSPage.Configurer.create(8 * 1024).addHeader(new CRC()).fspage(new CompactIntegerSerializer(2)));
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
        Page<Integer> page = new CachedPage<>(FSPage.Configurer.create(array)
                .addHeader(new CRC())
                .fspage(new CompactIntegerSerializer(2)));
        int index = 0;
        while (page.free() > 2) {
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
        Page<Integer> page = new CachedPage<>(FSPage.Configurer.create(8 * 1024).addHeader(new CRC()).fspage(new CompactIntegerSerializer(2)));
        int index = 0;
        while (page.free() > 2) {
            page.append(index++);
        }
        page.flush();
        array[18] = 0x7F;
        assertThrows(CRCMismatchException.class, () -> FSPage.Configurer.create(array).addHeader(new CRC())
                .read(new CompactIntegerSerializer(2)));
    }

    @Test
    void testClear() {
        Page<Integer> page = new CachedPage<>(FSPage.Configurer.create(8 * 1024).addHeader(new CRC()).fspage(new CompactIntegerSerializer(2)));
        int index = 0;
        while (page.free() > 2) {
            page.append(index++);
        }
        page.clear();
        assertEquals(0, page.size());
        assertThrows(ReadOverflowException.class, () -> page.get(0));
    }

    @Test
    void testRead() {
        Page<Integer> page = new CachedPage<>(FSPage.Configurer.create(8 * 1024).addHeader(new CRC()).fspage(new CompactIntegerSerializer(2)));
        int index = 0;
        while (page.free() > 2) {
            page.append(index++);
        }
        Array<Integer> read = page.read(Integer.class);
        for (int i = 0; i < read.length(); i++) {
            assertEquals(i, read.get(i));
        }
    }

}