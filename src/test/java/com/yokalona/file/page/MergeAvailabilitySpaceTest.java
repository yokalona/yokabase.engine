package com.yokalona.file.page;

import com.yokalona.file.exceptions.NoFreeSpaceLeftException;
import com.yokalona.file.exceptions.WriteOverflowException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.yokalona.file.page.MergeAvailabilitySpace.*;
import static org.junit.jupiter.api.Assertions.*;

class MergeAvailabilitySpaceTest {

    int REPEATS = 1;

    @Test
    void testCreate() {
        byte[] space = new byte[4 * 1024];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(new Configuration(space, 0, 2048, space.length));//, new ASPage.Configuration(space, 0, 2048)));
        assertEquals(2048, availability.available());
    }

    @Test
    void testAlloc() {
        byte[] space = new byte[4 * 1024];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(new Configuration(space, 0, 2048, space.length));
        assertEquals(2048, availability.available());
        for (int i = 0; i < 1024; i ++) {
            availability.alloc(2);
        }
        assertEquals(0, availability.available());
        assertThrows(NoFreeSpaceLeftException.class, () -> availability.alloc(1));
    }

    @Test
    void testAllocOrder() {
        byte[] space = new byte[4 * 1024];
        int[] addresses = new int[1024];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(new Configuration(space, 0, 2048, space.length));
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
    void testAllocFragmentation() {
        byte[] space = new byte[4 * 1024];
        int[] addresses = new int[256];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(new Configuration(space, 0, 2048, space.length));
        assertEquals(2048, availability.available());
        for (int i = 0; i < addresses.length; i ++) {
            addresses[i] = availability.alloc(8);
        }
        for (int i = 0; i < addresses.length; i += 2) {
            availability.free0(8, addresses[i]);
        }
        assertEquals(1024, availability.available());
        assertEquals(-1, availability.alloc(24));
        availability.free0(8, addresses[1]);
        assertTrue(availability.alloc(24) >= 0);
    }

    @Test
    void testFits() {
        byte[] space = new byte[4 * 1024];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(new Configuration(space, 0, 2048, space.length));
        assertEquals(2048, availability.available());
        assertTrue(availability.fits(8));
        while (availability.fits(8)) {
            availability.alloc(8);
        }
        assertFalse(availability.fits(8));
        availability.free0(8, 4040);
        assertTrue(availability.fits(8));
    }

    @Test
    void testReduce() {
        byte[] space = new byte[4 * 1024];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(new Configuration(space, 0, 2048, space.length));
        assertEquals(2048, availability.available());
        availability.reduce(2);
        assertEquals(2046, availability.available());
        availability.reduce(-4);
        assertEquals(2050, availability.available());
        int alloc = availability.alloc(0);
        assertEquals(4096, alloc);
    }

    @Test
    void testReduceThrows() {
        byte[] space = new byte[4 * 1024];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(new Configuration(space, 0, 2048, space.length));
        assertEquals(2048, availability.available());
        assertTrue(availability.fits(8));
        while (availability.fits(8)) {
            availability.alloc(8);
        }
        assertThrows(NoFreeSpaceLeftException.class, () -> availability.reduce(+2));
    }

    @Test
    void testFree() {
        byte[] space = new byte[4 * 1024];
        int[] addresses = new int[1024];
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(new Configuration(space, 0, 2048, space.length));
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
        MergeAvailabilitySpace availability = new MergeAvailabilitySpace(new Configuration(space, 0, 4096, space.length));
        assertEquals(space.length - 4096, availability.available());
        for (int i = 0; i < addresses.length; i ++) {
            addresses[i] = availability.alloc(2);
        }
        assertEquals(2048, availability.available());
        int free, count = 1, memory = 2048;
        for (int index = 0; index < addresses.length; index += 2) {
            free = availability.freeImmediately(2, addresses[index]);
            assertEquals(++ count, free);
            assertEquals(memory += 2, availability.available());
        }
        assertThrows(WriteOverflowException.class, () -> availability.freeImmediately(4096, addresses[1]));
        free = availability.freeImmediately(2048, addresses[1023]);
        assertEquals(1, free);
        assertEquals(4096, availability.available());
    }

    @Test
    void testFreeThrows() {
        byte[] space = new byte[8 * 1024];
        int[] addresses = new int[1024];
        MergeAvailabilitySpace availability =
                new MergeAvailabilitySpace(new Configuration(space, 0, 2048, space.length));
        assertEquals(space.length - 2048, availability.available());
        for (int i = 0; i < addresses.length; i ++) {
            addresses[i] = availability.alloc(2);
        }
        assertThrows(WriteOverflowException.class, () -> availability.free0(8, 1024));
        assertThrows(WriteOverflowException.class, () -> availability.free0(8 * 1024, addresses[0]));
    }

    @Test
    void testRabbitAndTheHatFree0() {
        for (int iteration = 0; iteration < REPEATS; iteration ++) {
            System.out.println(iteration);
            byte[] space = new byte[8 * 1024];
            List<Integer> addresses = new ArrayList<>();
            MergeAvailabilitySpace availability =
                    new MergeAvailabilitySpace(new Configuration(space, 0, 4096, space.length));
            while (availability.fits(4)) {
                addresses.add(availability.alloc(4));
                if (addresses.getLast() < 0) throw new RuntimeException();
            }
            Collections.shuffle(addresses);
            for (int address : addresses) {
                availability.free0(4, address);
            }
            availability.defragmentation();
            assertEquals(1, availability.fragments());
        }
    }

    @Test
    void testRabbitAndTheHatFree() {
        for (int iteration = 0; iteration < REPEATS; iteration ++) {
            System.out.println(iteration);
            byte[] space = new byte[8 * 1024];
            List<Integer> addresses = new ArrayList<>();
            MergeAvailabilitySpace availability =
                    new MergeAvailabilitySpace(new Configuration(space, 0, 4096, space.length));
            while (availability.fits(4)) {
                addresses.add(availability.alloc(4));
                if (addresses.getLast() < 0) throw new RuntimeException();
            }
            Collections.shuffle(addresses);
            for (int address : addresses) {
                availability.freeImmediately(4, address);
            }
            availability.defragmentation();
            assertEquals(1, availability.fragments());
        }
    }
}