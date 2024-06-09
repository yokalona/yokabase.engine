package com.yokalona.file.headers;

import com.yokalona.array.serializers.primitives.LongSerializer;
import com.yokalona.file.Header;

public class CRC64JonesHeader implements Header {

    private static final long POLY = 0x95b3d9e635c6e57bL;
    private final byte[] space;
    private final int offset;

    public CRC64JonesHeader(byte[] space, int offset) {
        this.space = space;
        this.offset = offset;
    }

    @Override
    public int size() {
        return Long.BYTES;
    }

    @Override
    public void update(int start, int end) {
        long crc = 0L;
        for (int index = start; index < end; index++) {
            crc ^= space[index];
            for (int i = 0; i < 8; i++) {
                boolean bit = (crc & 0x8000000000000000L) != 0;
                crc <<= 1;
                if (bit) crc ^= POLY;
            }
        }
        LongSerializer.INSTANCE.serializeCompact(crc, space, offset);
    }
}
