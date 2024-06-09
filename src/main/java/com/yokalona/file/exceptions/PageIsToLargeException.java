package com.yokalona.file.exceptions;

public class PageIsToLargeException extends RuntimeException {
    public PageIsToLargeException(int size) {
        super(String.format("Page size of %d is too large.", size));
    }
}
