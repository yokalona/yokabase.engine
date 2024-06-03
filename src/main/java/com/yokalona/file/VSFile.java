package com.yokalona.file;

import java.nio.file.Path;

public class VSFile<Type> implements File<Type> {
    private Path shadow;
    private Path primary;
    private Header header;
    private int size;
    // size = header.size + pages.count * page.configuration.size;

    private VSFile(Path path) {
        this.primary = path;
        this.shadow = create();
        this.size = header.size();
    }

    private Path
    create() {
        return null;
    }

    private void
    promote() {
        this.primary = shadow;
        this.shadow = create();
    }

    @Override
    public int
    pages() {
        return 0;
    }

    @Override
    public Pointer
    write(int index, Type data) {
        return null;
    }

    @Override
    public Type
    read(Pointer pointer) {
        return null;
    }
}
