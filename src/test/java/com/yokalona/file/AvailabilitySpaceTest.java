package com.yokalona.file;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AvailabilitySpaceTest {

    @Test
    void testCreate() {
        byte[] space = new byte[4 * 1024];
        AvailabilitySpace availability = new AvailabilitySpace(2 * 1024, 4 * 1024, space, 0);
        assertEquals(2048, availability.available());
    }

    @Test
    void testAlloc() {
        byte[] space = new byte[4 * 1024];
        AvailabilitySpace availability = new AvailabilitySpace(2 * 1024, 4 * 1024, space, 0);
        assertEquals(2048, availability.available());
        for (int i = 0; i < 1024; i ++) {
            availability.alloc(2);
        }
        assertEquals(0, availability.available());
        assertTrue(availability.alloc(1) < 0);
    }

    @Test
    void testAllocOrder() {
        byte[] space = new byte[4 * 1024];
        int[] addresses = new int[1024];
        AvailabilitySpace availability = new AvailabilitySpace(2 * 1024, 4 * 1024, space, 0);
        assertEquals(2048, availability.available());
        for (int i = 0; i < 1024; i ++) {
            addresses[i] = availability.alloc(2);
        }
        int start = addresses[0];
        for (int i = 1; i < 1024; i ++) {
            assertTrue(start > addresses[i]);
            start = addresses[i];
        }
    }

    @Test
    void testFree() {
        byte[] space = new byte[4 * 1024];
        int[] addresses = new int[1024];
        AvailabilitySpace availability = new AvailabilitySpace(2 * 1024, 4 * 1024, space, 0);
        assertEquals(2048, availability.available());
        for (int i = 0; i < 1024; i ++) {
            addresses[i] = availability.alloc(2);
        }
        assertEquals(0, availability.available());
        int free = availability.free(2, addresses[44]);
        assertEquals(1, free);
        free = availability.free(2, addresses[45]);
        assertEquals(1, free);
        free = availability.free(2, addresses[47]);
        assertEquals(2, free);
        free = availability.free(2, addresses[46]);
        assertEquals(1, free);
        assertEquals(8, availability.available());
    }

}