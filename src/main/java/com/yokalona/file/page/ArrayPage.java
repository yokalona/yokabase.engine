package com.yokalona.file.page;

import java.util.Comparator;

public interface ArrayPage<Type> extends Page<Type>, Iterable<Type> {
    void set(int index, Type value);

    void insert(int index, Type value);

    void swap(int left, int right);

    int find(Type value, Comparator<Type> comparator);

    Type first();

    Type last();

    int length();
}
