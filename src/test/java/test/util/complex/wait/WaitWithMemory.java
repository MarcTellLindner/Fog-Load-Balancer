package test.util.complex.wait;

import test.util.complex.SomethingComplex;

public class WaitWithMemory implements SomethingComplex {

    @Override
    public boolean doSomethingComplex(int intVal, long longVal) {
        try {
            long[] memory = new long[intVal * (1_000_000 / 8)]; // Divide by 8 to 'make it' bytes
            Thread.sleep(intVal);
        } catch (OutOfMemoryError | InterruptedException e) {
            return false;
        }
        return true;
    }

}
