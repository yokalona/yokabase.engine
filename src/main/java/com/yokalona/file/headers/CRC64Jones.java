package com.yokalona.file.headers;

public class CRC64Jones {

    private static final long POLY = 0x95b3d9e635c6e57bL;

    public static long
    calculate(byte[] space, int start, int end) {
        long crc = 0L;
        for (int index = start; index < end; index++) {
            crc ^= space[index];
            for (int i = 0; i < 8; i++) {
                boolean bit = (crc & 0x8000000000000000L) != 0;
                crc <<= 1;
                if (bit) crc ^= POLY;
            }
        }
        return crc;
    }
}
