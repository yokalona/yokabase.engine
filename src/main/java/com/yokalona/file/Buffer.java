package com.yokalona.file;

public class Buffer {
    byte[] buffer;

    Shared
    share(int offset, int length) {
        return new Shared(buffer, offset, length);
    }

    static class Shared {
        private final int offset;
        private final int length;
        private final byte[] buffer;

        public Shared(byte[] buffer, int offset, int length) {
            this.buffer = buffer;
            this.offset = offset;
            this.length = length;
        }

        public byte
        get(int index) {
            withinLimits(index);
            return buffer[offset + index];
        }

        public void
        set(int index, byte value) {
            withinLimits(index);
            buffer[offset + index] = value;
        }

        private void
        withinLimits(int index) {
            if (index < 0) throw new RuntimeException();
            if (index + offset > length) throw new RuntimeException();
        }
    }
}
