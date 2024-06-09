package com.yokalona.array.serializers.primitives;

import com.yokalona.array.serializers.FixedSizeSerializer;

public class BooleanSerializer implements FixedSizeSerializer<Boolean> {

    public static final FixedSizeSerializer<Boolean> INSTANCE = new BooleanSerializer();

    @Override
    public int
    serialize(Boolean value, byte[] bytes, int offset) {
        if (value == null) bytes[offset] = 0xF;
        else if (value) bytes[offset] = 0x1;
        else bytes[offset] = 0x0;
        return 1;
    }

    @Override
    public Boolean
    deserialize(byte[] bytes, int offset) {
        return switch (bytes[offset]) {
            case 0x0 -> false;
            case 0x1 -> true;
            default -> null;
        };
    }

    @Override
    public int sizeOf() {
        return 1;
    }
}
