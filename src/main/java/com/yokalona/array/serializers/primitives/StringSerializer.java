package com.yokalona.array.serializers.primitives;

import com.yokalona.array.serializers.VariableSizeSerializer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class StringSerializer implements VariableSizeSerializer<String> {

    public static final StringSerializer INSTANCE = new StringSerializer();

    @Override
    public byte[]
    serialize(String value) {
        byte[] buffer = new byte[value.length() + IntegerSerializer.INSTANCE.sizeOf()];
        serialize(value, buffer, 0);
        return buffer;
    }

    @Override
    public int
    serialize(String s, byte[] data, int offset) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int length = IntegerSerializer.INSTANCE.serializeCompact(bytes.length, data, offset);
        System.arraycopy(bytes, 0, data, offset + IntegerSerializer.SIZE, s.length());
        return length + bytes.length;
    }

    @Override
    public String
    deserialize(byte[] bytes, int offset) {
        int length = IntegerSerializer.INSTANCE.deserializeCompact(bytes, offset);
        offset += IntegerSerializer.SIZE;
        return new String(Arrays.copyOfRange(bytes, offset, offset + length));
    }

    @Override
    public int
    sizeOf(String value) {
        return IntegerSerializer.SIZE + value.length();
    }
}
