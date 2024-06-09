package com.yokalona.file;

import com.yokalona.file.exceptions.NoFreeSpaceAvailableException;
import com.yokalona.file.exceptions.WriteOverflowException;
import com.yokalona.file.page.MergeAvailabilitySpace;
import com.yokalona.tree.TestHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MergeAvailabilitySpaceTest {

    int REPEATS = 10000;

    @Test
    void testCreate() {
        byte[] space = new byte[4 * 1024];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(2 * 1024, 4 * 1024, space, 0);
        assertEquals(2048, availability.available());
    }

    @Test
    void testAlloc() {
        byte[] space = new byte[4 * 1024];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(2 * 1024, 4 * 1024, space, 0);
        assertEquals(2048, availability.available());
        for (int i = 0; i < 1024; i ++) {
            availability.alloc(2);
        }
        assertEquals(0, availability.available());
        assertThrows(NoFreeSpaceAvailableException.class, () -> availability.alloc(1));
    }

    @Test
    void testAllocOrder() {
        byte[] space = new byte[4 * 1024];
        int[] addresses = new int[1024];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(2 * 1024, 4 * 1024, space, 0);
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
//
//    @Test
//    void testReduce() {
//        byte[] space = new byte[4 * 1024];
//        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(2 * 1024, 4 * 1024, space, 0);
//        assertEquals(2048, availability.available());
//        int reduce = availability.reduce(2);
//        assertEquals(2048, reduce);
//        assertEquals(2050, availability.reduce(2));
//        assertEquals(2044, availability.available());
//        int alloc = availability.alloc(0);
//        assertEquals(4096, alloc);
//    }

    @Test
    void testFree() {
        byte[] space = new byte[4 * 1024];
        int[] addresses = new int[1024];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(2 * 1024, 4 * 1024, space, 0);
        assertEquals(2048, availability.available());
        for (int i = 0; i < 1024; i ++) {
            addresses[i] = availability.alloc(2);
        }
        assertEquals(0, availability.available());
        int free = availability.free0(2, addresses[0]);
        assertEquals(1, free);
        free = availability.free0(2, addresses[1]);
        availability.defragmentation();
        assertEquals(1, availability.fragments());
        free = availability.free0(2, addresses[3]);
        assertEquals(2, free);
        free = availability.free0(2, addresses[2]);
        assertEquals(3, free);
        availability.defragmentation();
        assertEquals(1, availability.fragments());
        assertEquals(8, availability.available());
    }

    @Test
    void testFreeSpill() {
        byte[] space = new byte[8 * 1024];
        int[] addresses = new int[1024];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(4 * 1024, 8 * 1024, space, 0);
        assertEquals(4096, availability.available());
        for (int i = 0; i < addresses.length; i ++) {
            addresses[i] = availability.alloc(2);
        }
        assertEquals(2048, availability.available());
        int free, count = 1;
        for (int index = 0; index < addresses.length; index += 2) {
            free = availability.free(1, addresses[index]);
            assertEquals(++ count, free);
        }
        assertThrows(WriteOverflowException.class, () -> availability.free(4096, addresses[1]));
        free = availability.free(2048, addresses[1023]);
        assertEquals(1, free);
        assertEquals(4096, availability.available());
    }

    @Test
    void testRabbitAndTheHat() {
        for (int iteration = 0; iteration < REPEATS; iteration ++) {
            byte[] space = new byte[8 * 1024];
            int[] addresses = new int[512];
            MergeAvailabilitySpace availability = new MergeAvailabilitySpace(4 * 1024, 8 * 1024, space, 0);
            for (int i = 0; i < addresses.length; i++) {
                addresses[i] = availability.alloc(4);
                if (addresses[i] < 0) throw new RuntimeException();
            }
            TestHelper.shuffle(addresses);
            for (int address : addresses) {
                availability.free0(4, address);
            }
            availability.defragmentation();
            assertEquals(1, availability.fragments());
        }
    }
}