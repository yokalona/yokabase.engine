package com.yokalona.file;

public class AddressTools {

    private AddressTools() {}

    public static byte
    significantBytes(long space) {
        if (space <= 0xFFFF) return 2;
        else if (space <= 0xFFFFFF) return 3;
        else if (space <= 0xFFFFFFFFL) return 4;
        else if (space <= 0xFFFFFFFFFFL) return 5;
        else if (space <= 0xFFFFFFFFFFFFL) return 6;
        else if (space <= 0xFFFFFFFFFFFFFFL) return 7;
        else return 8;
    }
}
