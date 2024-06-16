package com.yokalona.file;

public record Pointer(int start, int end) {
    public Pointer {
        assert start < end : "[ " + start + " >= " + end + " ]";
    }

    public Pointer
    adjust(int ΔStart, int ΔEnd) {
        return new Pointer(start + ΔStart, end + ΔEnd);
    }

    public int
    length() {
        return end - start;
    }
}
