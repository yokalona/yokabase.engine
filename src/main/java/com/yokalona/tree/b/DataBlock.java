package com.yokalona.tree.b;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.yokalona.Validations.KEY_SHOULD_HAVE_NON_NULL_VALUE;

public class DataBlock<Key extends Comparable<Key>, Data extends WithKey<? extends Key>>
        implements Iterable<Data> {
    private final Data[] array;
    private int size;
    final Find find = new Find();
    final Loader loader = new Loader();

    public DataBlock(Supplier<? extends Data[]> supplier) {
        this.array = supplier.get();
    }

    public Data
    get(int index) {
        if (!loader.loaded) loader.load();
        return array[index];
    }

    public void
    replace(int index, Data value) {
        assert value != null : "Null values are not permitted";

        if (!loader.loaded) loader.load();
        this.array[index] = value;
        assert check();
    }

    public void
    insert(Data value) {
        assert value != null : "Null values are not permitted";

        if (!loader.loaded) loader.load();
        array[size++] = value;
        assert check();
    }

    public void
    insert(int index, Data value) {
        assert value != null : "Null values are not permitted";

        if (!loader.loaded) loader.load();
        System.arraycopy(array, index, array, index + 1, size - index);
        array[index] = value;
        size++;

        assert check();
    }

    public void
    remove(int index) {
        if (!loader.loaded) loader.load();
        System.arraycopy(array, index + 1, array, index, size - index - 1);
        array[--size] = null;

        assert check();
    }

    private void
    copyTo(DataBlock<Key, Data> other, int from, int to, int length) {
        if (!loader.loaded) loader.load();
        System.arraycopy(array, from, other.array, to, length);
        other.size += length;

        assert check();
        assert other.check();
    }

    public void
    splitWith(DataBlock<Key, Data> other) {
        if (!loader.loaded) loader.load();
        copyTo(other, array.length / 2, 0, array.length / 2);
        remove(array.length / 2, array.length);

        assert check();
        assert other.check();
    }

    public void
    mergeWith(DataBlock<Key, Data> other) {
        if (!loader.loaded) loader.load();
        copyTo(other, 0, other.size, size);

        assert check();
        assert other.check();
    }

    public void
    remove(int from, int to) {
        if (!loader.loaded) loader.load();
        Arrays.fill(array, from, to, null);
        size -= to - from;

        assert check();
    }

    public int
    length() {
        if (!loader.loaded) loader.load();
        return this.array.length;
    }

    public int
    size() {
        return this.size;
    }

    @Override
    public Iterator<Data>
    iterator() {
        if (!loader.loaded) loader.load();
        return Arrays.stream(this.array, 0, size).iterator();
    }

    @Override
    public void
    forEach(Consumer<? super Data> action) {
        if (!loader.loaded) loader.load();
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<Data>
    spliterator() {
        if (!loader.loaded) loader.load();
        return Arrays.spliterator(array);
    }

    boolean check() {
        if (!loader.loaded) loader.load();
        checkConsistency();
        assert isOrdered();
        return true;
    }

    void
    checkConsistency() {
        assert size >= 0;
        assert size <= array.length;
        for (int point = 0; point < size; point++) assert array[point] != null;
        for (int point = size; point < array.length; point++) assert array[point] == null;
    }

    @SuppressWarnings("unchecked")
    boolean
    isOrdered() {
        Key previous = null;
        for (int point = 0; point < size; point++) {
            if (previous == null || array[point].key().compareTo(previous) > 0) previous = array[point].key();
            else return false;
        }
        return true;
    }

    class Find {

        public int equal(final Key key) {
            assert key != null : KEY_SHOULD_HAVE_NON_NULL_VALUE;

            int left = 0, right = size - 1;
            while (left <= right) {
                int mid = left + (right - left) / 2;
                int comparison = array[mid].key().compareTo(key);
                if (comparison > 0) right = mid - 1;
                else if (comparison < 0) left = mid + 1;
                else return mid;
            }
            return -(left + 1);
        }

        // TODO: solve
        public int position(final Key key) {
            assert key != null : KEY_SHOULD_HAVE_NON_NULL_VALUE;

            int left = 0, right = size - 1;
            while (left <= right) {
                int mid = left + (right - left) / 2;
                int comparison = array[mid].key().compareTo(key);
                if (comparison > 0) right = mid - 1;
                else if (comparison < 0) left = mid + 1;
                else return mid;
            }
            return -left;
        }

        public int lessThan(final Key key) {
            int position = position(key);
            if (position >= 0) return position - 1;
            else return (position * -1) - 1;
        }

        public int greaterThan(final Key key) {
            int position = position(key);
            if (position >= 0) return position + 1;
            else return position * -1;
        }
    }

     class Loader {

        private boolean loaded = false;

        private void load() {
            // do nothing for now
            this.loaded = true;
            Ignore.that(array);
        }

        public void unload() {
            this.loaded = false;
            Ignore.that(array);
        }

    }

    private static class Ignore {
        static void that(Object ignored) {}
    }
}
