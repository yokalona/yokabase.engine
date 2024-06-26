package com.yokalona.file.exceptions;

public class PageIsTooLargeException extends IllegalArgumentException {
    public PageIsTooLargeException(int size) {
        super(String.format("Page size of %d is too large.", size));
    }
}
