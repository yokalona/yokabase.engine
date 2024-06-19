package com.yokalona.file;

import com.yokalona.array.configuration.File;
import com.yokalona.array.io.CachedFile;
import com.yokalona.array.io.InputReader;
import com.yokalona.array.io.OutputWriter;
import com.yokalona.array.serializers.VariableSizeSerializer;
import com.yokalona.array.serializers.primitives.LongSerializer;
import com.yokalona.file.page.ASPage;
import com.yokalona.file.page.VSPage;

import java.io.IOException;

public class VSFile<Type> implements Index<Type> {

    public static int VS_PAGE_SIZE = 32 * 1024;
    public static float VS_PAGE_DISTRIBUTION = .1F;
    public static int AS_PAGE_SIZE = 1024;

    private ASPage<Long> index;
    private final CachedFile cachedFile;
    private final VariableSizeSerializer<Type> serializer;
    private final byte[] vsBuffer = new byte[VS_PAGE_SIZE];

    public VSFile(VariableSizeSerializer<Type> serializer, Configuration configuration) {
        this.serializer = serializer;
        this.cachedFile = new CachedFile(configuration.file);
        this.index = new ASPage<>(LongSerializer.INSTANCE, new ASPage.Configuration(AS_PAGE_SIZE));
    }

    public VSPage<Type>
    create() {
        return new VSPage<>(serializer, new VSPage.Configuration(vsBuffer, 0, VS_PAGE_DISTRIBUTION));
    }

    public VSPage<Type>
    get(int page) {
        Long address = index.get(page);
        try (cachedFile) {
            InputReader reader = cachedFile.reader(vsBuffer);
            reader.seek(address);
            reader.refill();
            return VSPage.read(serializer, vsBuffer, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void
    append(Type value) {
        VSPage<Type> page;
        if (index.size() == 0) page = create();
        else page = get(index.size() - 1);
        page.append(value);
        page.flush();
        Long address = index.get(index.size() - 1);
        try (cachedFile) {
            OutputWriter writer = cachedFile.writer(vsBuffer);
            writer.seek(address);
            writer.flushAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public record Configuration(File file) {}
}
