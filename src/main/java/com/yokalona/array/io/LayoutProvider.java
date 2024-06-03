package com.yokalona.array.io;

import com.yokalona.array.serializers.FixedSizeSerializer;

import java.io.InputStream;

public interface LayoutProvider {
    DataLayout provide(FixedSizeSerializer<?> descriptor);

    static LayoutProvider
    which(byte format, InputStream input) {
        return switch (format & 0b0000011) {
            case 1 -> FixedObjectLayout::new;
            default -> throw new UnsupportedOperationException();
        };
    }
}
