package com.d9nich.generator;

public interface Generatable {
    /**
     * @param fileName name of resulting file
     * @param dataSize how many integers we should write to file
     */
    void generate(String fileName, int dataSize);
}
