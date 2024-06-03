package com.yokalona.array.serializers;

import com.yokalona.array.serializers.primitives.IntegerSerializer;

public non-sealed interface VariableSizeSerializer<Type> extends Serializer<Type> {
    int sizeOf(Type value);
    default int sizeOf(byte[] data, int offset) {
        return IntegerSerializer.INSTANCE.deserializeCompact(data, offset);
    }
}
