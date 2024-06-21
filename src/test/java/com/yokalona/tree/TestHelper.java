package com.yokalona.tree;

import com.yokalona.array.subscriber.CountingSubscriber;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class TestHelper {

    public static final Random RANDOM = new Random();
    private static final long kilo = 1024;
    private static final long mega = kilo * kilo;
    private static final long giga = mega * kilo;
    private static final long tera = giga * kilo;
    public static final String ALPHABET = "abcdefghjiklmopqrstuvxyz0123456789";

    public static double log(int number, int base) {
        return Math.log(number) / Math.log(base);
    }

    public static void
    shuffle(int[] array) {
        for (int idx = array.length - 1; idx > 0; idx--) {
            int element = (int) Math.floor(Math.random() * (idx + 1));
            swap(array, idx, element);
        }
    }

    public static int[]
    arrayOfSize(int size) {
        int [] array = new int[size];
        for (int i = 0; i < array.length; i ++) array[i] = i;
        TestHelper.shuffle(array);
        return array;
    }

    public static <Type extends Comparable<Type>> void shuffle(Type[] array) {
        for (int idx = array.length - 1; idx > 0; idx--) {
            int element = (int) Math.floor(Math.random() * (idx + 1));
            swap(array, idx, element);
        }
    }

    private static void
    swap(int[] arr, int left, int right) {
        int tmp = arr[left];
        arr[left] = arr[right];
        arr[right] = tmp;
    }

    private static <Type extends Comparable<Type>> void swap(Type[] arr, int left, int right) {
        Type tmp = arr[left];
        arr[left] = arr[right];
        arr[right] = tmp;
    }

    public static String
    getSize(long size) {
        double kb = (double) size / kilo, mb = kb / kilo, gb = mb / kilo, tb = gb / kilo;
        if (size < kilo) return size + " b";
        else if (size < mega) return String.format("%.2f Kb", kb);
        else if (size < giga) return String.format("%.2f Mb", mb);
        else if (size < tera) return String.format("%.2f Gb", gb);
        else return String.format("%.2f Tb", tb);
    }

    public static String
    getTime(long ms) {
        int seconds = (int) (ms / 1000) % 60 ;
        int minutes = (int) ((ms / (1000 * 60)) % 60);
        String result = "";
        if (minutes > 0) result += minutes + "m ";
        if (seconds > 0) result += seconds + "s ";
        return result + (ms % 1000);
    }

    public static void
    printHeader() {
        System.out.println("|=========================================+====================|");
    }

    public static void
    printHeader(int repeat, int total) {
        String header = """
                |-----------------------------------------+--------------------|
                | Iteration                               |    %5d/%-5d     |%n""";
        System.out.printf(header, repeat, total);
    }

    public static void
    printConfiguration(PersistenceTest test) {
        System.out.printf("""
                        |=========================================+====================|
                        | Randomized Access Test(RAT)             |                    |
                        |=========================================+====================|
                        | Array size                              | %10d records |
                        |     Chunks                              |                    |
                        |         Read                            | %10d records |
                        |         Write                           | %10d records |
                        |         Memory                          | %10d records |
                        |     Shared buffer size                  | %10d bytes   |
                        |     Serialized record size              | %10d bytes   |%n""",
                test.size, test.read, test.write, test.memory, test.buffer, test.typeSize);
    }

    public static void
    printStatistic() {
        System.out.println("""
                |-----------------------------------------+--------------------|
                |                                                              |""");
    }

    public static long
    printStatistic(String name, long time, CountingSubscriber statist) {
        System.out.printf("""
                        |                                         |--------------------|
                        |     %-15s                     | %10d ms      |
                        |     Cache misses                        | %10d times   |
                        |     Chunk serializations                | %10d times   |
                        |     Chunk deserializations              | %10d times   |
                        |     Write collision                     | %10d times   |
                        |     Serializations                      | %10d times   |
                        |     Deserializations                    | %10d times   |%n""",
                name, time,
                statist.get(CountingSubscriber.Counter.CACHE_MISS),
                statist.get(CountingSubscriber.Counter.CHUNK_SERIALIZATIONS),
                statist.get(CountingSubscriber.Counter.CHUNK_DESERIALIZATIONS),
                statist.get(CountingSubscriber.Counter.WRITE_COLLISIONS),
                statist.get(CountingSubscriber.Counter.SERIALIZATIONS),
                statist.get(CountingSubscriber.Counter.DESERIALIZATIONS));
        statist.reset();
        return time;
    }

    public static void
    printStatistic(String name, String value) {
        System.out.printf("""
                        |                                         |--------------------|
                        |     %-15s                     |    %10s      |%n""",
                name, value);
    }

    public static void
    printAverage(String name, float value, String measurement) {
        if (value >= 0) System.out.printf("""
                |     Average %-16s            | %10.2f %-5s   |%n""", name, value, measurement);
        else System.out.printf("""
                |     Average %-16s            |        N/A %-5s   |%n""", name, measurement);
    }

    public static void
    prettyPrint(byte[] space) {
        System.out.printf("%3s%-3s|", "-", "-");
        for (int i = 0; i < 10; i++) {
            System.out.printf("%6d", i);
        }
        System.out.println("\n------+------------------------------------------------------------+------");
        int count = 0;
        for (int i = 0; i < space.length; i += 10) {
            System.out.printf("%6d|", count);
            int min = Math.min(i + 10, space.length);
            for (int j = i; j < min; j++) {
                if (printable(space[j]))
                    System.out.printf("%6s", (char) space[j]);
                else System.out.printf("%6d", space[j]);
            }
            if (min < i + 10) System.out.printf("%" + (i + 10 - min) * 6 + "s|%-5d%n", "", count);
            else System.out.printf("|%-6d%n", count++);
        }
        System.out.println("------+------------------------------------------------------------+------");
        System.out.printf("%3s%-3s|", "-", "-");
        for (int i = 0; i < 10; i++) {
            System.out.printf("%6d", i);
        }
        System.out.println();
    }

    private static boolean
    printable(byte space) {
        return space >= 33 && space < 127;
    }

    public static String
    generate(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String
    randomString(int length) {
        byte[] bytes = new byte[length];
        return generate(bytes);
    }

    public static String
    randomString(int min, int max) {
        if (min == max) return generate(new byte[min]);
        byte[] bytes = new byte[RANDOM.nextInt(min, max)];
        return generate(bytes);
    }

    public record PersistenceTest(int size, int memory, int read, int write, int buffer, int typeSize) {

        public PersistenceTest {
            size = Math.max(1, size);
            memory = Math.max(1, memory);
            read = Math.max(1, read);
            write = Math.max(1, write);
        }
    }
}
