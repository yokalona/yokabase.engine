package com.yokalona.array.serializers;

public interface Serializer<Type> {
    byte[] serialize(Type type);
    Type deserialize(byte[] bytes);
    Type deserialize(byte[] bytes, int offset);
    TypeDescriptor<Type> descriptor();
}
