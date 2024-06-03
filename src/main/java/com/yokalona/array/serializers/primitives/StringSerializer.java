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
    public void
    serialize(String s, byte[] data, int offset) {
        IntegerSerializer.INSTANCE.serializeCompact(s.length(), data, offset);
        System.arraycopy(s.getBytes(StandardCharsets.UTF_8), 0, data,
                offset + IntegerSerializer.SIZE, s.length());
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
