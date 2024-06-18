package com.yokalona.file;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CachedArrayProviderTest {

    public static final Random RANDOM = new Random();

    @Test
    void testRabbitAndTheHat1() {
        for (int i = 1; i < 1000; i++) {
            SneakyArray array = new SneakyArray(generate(i));
            CachedArrayProvider<Integer> cache = new CachedArrayProvider<>(i, array::get);
            for (int index = 0; index < i * 10; index++)
                assertEquals(array.array[index % array.array.length], cache.get(index % array.array.length));
            assertEquals(i, array.gets);
        }
    }

    public static int[]
    generate(int length) {
        int[] array = new int[length];
        for (int i = 0; i < length; i++) array[i] = RANDOM.nextInt();
        return array;
    }

    static final class SneakyArray {
        private final int[] array;
        private int gets;

        SneakyArray(int[] array) {
            this.array = array;
        }

        int
        get(int index) {
            gets++;
            return array[index];
        }
    }

}