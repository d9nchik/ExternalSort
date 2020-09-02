package com.d9nich.generator;

import java.io.*;

public class SequentGenerator implements Generatable {


    @Override
    public void generate(String fileName, int dataSize) {
        int number = Integer.MIN_VALUE;
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))) {
            for (int i = 0; i < dataSize; i++)
                output.writeInt(number++);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
