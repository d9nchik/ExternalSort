package com.d9nich.generator;

public class MainGenerator {
    private final static int FILE_SIZE = 1_024;//in megabytes
    private final static int COUNT_OF_NUMBERS = FILE_SIZE * 1_024 * 1_024 / 4;

    public static void main(String[] args) {
        //Can be done in parallel mode
        new Thread(() -> new SequentGenerator().generate("sequent.dat", COUNT_OF_NUMBERS)).start();
        Thread second = new Thread(() -> new ReverseSequentGenerator().generate("reSequent.dat", COUNT_OF_NUMBERS));
        second.start();
        new Thread(()->new SmallGenerator().generate("small.dat", 0)).start();
        new RandomGenerator().generate("random.dat", COUNT_OF_NUMBERS);

        //Waiting before exit
        try {
            second.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
