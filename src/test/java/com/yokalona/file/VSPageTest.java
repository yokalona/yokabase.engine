package com.yokalona.file;

import com.yokalona.array.serializers.primitives.StringSerializer;
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
    void testCreation() {
        byte[] space = new byte[16 * 1024];
        VSPage<String> page = new VSPage.Configurer<>(StringSerializer.INSTANCE)
                .on(space, 0)
                .configure();
        assertEquals(0, page.size());
        assertEquals(2, page.occupied());
        assertEquals(16 * 1024 - (int) (16 * 1024 * .1) - 2, page.free());
    }

    @Test
    void testAppend() {
        byte[] space = new byte[16 * 1024];
        VSPage<String> page = new VSPage.Configurer<>(StringSerializer.INSTANCE)
                .on(space, 0)
                .configure();
        List<String> expected = new ArrayList<>();
        String string = randomString(3, 100);
        while (page.fits(StringSerializer.INSTANCE.sizeOf(string))) {
            page.append(string);
            expected.add(string);
            randomString(3, 100);
        }
        assertEquals(expected.size(), page.size());
        for (int index = 0; index < expected.size(); index++) {
            assertEquals(expected.get(index), page.get(index));
        }
        prettyPrint(space);
    }

    @Test
    void testRemove() {
        byte[] space = new byte[4 * 1024];
        VSPage<String> page = new VSPage.Configurer<>(StringSerializer.INSTANCE)
                .on(space, 0)
                .configure();
        List<String> expected = new ArrayList<>();
        String string = randomString(3, 100);
        while (page.fits(StringSerializer.INSTANCE.sizeOf(string))) {
            page.append(string);
            expected.add(string);
            randomString(3, 100);
        }
        string = randomString(50, 100);
        int size = StringSerializer.INSTANCE.sizeOf(string);
        while (!page.fits(size)) {
            int index = RANDOM.nextInt(page.size());
            page.remove(index);
            expected.remove(index);
        }

        for (int index = 0; index < expected.size(); index++) {
            assertEquals(expected.get(index), page.get(index));
        }
        assertEquals(expected.size(), page.size());

//        string = randomString(3, 10);

        page.append(string);
        expected.add(string);
//        prettyPrint(space);
        for (int index = 0; index < expected.size(); index++) {
            assertEquals(expected.get(index), page.get(index));
        }
        prettyPrint(space);
    }

    private static void
    prettyPrint(byte[] space) {
        System.out.printf("%3s%-3s|", "-", "-");
        for (int i = 0; i < 10; i ++) {
            System.out.printf("%6d", i);
        }
        System.out.println("\n------+------------------------------------------------------------+------");
        int count = 0;
        for (int i = 0; i < space.length; i += 10) {
            System.out.printf("%6d|", count);
            for (int j = i; j < Math.min(i + 10, space.length); j ++) {
                if (space[j] < 'a') System.out.printf("%6d", space[j]);
                else System.out.printf("%6s", (char) space[j]);
            }
            System.out.printf("|%-6d%n", count ++);
        }
        System.out.printf("%3s%-3s|", "-", "-");
        for (int i = 0; i < 10; i ++) {
            System.out.printf("%6d", i);
        }
        System.out.println();
    }

    String
    randomString(int min, int max) {
        byte [] bytes = new byte[RANDOM.nextInt(min, max)];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

}