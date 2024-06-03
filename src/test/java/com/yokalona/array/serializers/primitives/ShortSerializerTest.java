package com.yokalona.array.serializers.primitives;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static com.yokalona.array.serializers.primitives.ShortSerializer.INSTANCE;
import static org.junit.jupiter.api.Assertions.*;

class ShortSerializerTest {

    private static final Random RANDOM = new Random();
    private final byte [] buffer = new byte[256];

    @Test
    void testShort() {
        testBothWays(new byte[]{0xF, 0x0, 0x0}, null);
        for (byte number = 0; number < 127; number++) {
            testBothWays(new byte[]{0x0, 0x0, number}, (short) number);
            testBothWays(new byte[]{0x0, number, 0x0}, (short) (number << 8));
        }
        testBothWays(new byte[]{0x0, -0x1, -0x1}, (short) -1);
        testBothWays(new byte[]{0, 0x7F, -0x1}, Short.MAX_VALUE);
        testBothWays(new byte[]{0, -0x80, 0x0}, Short.MIN_VALUE);
    }

    void testBothWays(byte[] bytes, Short value) {
        assertArrayEquals(bytes, INSTANCE.serialize(value));
        assertEquals(value, INSTANCE.deserialize(bytes, 0));
        assertArrayEquals(bytes, testOffsetWrite(value));
        assertEquals(value, testOffsetRead(bytes));
    }

    private byte[] testOffsetWrite(Short value) {
        int offset = getOffset();
        INSTANCE.serialize(value, buffer, offset);
        return Arrays.copyOfRange(buffer, offset, offset + INSTANCE.sizeOf());
    }

    private Short testOffsetRead(byte[] bytes) {
        int offset = getOffset();
        System.arraycopy(bytes, 0, buffer, offset, INSTANCE.sizeOf());
        return INSTANCE.deserialize(buffer, offset);
    }

    private int getOffset() {
        return RANDOM.nextInt(INSTANCE.sizeOf(), buffer.length - INSTANCE.sizeOf());
    }

}