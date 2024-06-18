package com.yokalona.file.page;

import com.yokalona.array.serializers.primitives.LongSerializer;
import com.yokalona.array.serializers.primitives.StringSerializer;
import com.yokalona.file.AddressTools;
import com.yokalona.file.Cache;
import com.yokalona.file.CachedArrayProvider;
import com.yokalona.file.Pointer;
import com.yokalona.file.exceptions.NoFreeSpaceLeftException;
import com.yokalona.file.exceptions.WriteOverflowException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class VSPageTest {

    public static final Random RANDOM = new Random();
    public static final String ALPHABET = "abcdefghjiklmopqrstuvxyz0123456789";

    @Test
    void testCreating() {
        byte[] space = new byte[16 * 1024];
        VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                new VSPage.Configuration(space, 0, 2048, space.length - 2048));
        assertEquals(0, page.size());
        assertEquals(2066, page.occupied());
    }

    @Test
    void testGet() {
        byte[] space = new byte[16 * 1024];
        VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                new VSPage.Configuration(space, 0, 2048, space.length - 2048));
        page.append("abc");
        assertEquals(1, page.size());
        assertEquals("abc", page.get(0));
        Assertions.assertEquals(2066 + AddressTools.significantBytes(16 * 1024) + StringSerializer.INSTANCE.sizeOf("abc"), page.occupied());
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
        assertEquals(2066 + AddressTools.significantBytes(16 * 1024) * 2 + StringSerializer.INSTANCE.sizeOf("abc") * 2, page.occupied());
        Cache<String> read = page.read(String.class);
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
        prettyPrint(space);
        assertThrows(NoFreeSpaceLeftException.class, () -> page.append("abc"));
        page.remove(2);
        page.remove(2);
        page.remove(2);
        page.remove(2);
        page.append(ALPHABET);
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
        page.flush();
        prettyPrint(space);
    }

    @Test
    void testRepeatableAppend() {
        byte[] space = new byte[2 * 1024];
        for (int i = 0; i <= 1000; i++) {
            VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                    new VSPage.Configuration(space, 0, 1024, space.length - 1024));
            String string = randomString(i);
            List<String> strings = new ArrayList<>();
            while (page.fits(string)) {
                strings.add(string);
                page.append(string);
                string = randomString(i);
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
        for (int i = 0; i <= 1000; i++) {
            VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                    new VSPage.Configuration(space, 0, 1024, space.length - 1024));
            String string = randomString(i);
            List<String> strings = new ArrayList<>();
            while (page.fits(string)) {
                strings.add(string);
                page.append(string);
                string = randomString(i);
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
                string = randomString(i);
            }
            for (int index = 0; index < strings.size(); index++) {
                assertEquals(strings.get(index), page.get(index));
            }
        }
    }

    @Test
    void testRepeatablyAppendRemove2() {
        byte[] space = new byte[32 * 1024];
        int min = 0, max = 100;
        for (int i = 0; i <= 1000; i++) {
            VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                    new VSPage.Configuration(space, 0, 1024, space.length - 1024));
            String string = randomString(min, max);
            List<String> strings = new ArrayList<>();
            while (page.fits(string)) {
                strings.add(string);
                page.append(string);
                string = randomString(min, max);
            }
            assertEquals(strings.size(), page.size());
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
                string = randomString(min, max);
            }
            for (int index = 0; index < strings.size(); index++) {
                assertEquals(strings.get(index), page.get(index));
            }
        }
    }

    @Test
    void testRepeatablyAppendRemove3() {
        byte[] space = new byte[32 * 1024];
        int min = 0, max = 100;
        for (int i = 0; i <= 10; i++) {
            VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                    new VSPage.Configuration(space, 0, 1024, space.length - 1024));
            String string = randomString(min, max);
            List<String> strings = new ArrayList<>();
            while (page.fits(string)) {
                strings.add(string);
                page.append(string);
                string = randomString(min, max);
            }
            assertEquals(strings.size(), page.size());
            for (int index = 0; index < strings.size(); index++) {
                assertEquals(strings.get(index), page.get(index));
            }
            for (int j = 0; j < 10; j++) {
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
                    string = randomString(min, max);
                }
                for (int index = 0; index < strings.size(); index++) {
                    assertEquals(strings.get(index), page.get(index));
                }
            }
        }
        prettyPrint(space);
    }

    private static void
    prettyPrint(byte[] space) {
        System.out.printf("%3s%-3s|", "-", "-");
        for (int i = 0; i < 10; i++) {
            System.out.printf("%6d", i);
        }
        System.out.println("\n------+------------------------------------------------------------+------");
        int count = 0;
        for (int i = 0; i < space.length; i += 10) {
            System.out.printf("%6d|", count);
            int min = Math.min(i + 10, space.length);
            for (int j = i; j < min; j++) {
                if ((space[j] >= 'A' && space[j] <= 'Z') || (space[j] >= 'a' && space[j] <= 'z'))
                    System.out.printf("%6s", (char) space[j]);
                else System.out.printf("%6d", space[j]);
            }
            if (min < i + 10) System.out.printf("%" + (i + 10 - min) * 6 + "s|%-5d%n", "", count);
            else System.out.printf("|%-6d%n", count++);
        }
        System.out.println("------+------------------------------------------------------------+------");
        System.out.printf("%3s%-3s|", "-", "-");
        for (int i = 0; i < 10; i++) {
            System.out.printf("%6d", i);
        }
        System.out.println();
    }

    String
    randomString(int length) {
        byte[] bytes = new byte[length];
        return generate(bytes);
    }

    String
    randomString(int min, int max) {
        byte[] bytes = new byte[new Random().nextInt(min, max)];
        return generate(bytes);
    }

    private static String
    generate(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ALPHABET.charAt(new Random().nextInt(ALPHABET.length()));
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

}