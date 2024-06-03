package com.yokalona.array;

import com.yokalona.array.configuration.Configuration;
import com.yokalona.array.debug.CompactInteger;
import com.yokalona.array.io.FixedObjectLayout;
import com.yokalona.array.subscriber.CountingSubscriber;
import com.yokalona.array.util.Power;
import com.yokalona.tree.TestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.yokalona.array.configuration.Chunked.chunked;
import static com.yokalona.array.configuration.ChunkedRead.read;
import static com.yokalona.array.configuration.ChunkedWrite.write;
import static com.yokalona.array.configuration.Configuration.configure;
import static com.yokalona.array.configuration.File.Mode.RW;
import static com.yokalona.array.configuration.File.file;
import static com.yokalona.tree.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

class LoadArrayTest {

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
    testMemoryConsumption() throws IOException {
        CountingSubscriber statist = new CountingSubscriber();
        int length = Power.two(20);
        Configuration configuration = configure(
                file(path.resolve(UUID.randomUUID() + ".la"))
                        .mode(RW)
                        .buffer(5 * Power.two(15))
                        .cached())
                .memory(chunked(Power.two(17)))
                .addSubscriber(statist)
                .read(read().breakOnLoaded().chunked(Power.two(17)))
                .write(write().chunked(Power.two(17)));
        long writeLinear = 0L, readLinear = 0L, writeRandom = 0L, readRandom = 0L;
        printConfiguration(new TestHelper.PersistenceTest(length, configuration.memory().size(),
                configuration.read().size(),
                configuration.write().size(),
                configuration.file().buffer(),
                CompactInteger.serializer.sizeOf()));
        int repeats = 5;
        for (int i = 0; i < repeats; i++) {
            printHeader(i + 1, repeats);
            writeLinear += printStatistic("Linear write", writeLinear(length, configuration, statist), statist);
            readLinear += printStatistic("Linear read", readLinear(length, configuration), statist);
            writeRandom += printStatistic("Random write", writeRandom(length, configuration, statist), statist);
            readRandom += printStatistic("Random read", readRandom(length, configuration), statist);
        }
        printHeader();
        printAverage("linear write", (float) writeLinear / repeats, "ms");
        printAverage("linear read", (float) readLinear / repeats, "ms");
        printAverage("random write", (float) writeRandom / repeats, "ms");
        printAverage("random read", (float) readRandom / repeats, "ms");
        printAverage("read collisions", statist.average(CountingSubscriber.Counter.CACHE_MISS, 1), "ops");
        printAverage("write collisions", statist.average(CountingSubscriber.Counter.WRITE_COLLISIONS, 1), "ops");
        printStatistic("File size", getSize(Files.size(configuration.file().path())));
        printHeader();
    }

    private static long
    readLinear(int length, Configuration configuration) {
        try (var array = PersistentArray.deserialize(CompactInteger.serializer, configuration)) {
            long start = System.currentTimeMillis();
            for (int index = 0; index < length; index++) {
                CompactInteger value = array.get(index);
                assertNotNull(value);
                assertEquals(index, value.value());
                printProgress(length, index);
            }
            System.err.print("\r");
            System.err.flush();
            return System.currentTimeMillis() - start;
        }
    }

    private static long
    readRandom(int length, Configuration configuration) {
        int[] indices = new int[length];
        for (int index = 0; index < length; index++) {
            indices[index] = index;
        }
        shuffle(indices);
        try (var array = PersistentArray.deserialize(CompactInteger.serializer, configuration)) {
            array.resizeReadChunk(1);
            long start = System.currentTimeMillis();
            int ops = 0;
            for (int index : indices) {
                CompactInteger value = array.get(index);
                assertNotNull(value);
                assertEquals(index, value.value());
                printProgress(length, ++ops);
            }
            System.err.print("\r");
            System.err.flush();
            return System.currentTimeMillis() - start;
        }
    }

    private static long
    writeLinear(int length, Configuration configuration, CountingSubscriber statist) {
        long fileStart = System.currentTimeMillis();
        try (var array = new PersistentArray<>(length, CompactInteger.serializer, FixedObjectLayout::new, configuration)) {
            printStatistic("Creation time", (statist.get(CountingSubscriber.Counter.FILE_CREATED) - fileStart) + " ms");
            long start = System.currentTimeMillis();
            for (int index = 0; index < length; index++) {
                array.set(index, new CompactInteger(index));
                printProgress(length, index);
            }
            System.out.print("\r");
            System.out.flush();
            return System.currentTimeMillis() - start;
        }
    }

    private static long
    writeRandom(int length, Configuration configuration, CountingSubscriber statist) {
        int[] indices = new int[length];
        for (int index = 0; index < length; index++) {
            indices[index] = index;
        }
        shuffle(indices);
        long fileStart = System.currentTimeMillis();
        try (var array = new PersistentArray<>(length, CompactInteger.serializer, FixedObjectLayout::new, configuration)) {
            printStatistic("Creation time", (statist.get(CountingSubscriber.Counter.FILE_CREATED) - fileStart) + " ms");
            array.resizeWriteChunk(1);
            long start = System.currentTimeMillis();
            int ops = 0;
            for (int index : indices) {
                array.set(index, new CompactInteger(index));
                printProgress(length, ++ops);
            }
            System.err.print("\r");
            System.err.flush();
            return System.currentTimeMillis() - start;
        }
    }

    private static void printProgress(int length, int index) {
        int del = Math.max(1, length / 100);
        if (index % del == 0) {
            System.out.printf("\r|-record %10d out of %-10d-----+--------------------|", index, length);
            System.out.flush();
        }
    }


}