package com.yokalona.file;

import com.yokalona.array.serializers.primitives.StringSerializer;
import com.yokalona.array.util.Power;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VSPageTest {

    @Test
    void testSet() {
        VSPage<String> page = new VSPage<>(4, new StringSerializer());
        page.append("VSPage is variable type page");
        page.append("that can feet variable type data");
        page.append("which is useful to store strings or array");
        assertEquals("VSPage is variable type page", page.get(0));
        assertEquals("that can feet variable type data", page.get(1));
        assertEquals("which is useful to store strings or array", page.get(2));
        page.set(1, "that can fit variable type data");
        assertEquals("that can fit variable type data", page.get(1));
        page.set(2, "however, it is more complex, than regular ASPage");
        assertEquals("however, it is more complex, than regular ASPage", page.get(2));
    }

    @Test
    void testAppend2() {
        VSPage<String> page = new VSPage<>(Power.two(15), new StringSerializer());
        String uuid = UUID.randomUUID().toString();
        int actualSize = uuid.length();
        long start = System.currentTimeMillis();
        while (page.canFit(StringSerializer.INSTANCE.sizeOf(uuid))) {
            page.append(uuid);
            uuid = UUID.randomUUID().toString();
            actualSize += uuid.length();
        }
        long end = System.currentTimeMillis();
        System.out.printf("""
                             Space occupied in VSPage   %10d bytes
                             Actual data size           %10d bytes
                             Total space taken          %10d bytes
                             Time taken                 %10d ms%n""",
                page.occupied(), actualSize, page.space, end - start);
        System.out.println(page.size());
    }

}