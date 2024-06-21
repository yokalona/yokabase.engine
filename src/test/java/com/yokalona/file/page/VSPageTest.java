package com.yokalona.file.page;

import com.yokalona.array.serializers.primitives.StringSerializer;
import com.yokalona.file.AddressTools;
import com.yokalona.file.Array;
import com.yokalona.file.Pointer;
import com.yokalona.file.exceptions.NoFreeSpaceLeftException;
import com.yokalona.file.exceptions.WriteOverflowException;
import com.yokalona.tree.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class VSPageTest {

    public static final Random RANDOM = new Random();
//    public static final String ALPHABET = "X";

    @Test
    void testCreating() {
        byte[] space = new byte[16 * 1024];
        VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                new VSPage.Configuration(space, 0, 2048, space.length - 2048));
        assertEquals(0, page.size());
        assertEquals(2094, page.occupied());
    }

    @Test
    void testGet() {
        byte[] space = new byte[16 * 1024];
        VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                new VSPage.Configuration(space, 0, 2048, space.length - 2048));
        page.append("abc");
        assertEquals(1, page.size());
        assertEquals("abc", page.get(0));
        Assertions.assertEquals(2094 + AddressTools.significantBytes(16 * 1024) + StringSerializer.INSTANCE.sizeOf("abc"), page.occupied());
    }

    @Test
    void testRead() {
        byte[] space = new byte[16 * 1024];
        VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                new VSPage.Configuration(space, 0, 2048, space.length - 2048));
        page.append("abc");
        page.append("def");
        assertEquals(2, page.size());
        assertEquals("abc", page.get(0));
        assertEquals("def", page.get(1));
        assertEquals(2094 + AddressTools.significantBytes(16 * 1024) * 2 + StringSerializer.INSTANCE.sizeOf("abc") * 2, page.occupied());
        Array<String> read = page.read(String.class);
        assertEquals(2, read.length());
        assertEquals("abc", read.get(0));
        assertEquals("def", read.get(1));
    }

    @Test
    void testSet() {
        byte[] space = new byte[16 * 1024];
        VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                new VSPage.Configuration(space, 0, 2048, space.length - 2048));
        page.append("abc");
        assertEquals(1, page.size());
        assertEquals("abc", page.get(0));
        page.set(0, "def");
        assertEquals(1, page.size());
        assertEquals("def", page.get(0));
        page.set(0, "g");
        assertEquals(1, page.size());
        assertEquals("g", page.get(0));
        assertThrows(WriteOverflowException.class, () -> page.set(0, "abc"));
    }

    @Test
    void testAppend() {
        byte[] space = new byte[16 * 1024];
        VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                new VSPage.Configuration(space, 0, 2048, space.length - 2048));
        page.append("abc");
        assertEquals(1, page.size());
        assertEquals("abc", page.get(0));
        while (page.fits("abc")) {
            page.append("abc");
        }
        TestHelper.prettyPrint(space);
        assertThrows(NoFreeSpaceLeftException.class, () -> page.append("abc"));
        page.remove(2);
        page.remove(2);
        page.remove(2);
        page.remove(2);
        page.remove(2);
        page.remove(2);
        page.append(TestHelper.ALPHABET);
    }

    @Test
    void testFree() {
        byte[] space = new byte[2 * 1024];
        VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                new VSPage.Configuration(space, 0, 1024, space.length - 1024));
        page.append("abc");
        assertEquals(1, page.size());
        assertEquals("abc", page.get(0));
        int memory = page.free();
        while (page.fits("abc")) {
            page.append("abc");
            memory -= StringSerializer.INSTANCE.sizeOf("abc") + 2;
            assertEquals(memory, page.free());
        }
        assertThrows(NoFreeSpaceLeftException.class, () -> page.append("abc"));
        page.remove(2);
        assertEquals(memory + StringSerializer.INSTANCE.sizeOf("abc") + 2, page.free());
//        page.flush();
        TestHelper.prettyPrint(space);
    }

    @Test
    void testRepeatableAppend() {
        byte[] space = new byte[2 * 1024];
        for (int i = 0; i <= 1000; i++) {
            VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                    new VSPage.Configuration(space, 0, 1024, space.length - 1024));
            String string = TestHelper.randomString(i);
            List<String> strings = new ArrayList<>();
            while (page.fits(string)) {
                strings.add(string);
                page.append(string);
                string = TestHelper.randomString(i);
            }
            assertEquals(strings.size(), page.size());
            assertTrue(page.free() < i + Integer.BYTES + 2);
            for (int index = 0; index < strings.size(); index++) {
                assertEquals(strings.get(index), page.get(index));
            }
        }
    }

    @Test
    void testRepeatablyAppendRemove1() {
        byte[] space = new byte[2 * 1024];
        for (int i = 3; i <= 1000; i++) {
            VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                    new VSPage.Configuration(space, 0, 1024, space.length - 1024));
            String string = TestHelper.randomString(i);
            List<String> strings = new ArrayList<>();
            while (page.fits(string)) {
                strings.add(string);
                page.append(string);
                string = TestHelper.randomString(i);
            }
            assertEquals(strings.size(), page.size());
            assertTrue(page.free() < i + Integer.BYTES + 2);
            for (int index = 0; index < strings.size(); index++) {
                assertEquals(strings.get(index), page.get(index));
            }

            for (int index = 0; index < strings.size(); index += 2) {
                page.remove(index);
                strings.remove(index);
            }
            for (int index = 0; index < strings.size(); index++) {
                assertEquals(strings.get(index), page.get(index));
            }

            while (page.fits(string)) {
                strings.add(string);
                page.append(string);
                string = TestHelper.randomString(i);
            }
            for (int index = 0; index < strings.size(); index++) {
                assertEquals(strings.get(index), page.get(index));
            }
        }
    }

    @Test
    void testRepeatablyAppendRemove2() throws FileNotFoundException { // 5.716
        byte[] space = new byte[3048];
        int min = 3, max = 100;
        VSPage<String> page = null;
        try {
            for (int i = 0; i < 1000; i++) {
                System.out.print("\rIteration: " + i);
                System.out.flush();
                page = new VSPage<>(StringSerializer.INSTANCE,
                        new VSPage.Configuration(space, 1000, 256, space.length - 1000 - 256));
                String string = TestHelper.randomString(min, max);
                List<String> strings = new ArrayList<>();
                while (page.fits(string)) {
                    strings.add(string);
                    page.append(string);
                    string = TestHelper.randomString(min, max);
                }
                assertEquals(strings.size(), page.size());
                for (int index = 0; index < strings.size(); index++) {
                    assertEquals(strings.get(index), page.get(index));
                }

                for (int subiteration = 0; subiteration < 100; subiteration++) {
                    for (int index = 0; index < strings.size(); index += RANDOM.nextBoolean() ? 1 : 2) {
                        int idx = RANDOM.nextInt(strings.size());
                        page.remove(idx);
                        strings.remove(idx);
                    }
                    for (int index = 0; index < strings.size(); index++) {
                        assertEquals(strings.get(index), page.get(index));
                    }
                    while (page.fits(string)) {
                        strings.add(string);
                        page.append(string);
                        string = TestHelper.randomString(min, max);
                    }
                    for (int index = 0; index < strings.size(); index++) {
                        assertEquals(strings.get(index), page.get(index));
                    }
                }
            }
            System.out.println();
        } catch (Throwable t) {
            System.setOut(new PrintStream(new FileOutputStream("test-out.out")));
            TestHelper.prettyPrint(space);
            throw t;
        }
        TestHelper.prettyPrint(space);
    }

    @Test
    void testRepeatablyAppendRemove3() { //16.306
        byte[] space = new byte[2 * 1024];
        int min = 15, max = 15;
        try {
            for (int i = 0; i <= 100; i++) {
                VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                        // fix header issue already
                        new VSPage.Configuration(space, 0, 1024, space.length - 1024));
                String string = TestHelper.randomString(min, max);
                List<String> strings = new ArrayList<>();
                while (page.fits(string)) {
                    strings.add(string);
                    page.append(string);
                    string = TestHelper.randomString(min, max);
                }
                assertEquals(strings.size(), page.size());
                for (int index = 0; index < strings.size(); index++) {
                    assertEquals(strings.get(index), page.get(index));
                }
                for (int j = 0; j < 1000; j++) {
                    for (int index = 0; index < strings.size(); index += 2) {
                        int idx = RANDOM.nextInt(strings.size());
                        page.remove(idx);
                        strings.remove(idx);
                    }
                    int[] array = TestHelper.arrayOfSize(strings.size());
                    for (int index = 0; index < strings.size(); index++) {
                        assertEquals(strings.get(array[index]), page.get(array[index]));
                    }
                    while (page.fits(string)) {
                        strings.add(string);
                        page.append(string);
                        string = TestHelper.randomString(min, max);
                    }
                    array = TestHelper.arrayOfSize(strings.size());
                    for (int index = 0; index < strings.size(); index++) {
                        assertEquals(strings.get(array[index]), page.get(array[index]));
                    }
                }

                page.flush();
                VSPage<String> read = VSPage.read(StringSerializer.INSTANCE, space, 0);
                int[] array = TestHelper.arrayOfSize(strings.size());
                for (int index = 0; index < strings.size(); index++) {
                    assertEquals(strings.get(array[index]), read.get(array[index]));
                }
            }
        } catch (Throwable t) {
            TestHelper.prettyPrint(space);
            throw t;
        }
        TestHelper.prettyPrint(space);
    }

//    @Test
    void testSmallASpace() {
        byte[] space = new byte[4 * 1024];
        int min = 3, max = 10;
        try {
            for (int i = 0; i < 1000; i ++) {
                VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                        // fix header issue already
                        new VSPage.Configuration(space, 0, 32, space.length - 32));

                String string = TestHelper.randomString(min, max);
                while (page.fits(string)) {
                    page.append(string);
                    string = TestHelper.randomString(min, max);
                }

                int size = page.size();
                for (int index = 0; index < size / 4; index++) {
                    int idx = RANDOM.nextInt(page.size());
                    page.remove(idx);
                }

                while (page.fits(string)) {
                    page.append(string);
                    string = TestHelper.randomString(min, max);
                }
                System.out.print("\r" + page.size() + " " + page.free());
                System.out.flush();
            }
        } catch (Throwable t) {
            TestHelper.prettyPrint(space);
            throw t;
        }
    }

}