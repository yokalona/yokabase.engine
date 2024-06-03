package com.yokalona.array.debug;

import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.primitives.IntegerSerializer;

public record CompactInteger(int value) {

    public static final FixedSizeSerializer<CompactInteger> serializer = new CompactIntegerSerializer();

    public static CompactInteger
    compact(int value) {
        return new CompactInteger(value);
    }

    private static class CompactIntegerSerializer implements FixedSizeSerializer<CompactInteger> {

        @Override
        public void
        serialize(CompactInteger value, byte[] data, int offset) {
            if (value == null) data[offset] = 0xF;
            else IntegerSerializer.INSTANCE.serialize(value.value, data, offset);
        }

        @Override
        public CompactInteger
        deserialize(byte[] bytes, int offset) {
            Integer value = IntegerSerializer.INSTANCE.deserialize(bytes, offset);
            if (value == null) return null;
            return new CompactInteger(value);
        }

        @Override
        public int
        sizeOf() {
            return IntegerSerializer.INSTANCE.sizeOf();
        }
    }

}
