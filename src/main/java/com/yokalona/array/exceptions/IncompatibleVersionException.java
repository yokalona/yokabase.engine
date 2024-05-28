package com.yokalona.array.exceptions;

import com.yokalona.array.PersistentArray;
import com.yokalona.array.serializers.Version;

public class IncompatibleVersionException extends RuntimeException {
    public IncompatibleVersionException(Version version) {
        super("Provided version: %s is not compatible with: %s".formatted(version, PersistentArray.VERSION));
    }
}
