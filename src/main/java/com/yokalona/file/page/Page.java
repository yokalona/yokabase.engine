package com.yokalona.file.page;

import com.yokalona.array.serializers.FixedSizeSerializer;
import com.yokalona.array.serializers.Serializer;
import com.yokalona.file.Array;

public interface Page<Type> {

    /**
     * @return number of records within the page
     */
    int size();

    /**
     * @return free space size in bytes
     */
    int free();

    /**
     * @return taken space size in bytes
     */
    int occupied();

    Type get(int index);

    int append(Type value);

    int remove(int index);

    void flush();

    FixedSizeSerializer<Type> serializer();

    ASPage.Configuration configuration();

    Array<Type> read(Class<Type> type);

    void clear();
}
