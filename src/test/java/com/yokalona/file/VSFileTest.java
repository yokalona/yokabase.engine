package com.yokalona.file;

import com.yokalona.array.configuration.File;
import com.yokalona.array.serializers.primitives.CompactIntegerSerializer;
import com.yokalona.tree.TestHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class VSFileTest {

    @Test
    void test() throws IOException {
        Path vs = Path.of("vs");
        if (!Files.exists(vs)) Files.createDirectory(vs);
        Path path = vs.resolve("vsfile.yaga");
        Files.deleteIfExists(path);
        File cached = File.file(path).cached();
        VSFile<Integer> file = new VSFile<>(new CompactIntegerSerializer(Integer.BYTES),
                new VSFile.Configuration(cached));

        for (int i = 0; i < 10; i++) {
            file.append(TestHelper.RANDOM.nextInt());
        }
        byte[] bytes = Files.readAllBytes(path);
        TestHelper.prettyPrint(bytes);
    }

}