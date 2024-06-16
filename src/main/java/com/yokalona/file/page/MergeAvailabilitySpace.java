package com.yokalona.file.page;

import com.yokalona.annotations.Approximate;
import com.yokalona.annotations.PerformanceImpact;
import com.yokalona.annotations.SpawnSubprocess;
import com.yokalona.file.Pointer;
import com.yokalona.file.exceptions.NoFreeSpaceLeftException;
import com.yokalona.file.exceptions.WriteOverflowException;
import com.yokalona.file.serializers.PointerSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class MergeAvailabilitySpace {
    private int free;
    private int start;
    private final int end;
    private final ASPage<Pointer> pointers;

    public MergeAvailabilitySpace(Configuration configuration) {
        this.start = configuration.offset + configuration.dataSpace;
        this.end = configuration.totalSpace;
        this.pointers = new ASPage<>(PointerSerializer.forSpace(configuration.totalSpace),
                new ASPage.Configuration(configuration.page, configuration.offset, configuration.dataSpace));
        this.pointers.append(new Pointer(this.start, this.end));
        this.free = this.end - this.start;
    }

    public int
    alloc(int size) {
        int address = allocate(size);
        if (address < 0) {
            defragmentation();
            address = allocate(size);
        }
        return address;
    }

    @Approximate
    public boolean
    fits(int size) {
        return available() >= size;
    }

    public void
    reduce(int by) {
        if (this.pointers.size() == 0) throw new NoFreeSpaceLeftException();
        this.start += by;
        this.free -= by;
    }

    public int
    free0(int size, int address) {
        if (address < start)
            throw new WriteOverflowException("Attempt to free space outside the available space");
        if (address + size > end) throw new WriteOverflowException("Freeing more memory, that is accessible");
        Pointer pointer = new Pointer(address, address + size);
        int index = this.pointers.find(pointer, Comparator.comparingInt(Pointer::start));
        if (index < 0) {
            if (this.pointers.spills()) {
                defragmentation();
                index = this.pointers.find(pointer, Comparator.comparingInt(Pointer::start));
                if (index < 0) this.pointers.insert(-(index + 1), pointer);
                else this.pointers.set(index, pointer);
            } else this.pointers.insert(-(index + 1), pointer);
        } else this.pointers.set(index, pointer);
        this.free += size;
        return this.pointers.size();
    }

    void
    freeImmediately(int end) {
        this.pointers.clear();
        this.pointers.append(new Pointer(start, end));
        this.free += end;
    }

    public int
    freeImmediately(int size, int address) {
        if (address + size > end) throw new WriteOverflowException("Freeing more memory, that is accessible");
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
        this.free = 0;
        for (Pointer p : merged) {
            if (this.pointers.spills()) defragmentation();
            this.pointers.append(p);
            this.free += p.length();
        }
        return this.pointers.size();
    }

    @Approximate
    @PerformanceImpact
    public int
    available() {
        return free;
    }

    public int
    fragments() {
        return this.pointers.size();
    }

    @SpawnSubprocess
    public void
    defragmentation() {
        Pointer[] pointers = this.pointers.read(Pointer.class);
        if (pointers.length == 0) throw new NoFreeSpaceLeftException();
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
        this.free = 0;
        for (Pointer p : merged) {
            this.pointers.append(p);
            this.free += p.length();
        }
    }

    public int
    maxAddress() {
        return end;
    }

    private int
    allocate(int size) {
        int selected = -1, delta = Integer.MAX_VALUE;
        Pointer[] pointers = this.pointers.read(Pointer.class);
        for (int index = 0; index < pointers.length; index++) {
            if (pointers[index].length() >= size && delta > pointers[index].length() - size)
                delta = pointers[selected = index].length() - size;
            if (delta == 0) break;
        }
        if (selected < 0) return -1;
        if (delta != 0) this.pointers.set(selected, pointers[selected].adjust(+0, -size));
        else this.pointers.remove(selected);
        this.free -= size;
        return pointers[selected].end() - size;
    }

    private static boolean
    overlaps(Pointer pointer, Pointer pointers) {
        return pointer.start() <= pointers.end() && pointers.start() <= pointer.end();
    }

    public record Configuration(byte[] page, int offset, int dataSpace, int totalSpace) {
    }
}
