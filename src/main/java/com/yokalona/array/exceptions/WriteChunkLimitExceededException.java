package com.yokalona.array.exceptions;

public class WriteChunkLimitExceededException extends RuntimeException {
    public WriteChunkLimitExceededException() {
        super("Write chunk cannot be bigger than configured memory limit");
    }
}
