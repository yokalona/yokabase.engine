package com.yokalona.file;

public class OffsetException extends RuntimeException {
    public OffsetException(int offset, int space) {
        super("Offset %d is outside of space %d".formatted(offset, space));
    }
}
