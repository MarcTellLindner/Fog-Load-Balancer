package test.util.complex.sort;

import test.util.complex.SomethingComplex;

import java.util.stream.LongStream;

public class BubbleSort implements SomethingComplex {

    @Override
    public boolean doSomethingComplex(int intVal, long longVal) {
        long[] values = LongStream.generate(() -> (long) (Math.random() * longVal)).limit(intVal).toArray();
        sort(values);
        return true;
    }

    private void sort(long[] values) {
        for(int i = 0; i < values.length - 1; ++i) {
            for(int j = 0; j < values.length - i - 1; ++j) {
                if (values[j] > values[j + 1]) {
                    values[j] ^= values[j + 1];
                    values[j + 1] ^= values[j];
                    values[j] ^= values[j + 1];
                }
            }
        }
    }
}
