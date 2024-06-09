package com.yokalona.array.serializers.primitives;

import com.yokalona.array.serializers.FixedSizeSerializer;

public class ShortSerializer implements FixedSizeSerializer<Short> {

    public static final FixedSizeSerializer<Short> INSTANCE = new ShortSerializer();
    public static final int SIZE = Short.BYTES;

    @Override
    public int
    serialize(Short value, byte[] bytes, int offset) {
        if (value == null) bytes[offset] = (byte) 0xF;
        else {
            bytes[offset] = 0x0;
            for (int position = Short.BYTES - 1; position >= 0; position--) {
                bytes[offset + 1 + position] = (byte) (value & 0xFF);
                value = (short) (value >> 8);
            }
        }
        return sizeOf();
    }

    @Override
    public Short
    deserialize(byte[] bytes, int offset) {
        if (bytes[offset++] == 0xF) return null;
        short value = 0;
        for (int index = offset; index < offset + SIZE; index++) {
            value = (short) ((value << 8) + (short) (bytes[index] & 0xFF));
        }
        return value;
    }

    @Override
    public int sizeOf() {
        return SIZE + 1;
    }
}
