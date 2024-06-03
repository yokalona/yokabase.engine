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
    public void serialize(Pointer pointer, byte[] data, int offset) {
        serializer.serialize(pointer.length(), data, offset);
        serializer.serialize(pointer.address(), data, offset + significant);
    }

    @Override
    public Pointer deserialize(byte[] bytes, int offset) {
        Integer length = serializer.deserialize(bytes, offset);
        Integer address = serializer.deserialize(bytes, offset + significant);
        return new Pointer(length, address);
    }

    public static PointerSerializer
    forSpace(int size) {
        int significantBytes = AddressSpaceTools.significantBytes(size);
        return switch (significantBytes) {
            case 2 -> INSTANCE_2B;
            case 3 -> INSTANCE_3B;
            case 4 -> INSTANCE_4B;
            default -> throw new RuntimeException("Address space is to large");
        };
    }
}
