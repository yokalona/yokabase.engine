package com.yokalona.array.serializers.primitives;

import com.yokalona.array.serializers.FixedSizeSerializer;

public class LongSerializer implements FixedSizeSerializer<Long> {

    public static final int SIZE = Long.BYTES;
    public static final LongSerializer INSTANCE = new LongSerializer();

    @Override
    public int
    serialize(Long value, byte[] bytes, int offset) {
        if (value == null) bytes[offset] = (byte) 0xF;
        else {
            bytes[offset] = 0x0;
            for (int position = Long.BYTES - 1; position >= 0; position--) {
                bytes[offset + 1 + position] = (byte) (value & 0xFF);
                value >>= 8;
            }
        }
        return sizeOf();
    }

    @Override
    public Long
    deserialize(byte[] bytes, int offset) {
        if (bytes[offset ++] == 0xF) return null;
        long value = 0;
        for (int index = offset; index < offset + SIZE; index ++) {
            value = (value << 8) + (bytes[index] & 0xFF);
        }
        return value;
    }

    public int
    serializeCompact(long value, byte[] bytes, int offset) {
        return serializeCompact(value, SIZE, bytes, offset);
    }

    public int
    serializeCompact(long value, int length, byte[] bytes, int offset) {
        for (int position = length - 1; position >= 0; position--) {
            bytes[offset + position] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return length;
    }

    public long
    deserializeCompact(byte[] bytes, int offset) {
        return deserializeCompact(SIZE, bytes, offset);
    }

    public long
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
