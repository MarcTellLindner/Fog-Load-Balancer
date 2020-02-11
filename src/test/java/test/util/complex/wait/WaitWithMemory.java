package test.util.complex.wait;

import test.util.complex.SomethingComplex;

public class WaitWithMemory implements SomethingComplex {

    @Override
    public boolean doSomethingComplex(int intVal, long longVal) {
        long[] memory = new long[intVal * (1_000_000 / 8)]; // Divide by 8 to 'make it' bytes
        try {
            Thread.sleep(intVal);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

}
