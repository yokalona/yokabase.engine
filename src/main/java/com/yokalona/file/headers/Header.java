package com.yokalona.file.headers;

public interface Header {
    int length();

    void write(byte[] page, int offset);

    void read(byte[] page, int offset);

    default void offset(int ignore) {
    }
}
