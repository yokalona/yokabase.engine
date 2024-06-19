package com.yokalona.array.io;

import java.io.IOException;
import java.io.RandomAccessFile;

public class OutputWriter implements AutoCloseable {
    private final RandomAccessFile raf;
    private final byte[] buffer;
    private int position;

    public OutputWriter(RandomAccessFile raf, int size) {
        this(raf, new byte[size]);
    }

    public OutputWriter(RandomAccessFile raf, byte[] buffer) {
        this.raf = raf;
        this.buffer = buffer;
    }

    public void
    write(byte[] data) throws IOException {
        int written = 0;
        while (position + data.length - written >= buffer.length) {
            written += write(data, written, buffer.length - position);
            flush();
        }
        write(data, written, data.length - written);
    }

    public int
    write(byte[] data, int offset, int length) {
        System.arraycopy(data, offset, buffer, position, length);
        position += length;
        return length;
    }

    public void
    flush() throws IOException {
        raf.write(buffer, 0, position);
        position = 0;
    }

    public void
    flushAll() throws IOException {
        position = buffer.length;
        flush();
    }

    @Override
    public void
    close() throws Exception {
        flush();
    }

    public void
    seek(Long address) throws IOException {
        flush();
        raf.seek(address);
    }
}
