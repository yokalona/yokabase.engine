package com.yokalona.file.headers;

import com.yokalona.file.Header;

public class VersionHeader implements Header {

    public VersionHeader(byte[] space, int offset) {
        space[offset++] = 0x1;
        space[offset++] = 0x0;
        space[offset] = 0x0;
    }

    @Override
    public int size() {
        return 3;
    }

}
