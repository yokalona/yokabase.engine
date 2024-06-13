package com.yokalona.file.headers;

import org.junit.jupiter.api.Test;

import java.util.Random;

class CRC64JonesHeaderTest {

    @Test
    void testCalculate() {
        CRC64Jones header = new CRC64Jones();
        byte[] bytes = new byte[1024 * 1024];
        new Random().nextBytes(bytes);
        long time = System.currentTimeMillis();
        long first = header.calculate(bytes, 0, bytes.length);
        System.out.println("end: " + (System.currentTimeMillis() - time));
        byte aByte = bytes[44];
        bytes[44] = 0x7F;
        long second = header.calculate(bytes, 0, bytes.length);
        bytes[44] = aByte;
        long third = header.calculate(bytes, 0, bytes.length);
        System.out.println(first);
        System.out.println(second);
        System.out.println(third);
    }

}