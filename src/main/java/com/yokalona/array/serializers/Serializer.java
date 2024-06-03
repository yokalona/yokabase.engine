package com.yokalona.array.serializers;

public sealed interface Serializer<Type>
        permits FixedSizeSerializer, VariableSizeSerializer {
    byte[] serialize(Type value);
    void serialize(Type type, byte[] data, int offset);
    Type deserialize(byte[] bytes, int offset);
}
