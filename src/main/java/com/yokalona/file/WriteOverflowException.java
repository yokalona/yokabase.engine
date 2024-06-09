package com.yokalona.file;

public class WriteOverflowException extends RuntimeException {
    public WriteOverflowException(int size) {
        super(String.format("Attempted to append to the end of page, however, only %d bytes free left", size));
    }

    public WriteOverflowException(int size, int index) {
        super(String.format("Attempted to write to index %d outside of page borders [0, %d)", index, size));
    }

    public WriteOverflowException(String message) {
        super(message);
    }
}
