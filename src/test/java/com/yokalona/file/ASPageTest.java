package com.yokalona.file;

import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.file.exceptions.NegativePageSizeException;
import com.yokalona.file.exceptions.PageIsToLargeException;
import com.yokalona.file.exceptions.ReadOverflowException;
import com.yokalona.file.exceptions.WriteOverflowException;
import com.yokalona.file.page.ASPage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ASPageTest {

    @Test
    void testCreate() {
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        assertEquals(0, page.size());
        assertEquals(8190, page.free());
    }

    @Test
    void testCreateThrows() {
        assertThrows(PageIsToLargeException.class, () -> ASPage.create(129, new CompactIntegerSerializer(2)));
        assertThrows(NegativePageSizeException.class, () -> ASPage.create(0, new CompactIntegerSerializer(2)));
        assertThrows(NegativePageSizeException.class, () -> ASPage.create(-1, new CompactIntegerSerializer(2)));
    }

    @Test
    void testAppend() {
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        for (int index = 0; index < 4095; index++) {
            page.append(index);
            assertEquals(index, page.get(index));
        }
        assertEquals(0, page.free());
        assertEquals(4095, page.size());
    }

    @Test
    void testAppendThrows() {
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        for (int index = 0; index < 4095; index++) {
            page.append(index);
        }
        assertThrows(WriteOverflowException.class, () -> page.append(1200));
    }

    @Test
    void testGet() {
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        for (int index = 0; index < 4095; index++) {
            page.append(index);
        }
        for (int index = 0; index < 4095; index++) {
            assertEquals(index, page.get(index));
        }
    }

    @Test
    void testGetThrows() {
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        for (int index = 0; index < 4095; index++) {
            page.append(index);
        }
        assertThrows(ReadOverflowException.class, () -> page.get(-1));
        assertThrows(ReadOverflowException.class, () -> page.get(8012));
    }

    @Test
    void testInsert() {
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        for (int index = 0; index < 4094; index++) {
            page.append(index);
        }
        page.insert(44, 8890);
        for (short index = 0; index < 4095; index++) {
            if (index == 44) assertEquals(8890, page.get(44));
            else if (index < 44) assertEquals(index, page.get(index));
            else assertEquals(index - 1, page.get(index));
        }
    }

    @Test
    void testInsertThrows() {
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        for (int index = 0; index < 4095; index++) {
            page.append(index);
        }
        assertThrows(WriteOverflowException.class, () -> page.insert(-1, 0));
        assertThrows(WriteOverflowException.class, () -> page.insert(9999, 0));
        assertThrows(WriteOverflowException.class, () -> page.insert(44, 0));
    }

    @Test
    void testSet() {
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        for (int index = 0; index < 4095; index++) {
            page.append(index);
        }
        page.set(44, 8890);
        for (short index = 0; index < 4095; index++) {
            if (index == 44) assertEquals(8890, page.get(44));
            else assertEquals(index, page.get(index));
        }
    }

    @Test
    void testSetThrows() {
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        for (int index = 0; index < 4095; index++) {
            page.append(index);
        }
        assertThrows(WriteOverflowException.class, () -> page.set(-1, 0));
        assertThrows(WriteOverflowException.class, () -> page.set(9999, 0));
    }

    @Test
    void testSwap() {
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        for (int index = 0; index < 4095; index++) {
            page.append(index);
        }
        page.swap(44, 45);
        for (short index = 0; index < 4095; index++) {
            if (index == 44) assertEquals(45, page.get(44));
            else if (index == 45) assertEquals(44, page.get(index));
            else assertEquals(index, page.get(index));
        }
    }

    @Test
    void testSwapThrows() {
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        for (int index = 0; index < 4095; index++) {
            page.append(index);
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
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        for (int index = 0; index < 4095; index++) {
            page.append(index);
        }
        assertEquals(4095, page.size());
        page.remove(44);
        assertEquals(4094, page.size());
        for (short index = 0; index < 4094; index++) {
            if (index < 44) assertEquals(index, page.get(index));
            else assertEquals(index + 1, page.get(index));
        }
    }

    @Test
    void testRemoveThrows() {
        ASPage<Integer> page = ASPage.create(8, new CompactIntegerSerializer(2));
        assertThrows(WriteOverflowException.class, () -> page.remove(-1));
        assertThrows(WriteOverflowException.class, () -> page.remove(0));
        assertThrows(WriteOverflowException.class, () -> page.remove(999));
        page.append(44);
        page.remove(0);
        assertThrows(WriteOverflowException.class, () -> page.remove(0));
    }

}