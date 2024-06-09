package com.yokalona.file;

public class AddressTools {

    private AddressTools() {}

    public static byte
    significantBytes(int space) {
        if (space <= 0xFFFF) return 2;
        else if (space <= 0xFFFFFF) return 3;
        else return 4;
    }
}
