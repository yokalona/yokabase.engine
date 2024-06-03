package com.yokalona.file;

public interface Page {

    /**
     * @return Total size in bytes
     */
    int size();

    /**
     * @return Free space size in bytes;
     */
    int free();

    static int
    kb(int kb) {
        return 1024 * kb;
    }

    int
    occupied();
}
