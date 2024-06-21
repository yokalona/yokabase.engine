package com.yokalona.file.page;

import com.yokalona.annotations.Approximate;
import com.yokalona.annotations.SpawnSubprocess;
import com.yokalona.array.serializers.primitives.IntegerSerializer;
import com.yokalona.file.Array;
import com.yokalona.file.Pointer;
import com.yokalona.file.exceptions.NoFreeSpaceLeftException;
import com.yokalona.file.exceptions.WriteOverflowException;
import com.yokalona.file.serializers.PointerSerializer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class MASpace {
    private int free;
    private int start;
    private final int end;
    public final ArrayPage<Pointer> pointers;

    public MASpace(Configuration configuration) {
        this.start = configuration.offset + configuration.dataSpace + 22;
        this.end = configuration.totalSpace;
        this.pointers = new CachedArrayPage<>(
                ASPage.Configurer.create(configuration.page, configuration.offset + Integer.BYTES)
                        .length(configuration.dataSpace)
                        .aspage(PointerSerializer.forSpace(configuration.totalSpace)));
        this.pointers.append(new Pointer(this.start, this.end));
        this.free = this.end - this.start;
        write(start, configuration.page, configuration.offset);
    }

    private MASpace(int start, int end, ASPage<Pointer> pointers) {
        this.end = end;
        this.start = start;
        Array<Pointer> read = pointers.read(Pointer.class);
        for (Pointer p : read) {
            if (p.start() < start) {
                int delta = p.end() - start;
                if (delta > 0) this.free += delta;
            } else this.free += p.length();
        }
        this.pointers = pointers;
    }

    public static MASpace
    read(byte[] page, int offset, int length) {
        int start = IntegerSerializer.INSTANCE.deserializeCompact(page, offset);
        return new MASpace(start, length,
                ASPage.Configurer.create(page, offset + Integer.BYTES)
                .read(PointerSerializer.forSpace(length)));
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

    public boolean
    reduce(int by) {
        if (this.pointers.size() == 0) throw new NoFreeSpaceLeftException();
        this.start += by;
        this.free -= by;
        write(start, pointers.configuration().page(), pointers.configuration().offset() - Integer.BYTES);
        return true;
    }

    public int
    free0(int size, int address) {
        if (address < start)
            throw new WriteOverflowException("Attempt to free space outside the available space");
        if (address + size > end) throw new WriteOverflowException("Freeing more memory, that is accessible");
        Pointer pointer = new Pointer(address, address + size);
        int index = this.pointers.find(pointer, Comparator.comparingInt(Pointer::start));
        if (index < 0) {
            if (this.pointers.free() < this.pointers.serializer().sizeOf()) {
                defragmentation();
                index = this.pointers.find(pointer, Comparator.comparingInt(Pointer::start));

                // TODO: defrag on dataspace should fix that
                if (this.pointers.free() < this.pointers.serializer().sizeOf()) throw new NoFreeSpaceLeftException();

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
        if (end == start) return;
        this.pointers.append(new Pointer(start, end));
        this.free = end - start;
    }

    public int
    freeImmediately(int size, int address) {
        if (address + size > end) throw new WriteOverflowException("Freeing more memory, that is accessible");
        Pointer pointer = new Pointer(address, address + size);
        Array<Pointer> pointers = this.pointers.read(Pointer.class);
        List<Pointer> merged = new ArrayList<>();
        int index = 0;
        while (index < pointers.length() && pointer.start() > pointers.get(index).end())
            merged.add(pointers.get(index++));
        while (index < pointers.length() && overlaps(pointer, pointers.get(index)))
            pointer = new Pointer(Math.min(pointer.start(), pointers.get(index).start()),
                    Math.max(pointer.end(), pointers.get(index++).end()));
        merged.add(pointer);
        while (index < pointers.length()) merged.add(pointers.get(index++));
        this.pointers.clear();
        this.free = 0;
        for (Pointer p : merged) {
            if (this.pointers.free() < this.pointers.serializer().sizeOf()) defragmentation();
            this.pointers.append(p);
            this.free += p.length();
        }
        return this.pointers.size();
    }

    @Approximate
    public int
    available() {
        return free;
    }

    public int
    beginning() {
        return start;
    }

    public int
    fragments() {
        return this.pointers.size();
    }

    @SpawnSubprocess
    public void
    defragmentation() {
        Array<Pointer> pointers = this.pointers.read(Pointer.class);
        if (pointers.length() == 0) throw new NoFreeSpaceLeftException();
        LinkedList<Pointer> merged = new LinkedList<>();
        merged.add(pointers.get(0));
        for (int i = 1; i < pointers.length(); i++) {
            Pointer other = pointers.get(i);
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
            if (p.start() < start) {
                int delta = p.end() - start;
                if (delta > 0) this.free += delta;
            } else this.free += p.length();
        }
    }

    public int
    maxAddress() {
        return end;
    }

    private int
    allocate(int size) {
        int selected = -1, delta = Integer.MAX_VALUE;
        Array<Pointer> pointers = this.pointers.read(Pointer.class);
        for (int index = 0; index < pointers.length(); index++) {
            if (pointers.get(index).end() - size < start) continue;
            if (pointers.get(index).length() >= size && delta > pointers.get(index).length() - size) {
                delta = pointers.get(selected = index).length() - size;
                if (delta == 0) break;
            }
        }
        if (selected < 0) return -1;
        Pointer pointer = pointers.get(selected);
        if (delta != 0) this.pointers.set(selected, pointer.adjust(+0, -size));
        else this.pointers.remove(selected);
        this.free -= size;
        return pointer.end() - size;
    }

    private static boolean
    overlaps(Pointer pointer, Pointer pointers) {
        return pointer.start() <= pointers.end() && pointers.start() <= pointer.end();
    }

    private static int
    write(int value, byte[] page, int offset) {
        return IntegerSerializer.INSTANCE.serializeCompact(value, Integer.BYTES, page, offset);
    }

    public void
    flush() {
        this.pointers.flush();
    }

    public record Configuration(byte[] page, int offset, int dataSpace, int totalSpace) {
    }
}
