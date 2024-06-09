package com.yokalona.array.serializers.primitives;

import com.yokalona.array.serializers.FixedSizeSerializer;

public class IntegerSerializer implements FixedSizeSerializer<Integer> {
    public static final IntegerSerializer INSTANCE = new IntegerSerializer();
    public static final int SIZE = Integer.BYTES;

    @Override
    public int
    serialize(Integer value, byte[] bytes, int offset) {
        if (value == null) bytes[offset] = (byte) 0xF;
        else {
            bytes[offset++] = 0x0;
            serializeCompact(value, SIZE, bytes, offset);
        }
        return sizeOf();
    }

    @Override
    public Integer
    deserialize(byte[] bytes, int offset) {
        if (bytes[offset ++] == 0xF) return null;
        return deserializeCompact(bytes, offset);
    }

    public int
    serializeCompact(int value, byte[] bytes, int offset) {
        return serializeCompact(value, SIZE, bytes, offset);
    }

    public int
    serializeCompact(int value, int length, byte[] bytes, int offset) {
        for (int position = length - 1; position >= 0; position--) {
            bytes[offset + position] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return length;
    }

    public int
    deserializeCompact(byte[] bytes, int offset) {
        return deserializeCompact(SIZE, bytes, offset);
    }

    public int
    deserializeCompact(int length, byte[] bytes, int offset) {
        int value = 0;
        for (int index = offset; index < offset + length; index ++) {
            value = (value << 8) + (bytes[index] & 0xFF);
        }
        return value;
    }

    @Override
    public int sizeOf() {
        return SIZE + 1;
    }
}
