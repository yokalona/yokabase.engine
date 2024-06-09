package com.yokalona.file;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class AvailabilitySpace {
    private final ASPage<Pointer> pointers;

    /*
    size   -> address space size(excluding itself)
    total  -> total space, max address
    space  -> actual addressing space including everything
    offset -> offset on which availability space resides
     */
    public AvailabilitySpace(int size, int total, byte[] space, int offset) {
        PointerSerializer serializer = PointerSerializer.forSpace(size);
        this.pointers = ASPage.create(size, offset, serializer, space);
        this.pointers.append(new Pointer(total - size, total));
    }

    public int
    alloc(int size) {
        int pointer = region(size);
        if (pointer < 0) {
            defragmentation();
            pointer = region(size);
        }
        return pointer - size;
    }

    public int
    free(int size, int address) {
        Pointer pointer = new Pointer(size, address + size);
        Pointer[] pointers = this.pointers.read(Pointer.class);
        Arrays.sort(pointers, Comparator.comparingInt(Pointer::address));
        List<Pointer> merged = new ArrayList<>();
        int index = 0;
        while (index < pointers.length && pointer.start() > pointers[index].end()) merged.add(pointers[index++]);
        while (index < pointers.length && pointer.start() <= pointers[index].end() && pointers[index].start() <= pointer.end()) {
            int start = Math.min(pointer.start(), pointers[index].start());
            int end = Math.max(pointer.end(), pointers[index].end());
            pointer = new Pointer(end - start, end);
            index ++;
        }
        merged.add(pointer);
        while (index < pointers.length) merged.add(pointers[index++]);
        this.pointers.clear();
        merged.sort(Comparator.comparingInt(Pointer::length).reversed());
        for (Pointer p : merged) this.pointers.append(p);
        return this.pointers.size();
    }

    public int
    available() {
        return Arrays.stream(this.pointers.read(Pointer.class))
                .mapToInt(Pointer::length)
                .sum();
    }

    private int
    region(int size) {
        Iterator<Pointer> iterator = pointers.iterator();
        while (iterator.hasNext()) {
            Pointer pointer = iterator.next();
            if (pointer.length() >= size) {
                if (pointer.length() > size) pointers.append(pointer.adjust(-size, -size));
                iterator.remove();
                return pointer.address();
            }
        }
        return -1;
    }

    private void
    defragmentation() {
        // do nothing for now
    }
}
