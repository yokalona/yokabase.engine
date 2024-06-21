package com.yokalona.file;

import com.yokalona.array.configuration.File;
import com.yokalona.array.io.CachedFile;
import com.yokalona.array.io.InputReader;
import com.yokalona.array.io.OutputWriter;
import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.VariableSizeSerializer;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.array.serializers.primitives.IntegerSerializer;
import com.yokalona.array.serializers.primitives.LongSerializer;
import com.yokalona.file.headers.CRC;
import com.yokalona.file.headers.Fixed;
import com.yokalona.file.page.FSPage;
import com.yokalona.file.page.ArrayPage;
import com.yokalona.file.page.VSPage;

import java.io.IOException;
import java.nio.file.Files;

public class VSFile<Type> implements Index<Type> {

    public static int VS_PAGE_SIZE = 4096;
    public static float VS_PAGE_DISTRIBUTION = .1F;
    public static int AS_PAGE_SIZE = 4096;

    private final ArrayPage<PagePointer> index;
    private final CachedFile cachedFile;
    private final VariableSizeSerializer<Type> serializer;
    private final byte[] vsBuffer = new byte[VS_PAGE_SIZE];

    private int lastRead = -1;

    public VSFile(VariableSizeSerializer<Type> serializer, Configuration configuration) throws IOException {
        long blockSize = Files.getFileStore(configuration.file.path().getParent()).getBlockSize();

        this.serializer = serializer;
        this.cachedFile = new CachedFile(configuration.file);
        this.index = FSPage.Configurer.create(Math.min((int) blockSize, AS_PAGE_SIZE))
                .addHeader(new CRC())
                .addHeader(new Fixed<>(6, new CompactIntegerSerializer(1)))
                .fspage(new PagePointerSerializer());
    }

    public void
    append(Type value) {
        if (index.size() == 0) {
            create();
        }
        PagePointer last = index.last();
        if (lastRead != index.size() - 1) {
            if (lastRead >= 0) {
                PagePointer prior = index.get(lastRead);
                writePage(prior.address(), VSPage.Configurer.create(vsBuffer).read(serializer));
                updateIndex();
            }

            try (cachedFile) {
                InputReader reader = cachedFile.reader(vsBuffer);
                reader.seek(last.address());
                reader.refill();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            lastRead = index.size() - 1;
        }

        VSPage<Type> page = VSPage.Configurer.create(vsBuffer).read(serializer);
        if (!page.fits(value)) {
            create();
            append(value);
            return;
        }
        int size = page.append(value);
        page.flush();
        this.index.set(index.size() - 1, new PagePointer(last.address(), size));
//        writePage(last.address(), page);
//        updateIndex();
    }

    public void
    create() {
        VSPage<Type> page = VSPage.Configurer.create(vsBuffer).distribute(VS_PAGE_DISTRIBUTION).vspage(serializer);
        long address = AS_PAGE_SIZE + (long) this.index.size() * VS_PAGE_SIZE;
        this.index.append(new PagePointer(address, 0));
        writePage(address, page);
        updateIndex();
    }

    private void
    writePage(long address, VSPage<Type> page) {
        page.flush();
        try (cachedFile) {
            OutputWriter writer = cachedFile.writer(vsBuffer);
            writer.seek(address);
            writer.flushAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void
    updateIndex() {
        try (cachedFile) {
            OutputWriter writer = cachedFile.writer(this.index.configuration().page());
            this.index.flush();
            writer.seek(0L);
            writer.flushAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    record PagePointer(long address, int size) {
    }

    static class PagePointerSerializer implements FixedSizeSerializer<PagePointer> {

        public static final PagePointerSerializer INSTANCE = new PagePointerSerializer();

        @Override
        public int sizeOf() {
            return Long.BYTES + Integer.BYTES;
        }

        @Override
        public int serialize(PagePointer pagePointer, byte[] data, int offset) {
            LongSerializer.INSTANCE.serializeCompact(pagePointer.address, data, offset);
            IntegerSerializer.INSTANCE.serializeCompact(pagePointer.size, data, offset + Long.BYTES);
            return sizeOf();
        }

        @Override
        public PagePointer deserialize(byte[] bytes, int offset) {
            long address = LongSerializer.INSTANCE.deserializeCompact(bytes, offset);
            int size = IntegerSerializer.INSTANCE.deserializeCompact(bytes, offset + Long.BYTES);
            return new PagePointer(address, size);
        }
    }

    public record Configuration(File file) {
    }
}
