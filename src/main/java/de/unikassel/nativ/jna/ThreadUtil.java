package de.unikassel.nativ.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class ThreadUtil {
    static {
        if(!System.getProperty("os.name").equals("Linux") || !System.getProperty("os.arch").equals("amd64")) {
            throw new Error("This will only work on 64bit Linux");
        }
    }

    private interface CStdLibrary extends Library {

        CStdLibrary INSTANCE = (CStdLibrary) Native.load("c", CStdLibrary.class);

        long syscall(int number, Object... args);
    }

    public static long getThreadId() {
        return CStdLibrary.INSTANCE.syscall(186);
    }
}
