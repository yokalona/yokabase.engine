package com.yokalona.file.headers;

import com.yokalona.array.serializers.primitives.LongSerializer;
import com.yokalona.file.exceptions.CRCMismatchException;

import static com.yokalona.file.headers.CRC64Jones.calculate;

public class CRC implements Header {

    private int offset;

    @Override
    public void
    offset(int offset) {
        this.offset = offset;
    }

    @Override
    public int
    length() {
        return Long.BYTES;
    }

    @Override
    public void
    write(byte[] page, int offset) {
        long crc = calculate(page, this.offset, page.length);
        LongSerializer.INSTANCE.serializeCompact(crc, page, offset);
    }

    @Override
    public void
    read(byte[] page, int offset) {
        long expected = calculate(page, this.offset, page.length);
        long actual = LongSerializer.INSTANCE.deserializeCompact(page, offset);
        if (expected != actual) throw new CRCMismatchException();
    }
}
