package com.yokalona;

public class Validations {
    public static final String EXTERNAL_NODE_ON_A_WRONG_LEVEL = "External nodes should be present only on the last level";
    public static final String SELECTED_CHILD_IS_EXCEEDING_MAX_CAPACITY = "Selected child is exceeding max capacity";
    public static final String CAPACITY_SHOULD_BE_GREATER_THAN_2 = "Capacity should be greater than 2";
    public static final String CHILD_SHOULD_HAVE_NON_NULL_VALUE = "Child should have non null value";
    public static final String ROOT_SHOULD_HAVE_NON_NULL_VALUE = "Root should have non-null value";
    public static final String NODE_SHOULD_HAVE_NON_NULL_VALUE = "Node should have non-null value";
    public static final String KEY_SHOULD_HAVE_NON_NULL_VALUE = "Key should have non null value";
    public static final String NULL_LINK_IS_NOT_PERMITTED = "Null link is not permitted";
    public static final String DATA_BLOCK_CANNOT_BE_NULL = "Data block cannot be null";
    public static final String ORDER_CONSISTENCY_VIOLATED = "Order consistency violated";
    public static final String HEIGHT_CAN_NOT_BE_NEGATIVE = "Height can not be negative";
    public static final String CHILD_CAN_NOT_BE_NEGATIVE = "Child can not be negative";
    public static final String CAPACITY_SHOULD_BE_EVEN = "Capacity should be even";
    public static final String INCORRECT_COLORING = "Incorrect coloring";
    public static final String EXCEEDING_CAPACITY = "Exceeding capacity: ";
    public static final String EMPTY_NODE = "Node is empty";
    public static final String INDENT = "     ";


    private Validations() {}

    public static void
    validateCapacity(int capacity) {
        if (capacity <= 2) throw new IllegalArgumentException(CAPACITY_SHOULD_BE_GREATER_THAN_2);
        else if (capacity % 2 != 0) throw new IllegalArgumentException(CAPACITY_SHOULD_BE_EVEN);
    }

    public static <Key extends Comparable<Key>> void
    validateKey(Key key) {
        if (key == null) throw new IllegalArgumentException(KEY_SHOULD_HAVE_NON_NULL_VALUE);
    }
}
