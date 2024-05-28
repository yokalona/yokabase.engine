package com.yokalona.array;

import com.yokalona.array.PersistentArray;
import com.yokalona.array.TestExecutor;
import com.yokalona.array.configuration.Configuration;
import com.yokalona.array.debug.CompactInteger;
import com.yokalona.array.exceptions.FileMarkedForDeletingException;
import com.yokalona.array.exceptions.HeaderMismatchException;
import com.yokalona.array.exceptions.IncompatibleVersionException;
import com.yokalona.array.io.FixedObjectLayout;
import com.yokalona.array.serializers.Serializers;
import com.yokalona.array.subscriber.CountingSubscriber;
import com.yokalona.array.subscriber.CountingSubscriber.Counter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.yokalona.array.configuration.Chunked.chunked;
import static com.yokalona.array.configuration.ChunkedRead.read;
import static com.yokalona.array.configuration.ChunkedWrite.write;
import static com.yokalona.array.configuration.File.file;
import static com.yokalona.array.configuration.Configuration.configure;
import static com.yokalona.array.debug.CompactInteger.compact;
import static org.junit.jupiter.api.Assertions.*;

public class PersistentArrayTest {

    private Path path;

    @BeforeEach
    public void
    setUp() throws IOException {
        this.path = Files.createTempDirectory("array");
    }

    @AfterEach
    public void
    tearDown() throws IOException {
        try (var folder = Files.list(path)) {
            folder.map(Path::toFile).forEach(file -> {
                boolean ignore = file.delete();
            });
        }
    }

    @Test
    public void
    testCreateArrayCreatesFileOnDisk() throws IOException {
        Path filePath = path.resolve("testCreateArrayCreatesFileOnDisk.la");
        try (var ignore = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new,
                configure(file(filePath).cached())
                        .memory(chunked(10))
                        .read(read().chunked(10))
                        .write(write().chunked(10)))) {
            assertTrue(Files.exists(filePath));
            assertTrue(0 < Files.size(filePath));
        }
    }

    @Test
    public void
    testValidateHeader() throws IOException {
        Path filePath = path.resolve("testValidateHeader.la");
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(10))
                .read(read().chunked(10))
                .write(write().chunked(10));
        try (var ignore = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            assertTrue(Files.exists(filePath));
        }
        try (var file = new RandomAccessFile(filePath.toFile(), "rw")) {
            file.seek(1);
            file.write(0);
        }
        assertThrows(HeaderMismatchException.class, () -> PersistentArray.deserialize(CompactInteger.descriptor, configuration).close());
    }

    @Test
    public void
    testValidateVersion() throws IOException {
        Path filePath = path.resolve("testValidateVersion.la");
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(10))
                .read(read().chunked(10))
                .write(write().chunked(10));
        try (var ignore = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            assertTrue(Files.exists(filePath));
        }
        try (var file = new RandomAccessFile(filePath.toFile(), "rw")) {
            file.seek(7);
            file.write(5);
        }
        assertThrows(IncompatibleVersionException.class, () -> PersistentArray.deserialize(CompactInteger.descriptor, configuration).close());
    }

    @Test
    public void
    testFileMarkedForRemoval() throws IOException {
        Path filePath = path.resolve("testFileMarkedForRemoval.la");
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(10))
                .read(read().chunked(10))
                .write(write().chunked(10));
        try (var ignore = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            assertTrue(Files.exists(filePath));
        }
        try (var file = new RandomAccessFile(filePath.toFile(), "rw")) {
            file.seek(10);
            file.write(1);
        }
        assertThrows(FileMarkedForDeletingException.class, () -> PersistentArray.deserialize(CompactInteger.descriptor, configuration).close());
    }

    @Test
    public void
    testDataIsWritten() throws IOException {
        Path filePath = path.resolve("testDataIsWritten.la");
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(10))
                .read(read().chunked(10))
                .write(write().chunked(10));
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            assertTrue(Files.exists(filePath));
            for (int i = 0; i < array.length(); i++) {
                array.set(i, compact(i + 10));
            }
        }
        try (var file = new RandomAccessFile(filePath.toFile(), "rw")) {
            file.seek(11);
            byte[] bytes = new byte[5];
            file.read(bytes);
            assertEquals(10, Serializers.deserialize(bytes, 0));
        }
    }

    @Test
    public void
    testBaseEventsAreSent() throws IOException {
        Path filePath = path.resolve("testBaseEventsAreSent.la");
        CountingSubscriber subscriber = new CountingSubscriber();
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(10))
                .executor(new TestExecutor())
                .addSubscriber(subscriber)
                .read(read().chunked(10))
                .write(write().chunked(10));
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            assertTrue(Files.exists(filePath));
            for (int i = 0; i < array.length(); i++) {
                array.set(i, compact(i + 10));
            }
        }
        assertEquals(1, subscriber.get(Counter.CHUNK_SERIALIZATIONS));
        assertEquals(10, subscriber.get(Counter.SERIALIZATIONS));
        try (var array = PersistentArray.deserialize(CompactInteger.descriptor, configuration)) {
            for (int i = 0; i < array.length(); i++) {
                assertEquals(i + 10, array.get(i).value());
            }
        }
        assertEquals(1, subscriber.get(Counter.CHUNK_DESERIALIZATIONS));
        assertEquals(10, subscriber.get(Counter.DESERIALIZATIONS));
    }

    @Test
    public void
    testChunkSizeAffectIO() throws IOException {
        Path filePath = path.resolve("testChunkSizeAffectIO.la");
        CountingSubscriber subscriber = new CountingSubscriber();
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(10))
                .executor(new TestExecutor())
                .addSubscriber(subscriber)
                .read(read().chunked(10))
                .write(write().chunked(10));
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            assertTrue(Files.exists(filePath));
            for (int i = 0; i < array.length(); i++) {
                array.set(i, compact(i + 10));
            }
            assertEquals(1, subscriber.get(Counter.CHUNK_SERIALIZATIONS));
            assertEquals(10, subscriber.get(Counter.SERIALIZATIONS));
            array.resizeWriteChunk(3);
            subscriber.reset();
            for (int i = 0; i < array.length(); i++) {
                array.set(i, compact(i + 10));
            }
            assertEquals(3, subscriber.get(Counter.CHUNK_SERIALIZATIONS));
            assertEquals(9, subscriber.get(Counter.SERIALIZATIONS));
            array.flush();
            assertEquals(4, subscriber.get(Counter.CHUNK_SERIALIZATIONS));
            assertEquals(10, subscriber.get(Counter.SERIALIZATIONS));
        }

        try (var array = PersistentArray.deserialize(CompactInteger.descriptor, configuration)) {
            for (int i = 0; i < array.length(); i++) {
                assertEquals(i + 10, array.get(i).value());
            }
            assertEquals(1, subscriber.get(Counter.CHUNK_DESERIALIZATIONS));
            assertEquals(10, subscriber.get(Counter.DESERIALIZATIONS));
            array.resizeMemoryChunk(10);
            array.resizeReadChunk(3);
            subscriber.reset();
            for (int i = 0; i < array.length(); i++) {
                assertEquals(i + 10, array.get(i).value());
            }
            assertEquals(4, subscriber.get(Counter.CHUNK_DESERIALIZATIONS));
            assertEquals(10, subscriber.get(Counter.DESERIALIZATIONS));
        }
    }

    @Test
    public void
    testGetSame() throws IOException {
        Path filePath = path.resolve("testGetSame.la");
        CountingSubscriber subscriber = new CountingSubscriber();
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(10))
                .executor(new TestExecutor())
                .addSubscriber(subscriber)
                .read(read().chunked(3))
                .write(write().chunked(3));
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            assertTrue(Files.exists(filePath));
            for (int i = 0; i < array.length(); i++) {
                array.get(5);
            }
            array.get(6);
            array.get(7);
            assertEquals(1, subscriber.get(Counter.CHUNK_DESERIALIZATIONS));
            assertEquals(3, subscriber.get(Counter.DESERIALIZATIONS));
            assertEquals(1, subscriber.get(Counter.CACHE_MISS));
            array.get(8);
            assertEquals(2, subscriber.get(Counter.CHUNK_DESERIALIZATIONS));
            assertEquals(5, subscriber.get(Counter.DESERIALIZATIONS));
            assertEquals(2, subscriber.get(Counter.CACHE_MISS));
        }
        subscriber.reset();
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new,
                configure(configuration.file())
                        .memory(configuration.memory())
                        .executor(new TestExecutor())
                        .addSubscriber(subscriber)
                        .read(read().forceReload().chunked(3))
                        .write(configuration.write()))) {
            assertTrue(Files.exists(filePath));
            for (int i = 0; i < array.length(); i++) {
                array.get(5);
            }
            array.get(6);
            array.get(7);
            assertEquals(12, subscriber.get(Counter.CHUNK_DESERIALIZATIONS));
            assertEquals(36, subscriber.get(Counter.DESERIALIZATIONS));
            assertEquals(12, subscriber.get(Counter.CACHE_MISS));
            array.get(8);
            assertEquals(13, subscriber.get(Counter.CHUNK_DESERIALIZATIONS));
            assertEquals(38, subscriber.get(Counter.DESERIALIZATIONS));
            assertEquals(13, subscriber.get(Counter.CACHE_MISS));
        }
    }

    @Test
    public void
    testGetSameBreakOnLoad() throws IOException {
        Path filePath = path.resolve("testGetSameBreakOnLoad.la");
        CountingSubscriber subscriber = new CountingSubscriber();
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(5))
                .executor(new TestExecutor())
                .addSubscriber(subscriber)
                .read(read().breakOnLoaded().chunked(3))
                .write(write().chunked(3));
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            assertTrue(Files.exists(filePath));
            array.get(0);
            array.get(4);
            subscriber.reset();
            array.get(1);
            assertEquals(1, subscriber.get(Counter.DESERIALIZATIONS));
        }
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new,
                configure(configuration.file())
                        .memory(configuration.memory())
                        .addSubscriber(subscriber)
                        .executor(new TestExecutor())
                        .read(read().chunked(3))
                        .write(write().chunked(3)))) {
            assertTrue(Files.exists(filePath));
            array.get(0);
            array.get(4);
            subscriber.reset();
            array.get(1);
            assertEquals(2, subscriber.get(Counter.DESERIALIZATIONS));
        }
    }

    @Test
    public void
    testSet() throws IOException {
        Path filePath = path.resolve("testSet.la");
        CountingSubscriber subscriber = new CountingSubscriber();
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(5))
                .addSubscriber(subscriber)
                .executor(new TestExecutor())
                .read(read().breakOnLoaded().chunked(3))
                .write(write().chunked(3));
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            array.set(0, compact(0));
            array.set(5, compact(5));
            assertEquals(1, subscriber.get(Counter.SERIALIZATIONS));
            assertEquals(1, subscriber.get(Counter.WRITE_COLLISIONS));
            assertEquals(0, subscriber.get(Counter.CHUNK_SERIALIZATIONS));
            array.set(0, compact(10));
            assertEquals(2, subscriber.get(Counter.WRITE_COLLISIONS));
            assertEquals(2, subscriber.get(Counter.SERIALIZATIONS));
        }
        subscriber.reset();
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new,
                configure(configuration.file())
                        .memory(configuration.memory())
                        .addSubscriber(subscriber)
                        .executor(new TestExecutor())
                        .read(read().chunked(3))
                        .write(write().forceFlush().chunked(3)))) {
            assertTrue(Files.exists(filePath));
            array.set(0, compact(0));
            array.set(5, compact(5));
            assertEquals(1, subscriber.get(Counter.SERIALIZATIONS));
            assertEquals(1, subscriber.get(Counter.WRITE_COLLISIONS));
            assertEquals(1, subscriber.get(Counter.CHUNK_SERIALIZATIONS));
            array.set(0, compact(10));
            assertEquals(2, subscriber.get(Counter.WRITE_COLLISIONS));
            assertEquals(2, subscriber.get(Counter.CHUNK_SERIALIZATIONS));
            assertEquals(2, subscriber.get(Counter.SERIALIZATIONS));
        }
    }

    @Test
    public void
    testLinearGetSet() throws IOException {
        Path filePath = path.resolve("testLinearGetSet.la");
        CountingSubscriber subscriber = new CountingSubscriber();
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(10))
                .executor(new TestExecutor())
                .addSubscriber(subscriber)
                .read(read().linear())
                .write(write().linear());
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            assertTrue(Files.exists(filePath));
            for (int i = 0; i < array.length(); i++) {
                array.set(i, compact(i + 10));
            }
            assertEquals(10, subscriber.get(Counter.SERIALIZATIONS));
            assertEquals(0, subscriber.get(Counter.WRITE_COLLISIONS));
            assertEquals(0, subscriber.get(Counter.CHUNK_SERIALIZATIONS));
        }
        try (var array = PersistentArray.deserialize(CompactInteger.descriptor, configuration)) {
            assertTrue(Files.exists(filePath));
            for (int i = 0; i < array.length(); i++) {
                assertEquals(i + 10, array.get(i).value());
            }
            assertEquals(10, subscriber.get(Counter.DESERIALIZATIONS));
            assertEquals(10, subscriber.get(Counter.CACHE_MISS));
            assertEquals(0, subscriber.get(Counter.CHUNK_DESERIALIZATIONS));
        }
    }

    @Test
    public void
    testDataIsWrittenAndCanBeReadLater() throws IOException {
        Path filePath = path.resolve("testDataIsWrittenAndCanBeReadLater.la");
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(10))
                .read(read().chunked(10))
                .write(write().chunked(10));
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            assertTrue(Files.exists(filePath));
            for (int i = 0; i < array.length(); i++) {
                array.set(i, compact(i + 10));
            }
        }
        try (var array = PersistentArray.deserialize(CompactInteger.descriptor, configuration)) {
            for (int i = 0; i < array.length(); i++) {
                assertEquals(i + 10, array.get(i).value());
            }
        }
    }

    @Test
    public void
    testInsert() throws IOException {
        Path filePath = path.resolve("testInsert.la");
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(10))
                .read(read().chunked(10))
                .write(write().chunked(10));
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            assertTrue(Files.exists(filePath));
            array.set(0, compact(0));
            array.set(1, compact(2));
            array.set(2, compact(3));
            array.insert(1, compact(1));
        }
        try (var array = PersistentArray.deserialize(CompactInteger.descriptor, configuration)) {
            for (int i = 0; i < 4; i++) {
                assertEquals(i, array.get(i).value());
            }
        }
    }

    @Test
    public void
    testCopy() throws IOException {
        Path filePathFrom = path.resolve("testCopyFrom.la");
        Path filePathTo = path.resolve("testCopyTo.la");
        Configuration configurationFrom = configure(file(filePathFrom).cached())
                .memory(chunked(10))
                .read(read().chunked(10))
                .write(write().chunked(10));
        Configuration configurationTo = configure(file(filePathTo).cached())
                .memory(chunked(10))
                .read(read().chunked(10))
                .write(write().chunked(10));
        try (var from = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configurationFrom);
             var to = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configurationTo)) {
            for (int i = 0; i < from.length(); i ++) {
                from.set(i, compact(i));
            }
            for (int i = 0; i < to.length(); i ++) {
                to.set(i, compact(i + 10));
            }
            from.copyTo(5, to, 5, 5);
        }
        try (var array = PersistentArray.deserialize(CompactInteger.descriptor, configurationTo)) {
            for (int i = 0; i < 5; i++) {
                assertEquals(i + 10, array.get(i).value());
            }
            for (int i = 5; i < 10; i++) {
                assertEquals(i, array.get(i).value());
            }
        }
    }

    @Test
    public void
    testFill() throws IOException {
        Path filePath = path.resolve("testFill.la");
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(10))
                .read(read().chunked(10))
                .write(write().chunked(10));
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            array.fill(compact(10));
        }
        try (var array = PersistentArray.deserialize(CompactInteger.descriptor, configuration)) {
            for (int i = 0; i < array.length(); i++) {
                assertEquals(10, array.get(i).value());
            }
        }
    }

    @Test
    public void
    testClear() throws IOException {
        Path filePath = path.resolve("testFill.la");
        Configuration configuration = configure(file(filePath).cached())
                .memory(chunked(10))
                .read(read().chunked(10))
                .write(write().chunked(10));
        try (var array = new PersistentArray<>(10, CompactInteger.descriptor, FixedObjectLayout::new, configuration)) {
            array.fill(compact(10));
            for (int index = 0; index < array.length(); index++) assertNotNull(array.get(index));
            array.clear();
            for (int index = 0; index < array.length(); index++) assertNull(array.get(index));
        }
        assertThrows(FileMarkedForDeletingException.class, () -> PersistentArray.deserialize(CompactInteger.descriptor, configuration).close());
    }

}