package com.yokalona.array.serializers.primitives;

import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.VariableSizeSerializer;

public class CompactIntegerSerializer implements FixedSizeSerializer<Integer>, VariableSizeSerializer<Integer> {

    private final int significant;

    public CompactIntegerSerializer(int significant) {
        this.significant = significant;
        assert 0 < significant && significant < 5;
    }

    @Override
    public int
    sizeOf() {
        return significant;
    }

    @Override
    public byte[]
    serialize(Integer value) {
        byte[] buffer = new byte[significant];
        serialize(value, buffer, 0);
        return buffer;
    }

    @Override
    public void
    serialize(Integer value, byte[] data, int offset) {
        IntegerSerializer.INSTANCE.serializeCompact(value, significant, data, offset);
    }

    @Override
    public Integer
    deserialize(byte[] bytes, int offset) {
        return IntegerSerializer.INSTANCE.deserializeCompact(significant, bytes, offset);
    }

    @Override
    public int
    sizeOf(Integer value) {
        return significant;
    }
}
