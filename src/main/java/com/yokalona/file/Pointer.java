package com.yokalona.file;

public record Pointer(int length, int address) {
    public Pointer
    adjust(int deltaLength, int deltaAddress) {
        return new Pointer(length + deltaLength, address + deltaAddress);
    }

    public int
    end() {
        return address + length;
    }
}
