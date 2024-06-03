package com.yokalona.file;

public interface File<Type> {
    /**
     * @return count of pages;
     */
    int pages();

    /**
     * Writes data by index to the appropriate page. This action might cause page overflow and spill.
     *
     * @return pointer to page and internal offset within the page
     */
    Pointer write(int index, Type data);

    /**
     * @return data by page pointer.
     */
    Type read(Pointer pointer);
}
