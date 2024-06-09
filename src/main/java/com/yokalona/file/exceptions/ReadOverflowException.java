package com.yokalona.file.exceptions;

public class ReadOverflowException extends RuntimeException {
    public ReadOverflowException(int size, int index) {
        super("Attempted to read index %d which outside of page borders [0, %d)".formatted(index, size));
    }
}
