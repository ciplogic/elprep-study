package commons;

import java.io.IOException;

public class Utilities {

    public static void timedRun(boolean timed, String msg, Thunk f) {
        try {
            if (timed) {
                System.out.print(msg + " ... \t\t");
                var start = System.currentTimeMillis();
                f.apply();
                var end = System.currentTimeMillis();
                System.out.println("" + (end - start) + " ms.");
            } else {
                f.apply();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
