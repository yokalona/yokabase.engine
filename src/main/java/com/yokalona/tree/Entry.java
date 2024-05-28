package com.yokalona.tree;

public record Entry<Key extends Comparable<Key>, Value>(Key key, Value value) {
}
