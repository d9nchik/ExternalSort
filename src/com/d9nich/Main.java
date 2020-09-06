package com.d9nich;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import static com.d9nich.exteranlSort.SortLargeFile.sort;

public class Main {

    public static void main(String[] args) {

        System.out.println("Data before sorting: ");
        displayFile("small.dat");

        long startTime = System.currentTimeMillis();

        // Sort largedata.dat to sortedfile.dat
        try {
            sort("small.dat", "sortedfile.dat");
        } catch (Exception e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();

        // Display the first 100 numbers in the sorted file
        System.out.println("Data after sorting: ");
        displayFile("sortedfile.dat");

        System.out.println("Time: " + (endTime - startTime) / 1000 + "s");
    }

    /**
     * Display the first 100 numbers in the specified file
     */
    public static void displayFile(String filename) {
        try {
            DataInputStream input =
                    new DataInputStream(new FileInputStream(filename));
            for (int i = 0; i < 100 && input.available() > 0; i++)
                System.out.print(input.readInt() + " ");
            input.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println();
    }
}
