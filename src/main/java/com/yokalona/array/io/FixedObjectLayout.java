package com.yokalona.array.io;

import com.yokalona.array.PersistentArray;
import com.yokalona.array.serializers.FixedSizeSerializer;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Describes a fixed object data layout. Each object in such a layout has a fixed size it can occupy in the output
 * file. This layout can be beneficial for fixed size data types, such as integers or composite data types
 * consistent with fixed size data types. For large data sets, this data layout saves space on disk, however, for
 * small arrays it will create unnecessary overhead.
 *
 * @param descriptor
 */
public record FixedObjectLayout(FixedSizeSerializer<?> descriptor) implements DataLayout {
    @Override
    public void seek(int index, RandomAccessFile raf) throws IOException {
        raf.seek(((long) index) * descriptor.sizeOf() + PersistentArray.HEADER_SIZE);
    }

    @Override
    public byte
    mode() {
        return 1;
    }
}
