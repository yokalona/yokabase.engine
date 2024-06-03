package com.yokalona.array.serializers.primitives;

import com.yokalona.array.serializers.FixedSizeSerializer;

public class ByteSerializer implements FixedSizeSerializer<Byte> {

    public static final FixedSizeSerializer<Byte> INSTANCE = new ByteSerializer();

    @Override
    public void
    serialize(Byte value, byte[] bytes, int offset) {
        if (value == null) bytes[offset] = 0xF;
        else {
            bytes[offset++] = 0x0;
            bytes[offset] = value;
        }
    }

    @Override
    public Byte
    deserialize(byte[] bytes, int offset) {
        if (bytes[offset ++] == 0xF) return null;
        return bytes[offset];
    }

    @Override
    public int sizeOf() {
        return 2;
    }
}
