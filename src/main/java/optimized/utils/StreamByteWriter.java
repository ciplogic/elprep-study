package optimized.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class StreamByteWriter {

    private static byte[] intData = new byte[14];
    private final int MAX_CACHED_INT = 10000;
    private final int fullLen;
    private OutputStream out;
    private byte[] internalBuffer = new byte[4000_000];
    private byte[][] _cachedValuesArr = new byte[MAX_CACHED_INT][];
    private ArrayList<byte[]> _cachedValues2 = new ArrayList<>();
    private int pos = 0;

    public StreamByteWriter(OutputStream out) {
        this.out = out;
        this.fullLen = internalBuffer.length;
        for (int i = 0; i < MAX_CACHED_INT; i++) {
            _cachedValuesArr[i] = ("" + i).getBytes();
        }
        for (int i = 0; i < 100; i++) {

            _cachedValues2.add(((i < 10) ? "0" + i : "" + i).getBytes());
        }
    }

    public void close() throws IOException {

        out.write(internalBuffer, 0, pos);
    }

    public void writeByte(byte value) throws IOException {
        internalBuffer[pos++] = value;
        if (pos == fullLen)
            flush();

    }

    void write(byte[] arr, int arrPos, int len) throws IOException {
        while (len >= fullLen - pos) {
            int subLen = fullLen - pos;
            System.arraycopy(arr, arrPos, internalBuffer, this.pos, subLen);
            arrPos += subLen;
            len -= subLen;
            flush();

        }
        System.arraycopy(arr, arrPos, internalBuffer, this.pos, len);
        this.pos += len;

    }

    private void flush() throws IOException {
        out.write(internalBuffer, 0, fullLen);
        pos = 0;
    }

    public void printBigInt(int flag) throws IOException {

        int countDigits = 0;
        int origFlag = flag;

        while (flag >= 10000) {
            countDigits += 4;
            flag /= 10000;
        }
        while (flag != 0) {
            countDigits++;
            flag /= 10;
        }
        flag = origFlag;
        writeValueOfCountDigits(flag, countDigits);

    }

    private void writeValueOfCountDigits(int flag, int countDigits) throws IOException {

        int pos = countDigits - 1;
        while (countDigits > 1) {
            byte[] remainderBuf = _cachedValues2.get(flag % 100);
            flag /= 100;
            intData[pos--] = remainderBuf[1];
            intData[pos--] = remainderBuf[0];
            countDigits -= 2;

        }
        if (countDigits == 1) {
            intData[0] = (byte) ('0' + flag);
        }
        write(intData, 0, countDigits);
    }

    public void printInt(int flag) throws IOException {
        if (flag == 0) {
            writeByte((byte) '0');
            return;
        }
        if (flag < 0) {
            writeByte((byte) '-');
            printInt(-flag);
            return;
        }
        if (flag <= MAX_CACHED_INT) {
            write(_cachedValuesArr[flag]);
            return;
        }
        printBigInt(flag);
    }

    private void write(byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

}
