package com.yokalona.file;

public interface Header {
    /**
     * @return size of header in bytes
     */
    int size();

    default void update(int start, int end) {}
}
