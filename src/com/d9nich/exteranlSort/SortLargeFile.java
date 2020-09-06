package com.d9nich.exteranlSort;

import java.io.*;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class SortLargeFile {
    public static final int SIZE_OF_INT = 4;
    public static final int MAX_ARRAY_SIZE = 2;//set 2 to test small.dat
    public static final int BUFFER_SIZE = 300_000 * 1_024;

    /**
     * Sort data in source file and into target file
     */
    public static void sort(String sourcefile, String targetfile) throws Exception {
        // Implement Phase 1: Create initial segments
        int numberOfSegments = initializeSegments(sourcefile);
        // Implement Phase 2: Merge segments recursively
        merge(numberOfSegments, MAX_ARRAY_SIZE, "f1.dat", "f2.dat", targetfile);
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
             RandomAccessFile output = new RandomAccessFile("f1.dat", "rw")) {
            int numberOfSegments = 0;

            //Get file channel in read-only mode
            FileChannel fileChannel = input.getChannel();
            FileChannel outputChannel = output.getChannel();

            //Get direct byte buffer access using channel.map() operation
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            MappedByteBuffer outputBuffer = outputChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileChannel.size());

            // the buffer now reads the file as if it were loaded in memory.
//        System.out.println(buffer.isLoaded());  //prints false
//        System.out.println(buffer.capacity());  //Get the size based on content size of file

            IntBuffer intBuffer = buffer.asIntBuffer();//TODO: refactor
            IntBuffer outputIntBuffer = outputBuffer.asIntBuffer();

            //You can read the file from this buffer the way you like.
            for (int k = 0; k < intBuffer.limit(); ) {
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
                    outputIntBuffer.put(list[j]);
                }
            }
            return numberOfSegments;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored ")
    private static void merge(int numberOfSegments, int segmentSize, String f1, String f2, String targetfile)
            throws Exception {
        if (numberOfSegments > 1) {
            mergeOneStep(numberOfSegments, segmentSize, f1, f2);
            merge((numberOfSegments + 1) / 2, segmentSize * 2, f2, f1, targetfile);
        } else {
            // Rename f1 as the final sorted file
            File sortedFile = new File(targetfile);
            if (sortedFile.exists()) sortedFile.delete();
            new File(f1).renameTo(sortedFile);
        }
    }

    private static void mergeOneStep(int numberOfSegments, int segmentSize, String inputFile, String outputFile)
            throws Exception {//TODO: change to try-with-resources
        final long SKIP_BYTES = (numberOfSegments / 2) * segmentSize * SIZE_OF_INT;
        RandomAccessFile f1Input = new RandomAccessFile(inputFile, "r");
        f1Input.seek(0);
        final FileChannel f1InputChannel = f1Input.getChannel();
        IntBuffer f1Buffer = f1InputChannel.map(FileChannel.MapMode.READ_ONLY, 0, SKIP_BYTES).asIntBuffer();

        RandomAccessFile f2Input = new RandomAccessFile(inputFile, "r");
        f2Input.seek(SKIP_BYTES);
        final long TOTAL_SIZE = f1InputChannel.size();
        IntBuffer f2Buffer = f2Input.getChannel().map(FileChannel.MapMode.READ_ONLY, SKIP_BYTES,
                TOTAL_SIZE - SKIP_BYTES).asIntBuffer();

        // Copy half number of segments from inputFile.dat to output.dat
        // Merge remaining segments in inputFile with segments in output into f3
        RandomAccessFile output = new RandomAccessFile(outputFile, "rw");
        output.setLength(0);
        IntBuffer outputBuffer = output.getChannel().map(FileChannel.MapMode.READ_WRITE, 0,
                TOTAL_SIZE).asIntBuffer();
        mergeSegments(numberOfSegments / 2, segmentSize, f1Buffer, f2Buffer, outputBuffer);
        f1Input.close();
        f2Input.close();
        output.close();
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
    private static void mergeSegments(int numberOfSegments, int segmentSize, IntBuffer f1, IntBuffer f2,
                                      IntBuffer f3) {
        for (int i = 0; i < numberOfSegments; i++)
            mergeTwoSegments(segmentSize, f1, f2, f3);

        // If f1 has one extra segment, copy it to f3
        while (available(f2)) {
            f3.put(f2.get());
        }

    }

    /**
     * Merges two segments
     */
    private static void mergeTwoSegments(int segmentSize, IntBuffer f1, IntBuffer f2, IntBuffer f3) {
        int intFromF1 = f1.get();
        int intFromF2 = f2.get();
        int f1Count = 1;
        int f2Count = 1;
        while (true) {
            if (intFromF1 < intFromF2) {
                f3.put(intFromF1);
                if (!available(f1) || f1Count++ >= segmentSize) {
                    f3.put(intFromF2);
                    break;
                } else {
                    intFromF1 = f1.get();
                }
            } else {
                f3.put(intFromF2);
                if (!available(f2) || f2Count++ >= segmentSize) {
                    f3.put(intFromF1);
                    break;
                } else {
                    intFromF2 = f2.get();
                }
            }
        }
        while (available(f1) && f1Count++ < segmentSize) {
            f3.put(f1.get());
        }
        while (available(f2) && f2Count++ < segmentSize) {
            f3.put(f2.get());
        }
    }

    private static boolean available(IntBuffer buffer) {
        return buffer.position() < buffer.limit();
    }
}
