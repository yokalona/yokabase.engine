package com.yokalona.array.serializers.primitives;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static com.yokalona.array.serializers.primitives.IntegerSerializer.INSTANCE;
import static org.junit.jupiter.api.Assertions.*;

class IntegerSerializerTest {

    private static final Random RANDOM = new Random();
    private final byte [] buffer = new byte[256];

    @Test
    void testInteger() {
        testBothWays(new byte[]{0xF, 0x0, 0x0, 0x0, 0x0}, null);
        for (byte number = 0; number < 127; number++) {
            testBothWays(new byte[]{0x0, 0x0, 0x0, 0x0, number}, (int) number);
            testBothWays(new byte[]{0x0, 0x0, 0x0, number, 0x0}, (int) number << 8);
            testBothWays(new byte[]{0x0, 0x0, number, 0x0, 0x0}, (int) number << 16);
            testBothWays(new byte[]{0x0, number, 0x0, 0x0, 0x0}, (int) number << 24);
        }
        testBothWays(new byte[]{0x0, -0x1, -0x1, -0x1, -0x1}, -1);
        testBothWays(new byte[]{0, 0x7F, -0x01, -0x01, -0x01}, Integer.MAX_VALUE);
        testBothWays(new byte[]{0, -0x80, 0, 0, 0}, Integer.MIN_VALUE);
    }

    void testBothWays(byte[] bytes, Integer value) {
        assertArrayEquals(bytes, INSTANCE.serialize(value));
        assertEquals(value, INSTANCE.deserialize(bytes, 0));
        assertArrayEquals(bytes, testOffsetWrite(value));
        assertEquals(value, testOffsetRead(bytes));
    }

    private byte[] testOffsetWrite(Integer value) {
        int offset = getOffset();
        INSTANCE.serialize(value, buffer, offset);
        return Arrays.copyOfRange(buffer, offset, offset + INSTANCE.sizeOf());
    }

    private Integer testOffsetRead(byte[] bytes) {
        int offset = getOffset();
        System.arraycopy(bytes, 0, buffer, offset, INSTANCE.sizeOf());
        return INSTANCE.deserialize(buffer, offset);
    }

    private int getOffset() {
        return RANDOM.nextInt(INSTANCE.sizeOf(), buffer.length - INSTANCE.sizeOf());
    }
}