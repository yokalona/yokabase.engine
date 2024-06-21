package com.yokalona.file.page;

import com.yokalona.array.serializers.primitives.StringSerializer;
import com.yokalona.file.Array;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataSpaceTest {

    @Test
    void testCreate() {
        DataSpace<String> dataSpace = IndexedDataSpace.Configurer.create(FSPage.Configurer.create(4096)).dataspace(StringSerializer.INSTANCE);
        assertEquals(0, dataSpace.size());
        assertEquals(2, dataSpace.pointerSize());
    }

    @Test
    void testGet() {
        DataSpace<String> dataSpace = IndexedDataSpace.Configurer.create(FSPage.Configurer.create(4096)).dataspace(StringSerializer.INSTANCE);
        dataSpace.insert(2048, "abc");
        assertEquals("abc", dataSpace.get(0));
        assertEquals(2048, dataSpace.address(0));
        assertEquals(1, dataSpace.size());
    }

    @Test
    void testAddress() {
        DataSpace<String> dataSpace = IndexedDataSpace.Configurer.create(FSPage.Configurer.create(4096)).dataspace(StringSerializer.INSTANCE);
        dataSpace.insert(2048, "abc");
        assertEquals("abc", dataSpace.get(0));
        assertEquals(2048, dataSpace.address(0));
    }

    @Test
    void testSet() {
        DataSpace<String> dataSpace = IndexedDataSpace.Configurer.create(FSPage.Configurer.create(4096)).dataspace(StringSerializer.INSTANCE);
        dataSpace.insert(2048, "abc");
        assertEquals("abc", dataSpace.get(0));
        dataSpace.set(0, "def");
        assertEquals("def", dataSpace.get(0));
        assertEquals(1, dataSpace.size());
    }

    @Test
    void testInsert() {
        DataSpace<String> dataSpace = IndexedDataSpace.Configurer.create(FSPage.Configurer.create(4096)).dataspace(StringSerializer.INSTANCE);
        assertEquals(1, dataSpace.insert(2048, "abc"));
        assertEquals("abc", dataSpace.get(0));
        assertEquals(2048, dataSpace.address(0));
        assertEquals(1, dataSpace.size());
    }

    @Test
    void testRemove() {
        DataSpace<String> dataSpace = IndexedDataSpace.Configurer.create(FSPage.Configurer.create(4096)).dataspace(StringSerializer.INSTANCE);
        dataSpace.insert(2048, "abc");
        assertEquals("abc", dataSpace.get(0));
        dataSpace.remove(0);
        assertEquals(0, dataSpace.size());
    }

    @Test
    void testRead() {
        DataSpace<String> dataSpace = IndexedDataSpace.Configurer.create(FSPage.Configurer.create(4096)).dataspace(StringSerializer.INSTANCE);
        dataSpace.insert(2048, "abc");
        dataSpace.insert(2080, "def");
        Array<String> read = dataSpace.read(String.class);
        assertEquals(2, read.length());
        assertEquals("abc", read.get(0));
        assertEquals("def", read.get(1));
    }

    @Test
    void testClear() {
        DataSpace<String> dataSpace = IndexedDataSpace.Configurer.create(FSPage.Configurer.create(4096)).dataspace(StringSerializer.INSTANCE);
        dataSpace.insert(2048, "abc");
        dataSpace.insert(2080, "def");
        dataSpace.clear();
        Array<String> read = dataSpace.read(String.class);
        assertEquals(0, read.length());
        assertEquals(0, dataSpace.size());
    }
}