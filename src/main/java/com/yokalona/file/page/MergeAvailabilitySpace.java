package com.yokalona.file.page;

import com.yokalona.annotations.PerformanceImpact;
import com.yokalona.annotations.SpawnSubprocess;
import com.yokalona.file.exceptions.NoFreeSpaceAvailableException;
import com.yokalona.file.Pointer;
import com.yokalona.file.serializers.PointerSerializer;
import com.yokalona.file.exceptions.WriteOverflowException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class MergeAvailabilitySpace {
    private int start;
    private final int border;
    private final ASPage<Pointer> pointers;

    /**
     * Creates availability address space, the structure that controls blocks of bytes ready to be written to.
     *
     * @param size   of allocated space inside total space to be taken by availability space
     * @param total  address space
     * @param space  actual space represented as a byte array
     * @param offset within actual space
     */
    public MergeAvailabilitySpace(int size, int total, byte[] space, int offset) {
        this.border = total;
        this.pointers = new ASPage<>(PointerSerializer.forSpace(total), new ASPage.Configuration(space, offset, size));
        this.pointers.append(new Pointer(this.start = offset + size, total));
    }

    public int
    alloc(int size) {
        int address = allocate(size, false);
        if (address < 0) {
            defragmentation();
            address = allocate(size, false);
        }
        return address;
    }

    public boolean
    fits(int size) {
        return allocate(size, true) > 0;
    }

    public void
    reduce(int by) {
        start += by;
    }

    public int
    free0(int size, int address) {
        if (address < start) throw new WriteOverflowException("");
        if (address + size > border) throw new WriteOverflowException("Freeing more memory, that is accessible");
        Pointer pointer = new Pointer(address, address + size);
        int index = this.pointers.find(pointer, Comparator.comparingInt(Pointer::start));
        if (index < 0) {
            if (this.pointers.spills()) defragmentation();
            this.pointers.insert(-(index + 1), pointer);
        } else this.pointers.set(index, pointer);
        assert assertOrder();
        return this.pointers.size();
    }

    void
    free(int end) {
        this.pointers.clear();
        this.pointers.append(new Pointer(start, end));
    }

    public int
    free(int size, int address) {
        if (address + size > border) throw new WriteOverflowException("Freeing more memory, that is accessible");
        Pointer pointer = new Pointer(address, address + size);
        Pointer[] pointers = this.pointers.read(Pointer.class);
        List<Pointer> merged = new ArrayList<>();
        int index = 0;
        while (index < pointers.length && pointer.start() > pointers[index].end()) merged.add(pointers[index++]);
        while (index < pointers.length && overlaps(pointer, pointers[index]))
            pointer = new Pointer(Math.min(pointer.start(), pointers[index].start()),
                    Math.max(pointer.end(), pointers[index++].end()));
        merged.add(pointer);
        while (index < pointers.length) merged.add(pointers[index++]);
        this.pointers.clear();
        for (Pointer p : merged) {
            if (this.pointers.spills()) defragmentation();
            this.pointers.append(p);
        }
        assert assertOrder();
        return this.pointers.size();
    }

    public int
    available() {
        return Arrays.stream(this.pointers.read(Pointer.class))
                .mapToInt(Pointer::length)
                .sum();
    }

    public int
    fragments() {
        return this.pointers.size();
    }

    @SpawnSubprocess
    public void
    defragmentation() {
        Pointer[] pointers = this.pointers.read(Pointer.class);
        if (pointers.length == 0) throw new NoFreeSpaceAvailableException();
        LinkedList<Pointer> merged = new LinkedList<>();
        merged.add(pointers[0]);
        for (int i = 1; i < pointers.length; i++) {
            Pointer other = pointers[i];
            if (merged.getLast().end() < other.start()) merged.add(other);
            else {
                Pointer pointer = merged.removeLast();
                int end = Math.max(other.end(), pointer.end());
                merged.add(new Pointer(pointer.start(), end));
            }
        }
        this.pointers.clear();
        for (Pointer p : merged) this.pointers.append(p);
    }

    public int
    maxAddress() {
        return border;
    }

    @PerformanceImpact
    private boolean
    assertOrder() {
        Pointer[] pointers = this.pointers.read(Pointer.class);
        if (pointers.length < 1) return true;
        int start = pointers[0].start();
        for (int i = 1; i < pointers.length; i++) {
            Pointer pointer = pointers[i];
            if (pointer.start() < start) return false;
            start = pointer.start();
        }
        return true;
    }

    private int
    allocate(int size, boolean intermediate) {
        int selected = -1, delta = Integer.MAX_VALUE;
        Pointer[] pointers = this.pointers.read(Pointer.class);
        for (int index = 0; index < pointers.length; index++) {
            if (pointers[index].length() >= size && delta > pointers[index].length() - size)
                delta = pointers[selected = index].length() - size;
            if (delta == 0) break;
        }
        if (selected < 0) return -1;
        else if (intermediate) return 1;
        if (delta != 0) this.pointers.set(selected, pointers[selected].adjust(+0, -size));
        else this.pointers.remove(selected);
        return pointers[selected].end() - size;
    }

    private static boolean
    overlaps(Pointer pointer, Pointer pointers) {
        return pointer.start() <= pointers.end() && pointers.start() <= pointer.end();
    }
}
