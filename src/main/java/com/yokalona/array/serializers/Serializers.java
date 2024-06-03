package com.yokalona.array.serializers;

import com.yokalona.array.serializers.primitives.*;

public class Serializers {

    public static byte[]
    serialize(boolean value) {
        return BooleanSerializer.INSTANCE.serialize(value);
    }

    public static byte[]
    serialize(byte value) {
        return ByteSerializer.INSTANCE.serialize(value);
    }

    public static byte[]
    serialize(short value) {
        return ShortSerializer.INSTANCE.serialize(value);
    }

    public static byte[]
    serialize(int value) {
        return IntegerSerializer.INSTANCE.serialize(value);
    }

    public static byte[]
    serialize(long value) {
        return LongSerializer.INSTANCE.serialize(value);
    }

}
