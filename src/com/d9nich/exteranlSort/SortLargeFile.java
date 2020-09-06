package com.d9nich.exteranlSort;

import java.io.*;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class SortLargeFile {
    public static final int MAX_ARRAY_SIZE = 500_000_000;//set 2 to test small.dat
    public static final int BUFFER_SIZE = 500_000_000;

    /**
     * Sort data in source file and into target file
     */
    public static void sort(String sourcefile, String targetfile) throws Exception {
        // Implement Phase 1: Create initial segments
        int numberOfSegments = initializeSegments(sourcefile);
        // Implement Phase 2: Merge segments recursively
        merge(numberOfSegments, MAX_ARRAY_SIZE, "f1.dat", "f2.dat", "f3.dat", targetfile);
    }

    /**
     * Sort original file into sorted segments
     * Very cool thing is <b>MMF(Memory-Mapped File)</b>
     * I've been inspired by java community in
     * <a href="https://stackoverflow.com/questions/16022053/fastest-way-to-read-huge-number-of-int-from-binary-file">
     * StackOverFlow</a>
     * Also read: <a href="https://howtodoinjava.com/java/nio/memory-mapped-files-mappedbytebuffer/">
     * MMF in Java</a>
     */
    private static int initializeSegments(String originalFile) {
        int[] list = new int[SortLargeFile.MAX_ARRAY_SIZE];
        try (FileInputStream input = new FileInputStream(originalFile);
             DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("f1.dat")))) {
            int numberOfSegments = 0;

            //Get file channel in read-only mode
            FileChannel fileChannel = input.getChannel();

            //Get direct byte buffer access using channel.map() operation
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());

            // the buffer now reads the file as if it were loaded in memory.
//        System.out.println(buffer.isLoaded());  //prints false
//        System.out.println(buffer.capacity());  //Get the size based on content size of file

            IntBuffer intBuffer = buffer.asIntBuffer();

            //You can read the file from this buffer the way you like.
            for (int k = 0; k < intBuffer.limit(); k++) {
                numberOfSegments++;
                int i = 0;
                for (; k < intBuffer.limit() && i < SortLargeFile.MAX_ARRAY_SIZE; i++, k++) {
                    list[i] = intBuffer.get();
                }
                // Sort an array list[0..i−1]
                //Increased speed on multicore devices.
                java.util.Arrays.parallelSort(list, 0, i);
                // Write the array to f1.dat
                for (int j = 0; j < i; j++) {
                    output.writeInt(list[j]);
                }
            }
            input.close();
            output.close();
            return numberOfSegments;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored ")
    private static void merge(int numberOfSegments, int segmentSize, String f1, String f2, String f3, String targetfile)
            throws Exception {
        if (numberOfSegments > 1) {
            mergeOneStep(numberOfSegments, segmentSize, f1, f2, f3);
            merge((numberOfSegments + 1) / 2, segmentSize * 2, f3, f1, f2, targetfile);
        } else {
            // Rename f1 as the final sorted file
            File sortedFile = new File(targetfile);
            if (sortedFile.exists()) sortedFile.delete();
            new File(f1).renameTo(sortedFile);
        }
    }

    private static void mergeOneStep(int numberOfSegments, int segmentSize, String f1, String f2, String f3)
            throws Exception {//TODO: change to try-with-resources
        DataInputStream f1Input = new DataInputStream(new BufferedInputStream(new FileInputStream(f1), BUFFER_SIZE));
        DataOutputStream f2Output = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(f2), BUFFER_SIZE));// Copy half number of segments from f1.dat to f2.dat
        copyHalfToF2(numberOfSegments, segmentSize, f1Input, f2Output);
        f2Output.close();
        // Merge remaining segments in f1 with segments in f2 into f3
        DataInputStream f2Input = new DataInputStream(
                new BufferedInputStream(new FileInputStream(f2), BUFFER_SIZE));
        DataOutputStream f3Output = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(f3), BUFFER_SIZE));
        mergeSegments(numberOfSegments / 2, segmentSize, f1Input, f2Input, f3Output);
        f1Input.close();
        f2Input.close();
        f3Output.close();
    }

    /**
     * Copy first half number of segments from f1.dat to f2.dat
     */
    private static void copyHalfToF2(int numberOfSegments, int segmentSize, DataInputStream f1, DataOutputStream f2)
            throws Exception {
        for (int i = 0; i < (numberOfSegments / 2) * segmentSize; i++) {
            f2.writeInt(f1.readInt());
        }
    }

    /**
     * Merge all segments
     */
    private static void mergeSegments(int numberOfSegments, int segmentSize, DataInputStream f1, DataInputStream f2,
                                      DataOutputStream f3) throws Exception {
        for (int i = 0; i < numberOfSegments; i++)
            mergeTwoSegments(segmentSize, f1, f2, f3);

        // If f1 has one extra segment, copy it to f3
        while (f1.available() > 0)
            f3.writeInt(f1.readInt());
    }

    /**
     * Merges two segments
     */
    private static void mergeTwoSegments(int segmentSize, DataInputStream f1, DataInputStream f2, DataOutputStream f3)
            throws Exception {
        int intFromF1 = f1.readInt();
        int intFromF2 = f2.readInt();
        int f1Count = 1;
        int f2Count = 1;
        while (true) {
            if (intFromF1 < intFromF2) {
                f3.writeInt(intFromF1);
                if (f1.available() == 0 || f1Count++ >= segmentSize) {
                    f3.writeInt(intFromF2);
                    break;
                } else {
                    intFromF1 = f1.readInt();
                }
            } else {
                f3.writeInt(intFromF2);
                if (f2.available() == 0 || f2Count++ >= segmentSize) {
                    f3.writeInt(intFromF1);
                    break;
                } else {
                    intFromF2 = f2.readInt();
                }
            }
        }
        while (f1.available() > 0 && f1Count++ < segmentSize) {
            f3.writeInt(f1.readInt());
        }
        while (f2.available() > 0 && f2Count++ < segmentSize) {
            f3.writeInt(f2.readInt());
        }
    }
}
