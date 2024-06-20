package com.yokalona.file.headers;

import com.yokalona.array.serializers.FixedSizeSerializer;

import java.util.Objects;

public class Fixed<Type> implements Header {

    private Type value;
    private final FixedSizeSerializer<Type> serializer;

    public Fixed(FixedSizeSerializer<Type> serializer) {
        this.serializer = serializer;
    }

    public Fixed(Type value, FixedSizeSerializer<Type> serializer) {
        this.value = value;
        this.serializer = serializer;
    }

    @Override
    public int
    length() {
        return serializer.sizeOf();
    }

    @Override
    public void
    write(byte[] page, int offset) {
        serializer.serialize(value, page, offset);
    }

    @Override
    public void
    read(byte[] page, int offset) {
        Type actual = serializer.deserialize(page, offset);
        if (value == null) value = actual;
        else if (!Objects.equals(actual, value)) throw new RuntimeException();
    }

    public void
    value(Type value) {
        this.value = value;
    }

    public Type
    value() {
        return value;
    }
}
