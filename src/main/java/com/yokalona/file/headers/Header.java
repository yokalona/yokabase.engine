package com.yokalona.file.headers;

public interface Header {
    int length();

    void write(byte[] page, int offset);

    void read(byte[] page, int offset);

    default void offset(int ignore) {
    }

    static int
    headerOffset(Header[] headers) {
        int offset = 0;
        for (Header header : headers) offset += header.length();
        return offset;
    }

    static int
    initHeaders(Header[] headers) {
        int headerOffset = headerOffset(headers);
        for (Header header : headers) header.offset(headerOffset);
        return headerOffset;
    }

    static void
    readHeaders(byte[] page, int offset, Header[] headers) {
        for (Header header : headers) {
            header.read(page, offset);
            offset += header.length();
        }
    }

    static void
    writeHeaders(Header[] headers, byte[] page, int offset) {
        for (Header header : headers) {
            header.write(page, offset);
            offset += header.length();
        }
    }

    static Header[]
    join(Header[] required, Header[] headers) {
        Header[] result = new Header[required.length + headers.length];
        System.arraycopy(required, 0, result, 0, required.length);
        System.arraycopy(headers, 0, result, required.length, headers.length);
        return result;
    }
}
