package com.d9nich.generator;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class SmallGenerator implements Generatable {

    /**
     * @param fileName name of resulting file
     * @param dataSize not used only to support interface
     */
    @Override
    public void generate(String fileName, long dataSize) {
        int[] numbers = {2, 3, 4, 0, 5, 6, 7, 9, 8, 1};
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))) {
            for (int number : numbers)
                output.writeInt(number);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
