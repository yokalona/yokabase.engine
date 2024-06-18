package com.yokalona.file;

public interface Cache<Type> extends Iterable<Type> {
    Type get(int index);
    int length();
}
