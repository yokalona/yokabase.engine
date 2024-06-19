package com.yokalona.file.page;

import com.yokalona.array.serializers.primitives.StringSerializer;
import com.yokalona.file.Array;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataSpaceTest {

    @Test
    void testCreate() {
        IndexedDataSpace<String> dataSpace = new IndexedDataSpace<>(StringSerializer.INSTANCE, new ASPage.Configuration(4096));
        assertEquals(0, dataSpace.size());
        assertEquals(2, dataSpace.pointerSize());
    }

    @Test
    void testGet() {
        IndexedDataSpace<String> dataSpace = new IndexedDataSpace<>(StringSerializer.INSTANCE, new ASPage.Configuration(4096));
        dataSpace.insert(2048, "abc");
        assertEquals("abc", dataSpace.get(0));
        assertEquals(2048, dataSpace.address(0));
        assertEquals(1, dataSpace.size());
    }

    @Test
    void testAddress() {
        IndexedDataSpace<String> dataSpace = new IndexedDataSpace<>(StringSerializer.INSTANCE, new ASPage.Configuration(4096));
        dataSpace.insert(2048, "abc");
        assertEquals("abc", dataSpace.get(0));
        assertEquals(2048, dataSpace.address(0));
    }

    @Test
    void testSet() {
        IndexedDataSpace<String> dataSpace = new IndexedDataSpace<>(StringSerializer.INSTANCE, new ASPage.Configuration(4096));
        dataSpace.insert(2048, "abc");
        assertEquals("abc", dataSpace.get(0));
        dataSpace.set(0, "def");
        assertEquals("def", dataSpace.get(0));
        assertEquals(1, dataSpace.size());
    }

    @Test
    void testInsert() {
        IndexedDataSpace<String> dataSpace = new IndexedDataSpace<>(StringSerializer.INSTANCE, new ASPage.Configuration(4096));
        assertEquals(1, dataSpace.insert(2048, "abc"));
        assertEquals("abc", dataSpace.get(0));
        assertEquals(2048, dataSpace.address(0));
        assertEquals(1, dataSpace.size());
    }

    @Test
    void testRemove() {
        IndexedDataSpace<String> dataSpace = new IndexedDataSpace<>(StringSerializer.INSTANCE, new ASPage.Configuration(4096));
        dataSpace.insert(2048, "abc");
        assertEquals("abc", dataSpace.get(0));
        dataSpace.remove(0);
        assertEquals(0, dataSpace.size());
    }

    @Test
    void testRead() {
        CachedDataSpace<String> dataSpace = new CachedDataSpace<>(new IndexedDataSpace<>(StringSerializer.INSTANCE, new ASPage.Configuration(4096)));
        dataSpace.insert(2048, "abc");
        dataSpace.insert(2080, "def");
        Array<String> read = dataSpace.read(String.class);
        assertEquals(2, read.length());
        assertEquals("abc", read.get(0));
        assertEquals("def", read.get(1));
    }

    @Test
    void testClear() {
        CachedDataSpace<String> dataSpace = new CachedDataSpace<>(new IndexedDataSpace<>(StringSerializer.INSTANCE, new ASPage.Configuration(4096)));
        dataSpace.insert(2048, "abc");
        dataSpace.insert(2080, "def");
        dataSpace.clear();
        Array<String> read = dataSpace.read(String.class);
        assertEquals(0, read.length());
        assertEquals(0, dataSpace.size());
    }
}