package com.yokalona.file;

import com.yokalona.array.serializers.VariableSizeSerializer;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.array.serializers.primitives.IntegerSerializer;

import java.util.*;

public class VSPage<Type> implements Page {

    int free;
    int space;
    byte[] page;
    int significantBytes;
    ASPage<Integer> dataPointerSpace;
    ASPage<Pointer> availabilitySpace;
    VariableSizeSerializer<Type> serializer;

    public VSPage(int size, VariableSizeSerializer<Type> serializer) {
        this.serializer = serializer;
        this.page = new byte[size * 1024];
        this.significantBytes = AddressSpaceTools.significantBytes(page.length);
        int availabilitySpaceSize = (int) (page.length * .1);
        int dataSpaceSize = page.length - availabilitySpaceSize - 1;
        this.availabilitySpace = ASPage.create(availabilitySpaceSize, significantBytes, PointerSerializer.forSpace(significantBytes), page);
        this.dataPointerSpace = ASPage.create(dataSpaceSize, availabilitySpace.space() + availabilitySpace.offset(),
                new CompactIntegerSerializer(significantBytes), page);
        availabilitySpace.append(new Pointer(dataPointerSpace.space(), availabilitySpace.space()));
        this.space = this.free = page.length - significantBytes - availabilitySpace.free() - dataPointerSpace.occupied();
        IntegerSerializer.INSTANCE.serializeCompact(this.free, this.page, 0);
    }

    public Type
    get(int index) {
        Integer address = dataPointerSpace.get(index);
        return serializer.deserialize(page, address);
    }

    public void
    append(Type value) {
        Pointer reservedSpace = availabilitySpace.first();
        int size = serializer.sizeOf(value) + significantBytes;
        if (size > reservedSpace.length())
            throw new RuntimeException("Reserved space exceeded");
        int address = reservedSpace.address() + (reservedSpace.length() - size - significantBytes);
        serializer.serialize(value, page, address);
        dataPointerSpace.append(address);
        updateReservedSpace(reservedSpace.adjust(-size, +significantBytes));
        IntegerSerializer.INSTANCE.serializeCompact(free -= size, significantBytes, page, 0);
    }

    public void
    set(int index, Type value) {
        Integer address = dataPointerSpace.get(index);
        int priorSize = serializer.sizeOf(page, address);
        int newSize = serializer.sizeOf(value);
        if (priorSize < newSize) {
            Pointer reservedSpace = availabilitySpace.first();
            address = reservedSpace.address() + (reservedSpace.length() - newSize);
            reservedSpace = reservedSpace.adjust(-newSize - significantBytes, +significantBytes);
            updateReservedSpace(reservedSpace);
            dataPointerSpace.set(index, address);
            free -= significantBytes;
        } else if (priorSize > newSize) {
            updateReservedSpace(new Pointer(priorSize - newSize, address + newSize));
        }
        serializer.serialize(value, page, address);
        IntegerSerializer.INSTANCE.serializeCompact(free + priorSize - newSize, significantBytes, page, 0);
    }

    public void
    remove(int index) {
        Integer address = dataPointerSpace.get(index);
        dataPointerSpace.remove(index);
        int size = serializer.sizeOf(page, address);
        updateReservedSpace(new Pointer(size, address));
        IntegerSerializer.INSTANCE.serializeCompact(free + size, significantBytes, page, address);
    }

    public boolean
    canFit(int size) {
        return size < free && availabilitySpace.free() >= significantBytes;
    }

    private void
    updateReservedSpace2(Pointer reservedSpace) {
        List<Pointer> result = new ArrayList<>();
        Pointer[] pointers = availabilitySpace.read(Pointer.class);
        Arrays.sort(pointers, Comparator.comparingInt(Pointer::address));
        int index = 0;
        while (index < pointers.length && pointers[index].end() < reservedSpace.address())
            result.add(pointers[index++]);
        while (index < pointers.length && overlaps(pointers[index], reservedSpace)) {
            int minAddress = Math.min(pointers[index].address(), reservedSpace.address());
            int maxEnd = Math.max(pointers[index].end(), reservedSpace.end());
            reservedSpace = new Pointer(maxEnd - minAddress, minAddress);
            index ++;
        }
        result.add(reservedSpace);
        while (index < pointers.length) {
            result.add(pointers[index++]);
        }
        availabilitySpace.clear();
        Arrays.sort(pointers, Comparator.comparingInt(Pointer::length));
        for (Pointer p : result) availabilitySpace.append(p);
    }

    boolean overlaps(Pointer left, Pointer right) {
        return (left.address() >= right.address() && left.address() <= right.end())
                || (right.address() >= left.address() && right.address() <= left.end());
    }

    public int[][] merge(int[][] intervals) {
        Arrays.sort(intervals, Comparator.comparingInt(interval -> interval[0]));
        LinkedList<int[]> result = new LinkedList<>();
        result.add(intervals[0]);
        for (int i = 1; i < intervals.length; i ++) {
            int [] other = intervals[i];
            if (result.getLast()[1] < other[0]) {
                result.add(other);
            } else {
                result.getLast()[1] = Math.max(result.getLast()[1], other[1]);
            }
        }
        return result.toArray(new int[0][]);
    }

    private void
    defragmentAvailabilitySpace() {
        Pointer[] pointers = availabilitySpace.read(Pointer.class);
        Arrays.sort(pointers, Comparator.comparingInt(Pointer::address));
        LinkedList<Pointer> result = new LinkedList<>();
        result.add(pointers[0]);
        for (int i = 1; i < pointers.length; i ++) {
            Pointer other = pointers[i];
            if (result.getLast().end() < other.address()) result.add(other);
            else {
                Pointer last = result.pollLast();
                result.add(new Pointer(last.address(), Math.max(last.end(), other.end()) - last.address()));
            }
        }
        availabilitySpace.clear();
        Arrays.sort(pointers, Comparator.comparingInt(Pointer::length));
        for (Pointer pointer : result) availabilitySpace.append(pointer);
    }

    private void
    updateReservedSpace(Pointer reservedSpace) {
        int position = availabilitySpace.find(reservedSpace, Comparator.comparingInt(Pointer::length));
        if (position < 0) availabilitySpace.insert(-(position + 1), reservedSpace);
        else availabilitySpace.set(position, reservedSpace);
    }

    @Override
    public int
    size() {
        return dataPointerSpace.size();
    }

    @Override
    public int
    free() {
        return free;
    }

    @Override
    public int
    occupied() {
        return space - free;
    }

    @Override
    public String
    toString() {
        return String.format("vs[%10d/%-10d].%5d", free, space, size());
    }

}
