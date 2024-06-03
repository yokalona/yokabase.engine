package com.yokalona.file;

public class AddressSpaceTools {

    private AddressSpaceTools() {}

    public static int
    significantBytes(int space) {
        if (space <= 0xFFFF) return 2;
        else if (space <= 0xFFFFFF) return 3;
        else return 4;
    }
}
