package com.yokalona.file;

import java.util.Arrays;
import java.util.Iterator;

public interface Array<Type> extends Iterable<Type> {
    Type get(int index);
    int length();

    class Indexed<Type> implements Array<Type> {

        private final Type[] array;

        public Indexed(Type[] array) {
            this.array = array;
        }

        @Override
        public Type
        get(int index) {
            return array[index];
        }

        @Override
        public int
        length() {
            return array.length;
        }

        @Override
        public Iterator<Type>
        iterator() {
            return Arrays.stream(array).iterator();
        }
    }
}
