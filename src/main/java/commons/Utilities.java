package commons;

import java.io.IOException;

public class Utilities {

    public static int indexOfByte(byte[] data, int item, int startIndex){
        for (int i = startIndex; i < data.length; i++) {
            var b = data[i];
            if (b == item)
                return i;
        }
        return -1;
    }

    public static byte[] combine(byte[] one, byte[] two)
    {
        byte[] combined = new byte[one.length + two.length];

        System.arraycopy(one,0,combined,0         ,one.length);
        System.arraycopy(two,0,combined,one.length,two.length);
        return combined;
    }

    public static void timedRun(boolean timed, String msg, Thunk f) {
        try {
            if (timed) {
                System.out.print(msg + " ... \t\t");
                var start = System.currentTimeMillis();
                f.apply();
                var end = System.currentTimeMillis();
                var msCount = (end - start) / 1000.0;
                System.out.println("Time: " + msCount + " sec.");
            } else {
                f.apply();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
