package com.yokalona.array.serializers.primitives;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static com.yokalona.array.serializers.primitives.ByteSerializer.INSTANCE;
import static org.junit.jupiter.api.Assertions.*;

class ByteSerializerTest {

    private static final Random RANDOM = new Random();
    private final byte [] buffer = new byte[256];

    @Test
    void testByte() {
        testBothWays(new byte[]{0xF, 0x0}, null);
        for (byte number = 0; number < 127; number++) {
            testBothWays(new byte[]{0x0, number}, number);
        }
        testBothWays(new byte[]{0x0, -0x1}, (byte) -1);
        testBothWays(new byte[]{0, 0x7F}, Byte.MAX_VALUE);
        testBothWays(new byte[]{0, -0x80}, Byte.MIN_VALUE);
    }

    void testBothWays(byte[] bytes, Byte value) {
        assertArrayEquals(bytes, INSTANCE.serialize(value));
        assertEquals(value, INSTANCE.deserialize(bytes, 0));
        assertArrayEquals(bytes, testOffsetWrite(value));
        assertEquals(value, testOffsetRead(bytes));
    }

    private byte[] testOffsetWrite(Byte value) {
        int offset = getOffset();
        INSTANCE.serialize(value, buffer, offset);
        return Arrays.copyOfRange(buffer, offset, offset + INSTANCE.sizeOf());
    }

    private Byte testOffsetRead(byte[] bytes) {
        int offset = getOffset();
        System.arraycopy(bytes, 0, buffer, offset, INSTANCE.sizeOf());
        return INSTANCE.deserialize(buffer, offset);
    }

    private int getOffset() {
        return RANDOM.nextInt(INSTANCE.sizeOf(), buffer.length - INSTANCE.sizeOf());
    }
}