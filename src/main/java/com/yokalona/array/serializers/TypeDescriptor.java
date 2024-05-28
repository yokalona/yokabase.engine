package com.yokalona.array.serializers;

public record TypeDescriptor<Type>(int size, Class<Type> type) {
}
