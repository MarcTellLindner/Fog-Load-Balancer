package test.util.complex.sort;

import test.util.complex.SomethingComplex;

import java.util.stream.LongStream;

public class BubbleSort implements SomethingComplex<long[]> {

    @Override
    public long[] doSomethingComplex(int howOften, long howBig) {
        long[] values = LongStream.generate(() -> (long) (Math.random() * howBig)).limit(howOften).toArray();
        sort(values);
        return values;
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
