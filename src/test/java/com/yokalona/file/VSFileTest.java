package com.yokalona.file;

import com.yokalona.array.configuration.File;
import com.yokalona.array.serializers.primitives.StringSerializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VSFileTest {

    @Test
    void test() throws IOException {
//        Path td = Files.createTempDirectory("vs");
//        Path td = Files.createDirectory(Path.of("vs"));
//        File cached = File.file(Path.of("file.vs")).uncached();
//        VSFile<String> file = new VSFile<>(new StringSerializer(), cached);
//        file.create();
//        try (var page = file.page(0)) {
//            page.page().append("abc");
//        }
//        file.flush();
//
//        byte[] bytes = Files.readAllBytes(Path.of("file.vs"));
//        prettyPrint(bytes);
    }

    private static void
    prettyPrint(byte[] space) {
        System.out.printf("%3s%-3s|", "-", "-");
        for (int i = 0; i < 10; i ++) {
            System.out.printf("%6d", i);
        }
        System.out.println("\n------+------------------------------------------------------------+------");
        int count = 0;
        for (int i = 0; i < space.length; i += 10) {
            System.out.printf("%6d|", count);
            for (int j = i; j < Math.min(i + 10, space.length); j ++) {
                if (space[j] < 'a') System.out.printf("%6d", space[j]);
                else System.out.printf("%6s", (char) space[j]);
            }
            System.out.printf("|%-6d%n", count ++);
        }
        System.out.printf("%3s%-3s|", "-", "-");
        for (int i = 0; i < 10; i ++) {
            System.out.printf("%6d", i);
        }
        System.out.println();
    }

}