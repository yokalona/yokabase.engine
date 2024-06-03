package com.yokalona.array.serializers;

public final class Version implements Comparable<Version> {

    public static final VersionSerializer serializer = new VersionSerializer();

    private final byte critical;
    private final byte major;
    private final byte minor;
    private byte mode;

    public Version(byte critical, byte major, byte minor, byte mode) {
        this.critical = critical;
        this.major = major;
        this.minor = minor;
        this.mode = mode;
    }

    public Version(int critical, int major, int minor, int mode) {
        this((byte) critical, (byte) major, (byte) minor, (byte) mode);
    }

    public Version
    copy() {
        return new Version(critical, major, minor, mode);
    }

    @Override
    public int
    compareTo(Version other) {
        int critical = Byte.compare(this.critical, other.critical);
        if (critical != 0) return critical;
        else return Byte.compare(this.major, other.major);
    }

    @Override
    public String
    toString() {
        return String.format("%d.%d.%d mode=%d", critical, major, minor, mode);
    }

    public byte
    mode() {
        return mode;
    }

    public void
    mode(byte mode) {
        this.mode = mode;
    }

    public static class VersionSerializer implements FixedSizeSerializer<Version> {

        @Override
        public void serialize(Version version, byte[] data, int offset) {
            data[offset++] = version.critical;
            data[offset++] = version.major;
            data[offset++] = version.minor;
            data[offset] = version.mode;
        }

        @Override
        public Version deserialize(byte[] bytes, int offset) {
            return new Version(bytes[offset], bytes[offset + 1], bytes[offset + 2], bytes[offset + 3]);
        }

        @Override
        public int sizeOf() {
            return 4;
        }
    }
}
