package compact.writer;

import compact.DeduplicatedDictionary;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class BatchWrapperWriter {
    private static byte[] intData = new byte[14];
    private final int MAX_CACHED_INT = 10000;
    private final int fullLen;
    private OutputStream out;
    private byte[] internalBuffer = new byte[40_000_000];
    private byte[][] _cachedValuesArr = new byte[MAX_CACHED_INT][];
    private ArrayList<byte[]> _cachedValues2 = new ArrayList<>();
    private int pos = 0;

    private byte[] sequenceBuffer = new byte[20];

    public BatchWrapperWriter(OutputStream out) {
        this.out = out;
        this.fullLen = internalBuffer.length;
        for (int i = 0; i < MAX_CACHED_INT; i++) {
            _cachedValuesArr[i] = ("" + i).getBytes();
        }
        for (int i = 0; i < 100; i++) {

            _cachedValues2.add(((i < 10) ? "0" + i : "" + i).getBytes());
        }
    }

    private static char charLetterDecode(int ch) {
        switch (ch) {
            case 1:
                return 'C';
            case 2:
                return 'G';
            case 3:
                return 'T';
            case 4:
                return 'N';
            default:
                return 'A';
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

    public void write(byte[] arr, int arrPos, int len) throws IOException {
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

    public void writeSequence(LongArrayList fullSequences, int start, int len) throws IOException {
        if (sequenceBuffer.length < len) {
            sequenceBuffer = new byte[len];
        }

        var divide = 0;
        var remainderIndex = 0;
        long charSeq = fullSequences.getLong(start);
        for (var i = 0; i < len; i++) {
            var charShifted = (int) (charSeq >> (remainderIndex * 3)) & 7;
            var charDecoded = charLetterDecode(charShifted);
            sequenceBuffer[i] = ((byte) charDecoded);
            remainderIndex++;
            if (remainderIndex == 21) {
                remainderIndex = 0;
                divide++;
                charSeq = fullSequences.getLong(start + divide);
            }
        }
        write(sequenceBuffer, 0, len);
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

    public void printInt(IntArrayList values, int index) throws IOException {
        printInt(values.getInt(index));
    }

    public void printInt(CharArrayList values, int index) throws IOException {
        printInt(values.getChar(index));
    }

    public void printInt(ByteArrayList values, int index) throws IOException {
        printInt(values.getByte(index));
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
        if (flag < MAX_CACHED_INT) {
            write(_cachedValuesArr[flag]);
            return;
        }
        printBigInt(flag);
    }

    public void write(byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    public void writeStringByIndex(DeduplicatedDictionary stringDict, int index) throws IOException {
        var indexInDict = stringDict.Items.getInt(index);
        byte[] getItemByIndex = stringDict.Values.get(indexInDict);
        write(getItemByIndex);
    }

}
