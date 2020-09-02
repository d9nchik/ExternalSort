package com.d9nich.generator;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class RandomGenerator implements Generatable {
    @Override
    public void generate(String fileName, int dataSize) {
        Random random = new Random();
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))) {
            for (int i = 0; i < dataSize; i++)
                output.writeInt(random.nextInt());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
