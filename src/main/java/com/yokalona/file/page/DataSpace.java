package com.yokalona.file.page;

import com.yokalona.file.Array;

public interface DataSpace<Type> {

    byte pointerSize();

    Type get(int index);

    int address(int index);

    void set(int index, Type value);

    int insert(int address, Type value);

    int remove(int index);

    int size();

    Array<Type> read(Class<Type> type);

    Array<Integer> addresses();

    void clear();

    int occupied();

    void flush();

}
