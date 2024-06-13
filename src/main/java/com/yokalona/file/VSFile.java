package com.yokalona.file;

import com.yokalona.array.configuration.File;
import com.yokalona.array.io.CachedFile;
import com.yokalona.array.io.InputReader;
import com.yokalona.array.io.OutputWriter;
import com.yokalona.array.serializers.VariableSizeSerializer;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.array.serializers.primitives.CompactLongSerializer;
import com.yokalona.file.page.ASPage;
import com.yokalona.file.page.Page;
import com.yokalona.file.page.VSPage;

import java.io.IOException;

public class VSFile<Type> implements Index<Type> {

    int[] content;
    ASPage<Long> pages;
    VariableSizeSerializer<Type> serializer;
    byte[] sharedBuffer;
    byte[] index;
    final CachedFile storage;

    VSFile(VariableSizeSerializer<Type> serializer, VSFileConfiguration configuration) {
        this.serializer = serializer;
        this.storage = new CachedFile(configuration.file());
        this.sharedBuffer = new byte[configuration.buffer.pages * configuration.page.sizeKb];
        this.index = new byte[configuration.index.sizeKb * 1024];
//        this.pages = ASPage.create(configuration.index.sizeKb * 1024, 0,
//                new CompactLongSerializer(AddressTools.significantBytes(configuration.index.sizeKb
//                        * configuration.index.sizeKb * 1024 * 1024L)), index);
        flush();
    }

    static record VSFileConfiguration(File file, IndexConfiguration index, PageConfiguration page, BufferConfiguration buffer) {
        record IndexConfiguration(int sizeKb) {}
        record PageConfiguration(int sizeKb, float spaceDistribution) { }
        record BufferConfiguration(int pages) { }
    }

    void
    create() {
        pages.append(4096L);
    }

    PersistentPage
    page(int index) {
        return new PersistentPage(index);
    }

    void
    flush() {
        try (storage) {
            OutputWriter out = new OutputWriter(storage.get(), index);
            storage.peek().seek(0);
            out.flushAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    class PersistentPage implements AutoCloseable {

        private final int index;
        private final Page<Type> page;

        public PersistentPage(int index) {
            this.index = index;
            Long address = pages.get(index);
            try (storage) {
                InputReader in = new InputReader(storage.get(), sharedBuffer);
                storage.peek().seek(address);
                in.refill();
                this.page = new VSPage<>(serializer, sharedBuffer, 0, 4096, .1F);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public Page<Type>
        page() {
            return page;
        }

        @Override
        public void
        close() {
            Long address = pages.get(index);
            try (storage) {
                // TODO: switch
                OutputWriter out = new OutputWriter(storage.get(), sharedBuffer);
                storage.peek().seek(address);
                out.flushAll();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
