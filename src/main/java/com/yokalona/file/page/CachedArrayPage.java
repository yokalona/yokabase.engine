package com.yokalona.file.page;

import com.yokalona.file.CachedArrayProvider;

import java.util.Comparator;
import java.util.Iterator;

public class CachedArrayPage<Type> extends CachedPage<Type> implements ArrayPage<Type> {

    public final ArrayPage<Type> page;

    public CachedArrayPage(ArrayPage<Type> page) {
        super(page);
        this.page = page;
    }

    @Override
    public void
    set(int index, Type value) {
        page.set(index, value);
        // TODO: should be invalidate index
        array.invalidate();
    }

    @Override
    public void
    insert(int index, Type value) {
        page.insert(index, value);
        array.invalidate();
        array.length(page.size());
    }

    @Override
    public void
    swap(int left, int right) {
        page.swap(left, right);
    }

    @Override
    public int
    find(Type value, Comparator<Type> comparator) {
        return page.find(value, comparator);
    }

    @Override
    public Type
    first() {
        return page.first();
    }

    @Override
    public Type
    last() {
        return page.last();
    }

    @Override
    public int
    length() {
        return page.length();
    }

    @Override
    public Iterator<Type> iterator() {
        return new Iterator<>() {
            private int current = -1;
            private boolean switched = false;

            @Override
            public boolean hasNext() {
                switched = true;
                if (current + 1 < size()) {
                    current++;
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public Type next() {
                return get(current);
            }

            @Override
            public void remove() {
                if (!switched) throw new IllegalStateException();
                CachedArrayPage.this.remove(current--);
                switched = false;
            }
        };
    }
}
