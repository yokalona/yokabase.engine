package com.yokalona.file.exceptions;

public class SpaceDistributionIsInvalidException extends IllegalArgumentException {

    public SpaceDistributionIsInvalidException(float distribution) {
        super(String.format("Space distribution %f is outside of range (0, 1)", distribution));
    }
}
