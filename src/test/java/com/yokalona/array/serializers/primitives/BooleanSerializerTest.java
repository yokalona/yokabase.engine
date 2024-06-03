package com.yokalona.array.serializers.primitives;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static com.yokalona.array.serializers.primitives.BooleanSerializer.INSTANCE;
import static org.junit.jupiter.api.Assertions.*;

class BooleanSerializerTest {

    private static final Random RANDOM = new Random();
    private final byte [] buffer = new byte[256];

    @Test
    void testShort() {
        testBothWays(new byte[]{0xF}, null);
        testBothWays(new byte[]{0x0}, false);
        testBothWays(new byte[]{0x1}, true);
    }

    void testBothWays(byte[] bytes, Boolean value) {
        assertArrayEquals(bytes, INSTANCE.serialize(value));
        assertEquals(value, INSTANCE.deserialize(bytes, 0));
        assertArrayEquals(bytes, testOffsetWrite(value));
        assertEquals(value, testOffsetRead(bytes));
    }

    private byte[] testOffsetWrite(Boolean value) {
        int offset = getOffset();
        INSTANCE.serialize(value, buffer, offset);
        return Arrays.copyOfRange(buffer, offset, offset + INSTANCE.sizeOf());
    }

    private Boolean testOffsetRead(byte[] bytes) {
        int offset = getOffset();
        System.arraycopy(bytes, 0, buffer, offset, INSTANCE.sizeOf());
        return INSTANCE.deserialize(buffer, offset);
    }

    private int getOffset() {
        return RANDOM.nextInt(INSTANCE.sizeOf(), buffer.length - INSTANCE.sizeOf());
    }

}