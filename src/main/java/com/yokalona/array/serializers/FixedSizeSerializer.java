package com.yokalona.array.serializers;

public non-sealed interface FixedSizeSerializer<Type> extends Serializer<Type> {
    int sizeOf();

    default byte[] serialize(Type value) {
        byte[] bytes = new byte[sizeOf()];
        serialize(value, bytes, 0);
        return bytes;
    }
}
