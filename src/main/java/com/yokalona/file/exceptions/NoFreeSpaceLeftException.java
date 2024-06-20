package com.yokalona.file.exceptions;

public class NoFreeSpaceLeftException extends RuntimeException {
    public NoFreeSpaceLeftException() {
        super();
    }
    public NoFreeSpaceLeftException(String message) {
        super(message);
    }
}
