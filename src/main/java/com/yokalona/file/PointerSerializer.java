package com.yokalona.file;

import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.VariableSizeSerializer;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;

public class PointerSerializer implements FixedSizeSerializer<Pointer> {
    public static PointerSerializer INSTANCE_2B = new PointerSerializer(2);
    public static PointerSerializer INSTANCE_3B = new PointerSerializer(3);
    public static PointerSerializer INSTANCE_4B = new PointerSerializer(4);

    private final int significant;
    private final VariableSizeSerializer<Integer> serializer;

    private PointerSerializer(int significant) {
        this.significant = significant;
        assert 1 < significant && significant < 5;
        this.serializer = new CompactIntegerSerializer(significant);
    }

    @Override
    public int sizeOf() {
        return significant * 2;
    }

    @Override
    public byte[] serialize(Pointer value) {
        byte[] buffer = new byte[significant];
        serialize(value, buffer, 0);
        return buffer;
    }

    @Override
    public int serialize(Pointer pointer, byte[] data, int offset) {
        int length = serializer.serialize(pointer.start(), data, offset);
        return length + serializer.serialize(pointer.end(), data, offset + significant);
    }

    @Override
    public Pointer deserialize(byte[] bytes, int offset) {
        Integer start = serializer.deserialize(bytes, offset);
        Integer end = serializer.deserialize(bytes, offset + significant);
        return new Pointer(start, end);
    }

    public static PointerSerializer
    forSpace(int size) {
        byte significantBytes = AddressTools.significantBytes(size);
        return switch (significantBytes) {
            case 2 -> INSTANCE_2B;
            case 3 -> INSTANCE_3B;
            case 4 -> INSTANCE_4B;
            default -> throw new RuntimeException("Address space is to large");
        };
    }
}
