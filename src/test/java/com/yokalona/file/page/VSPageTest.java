package com.yokalona.file.page;

import com.yokalona.array.serializers.primitives.StringSerializer;
import com.yokalona.file.AddressTools;
import com.yokalona.file.exceptions.NoFreeSpaceLeftException;
import com.yokalona.file.exceptions.WriteOverflowException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
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
        String[] read = page.read(String.class);
        assertEquals(2, read.length);
        assertEquals("abc", read[0]);
        assertEquals("def", read[1]);
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
        prettyPrint(space);
        assertEquals(1, page.size());
        assertEquals("abc", page.get(0));
        while (page.fits("abc")) {
            page.append("abc");
        }
        assertThrows(NoFreeSpaceLeftException.class, () -> page.append("abc"));
        page.remove(2);
        page.remove(2);
        page.remove(2);
        page.remove(2);
        page.append(ALPHABET);
    }

    @Test
    void testFree() {
        byte[] space = new byte[16 * 1024];
        VSPage<String> page = new VSPage<>(StringSerializer.INSTANCE,
                new VSPage.Configuration(space, 0, 2048, space.length - 2048));
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
            for (int j = i; j < Math.min(i + 10, space.length); j++) {
                if ((space[j] >= 'A' && space[j] <= 'Z') || (space[j] >= 'a' && space[j] <= 'z'))
                    System.out.printf("%6s", (char) space[j]);
                else System.out.printf("%6d", space[j]);
            }
            System.out.printf("|%-6d%n", count++);
        }
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