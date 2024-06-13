package com.yokalona.array.serializers.primitives;

import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.VariableSizeSerializer;

public class CompactLongSerializer implements FixedSizeSerializer<Long>, VariableSizeSerializer<Long> {

    private final int significant;

    public CompactLongSerializer(int significant) {
        this.significant = significant;
    }

    @Override
    public int
    sizeOf() {
        return significant;
    }

    @Override
    public byte[]
    serialize(Long value) {
        byte[] buffer = new byte[significant];
        serialize(value, buffer, 0);
        return buffer;
    }

    @Override
    public int
    serialize(Long value, byte[] data, int offset) {
        return LongSerializer.INSTANCE.serializeCompact(value, significant, data, offset);
    }

    @Override
    public Long
    deserialize(byte[] bytes, int offset) {
        return LongSerializer.INSTANCE.deserializeCompact(significant, bytes, offset);
    }

    @Override
    public int
    sizeOf(Long value) {
        return significant;
    }
}
