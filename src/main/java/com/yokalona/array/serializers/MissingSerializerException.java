package com.yokalona.array.serializers;

public class MissingSerializerException extends RuntimeException {
    public MissingSerializerException(Class<?> clazz) {
        super(clazz.getCanonicalName());
    }
}
