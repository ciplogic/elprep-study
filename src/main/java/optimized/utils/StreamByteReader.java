package optimized.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class StreamByteReader {
    //    final static int SIZE_INTERNAL_BUFFER = 10;
    final static int SIZE_INTERNAL_BUFFER = 1000_000;
    private final InputStream input;
    int pos;
    private byte[] internaBuffer = new byte[SIZE_INTERNAL_BUFFER];
    private byte[] readStr = new byte[2000];
    private int stringLen;
    private int readLen;

    public StreamByteReader(InputStream input) throws IOException {

        this.input = input;
        fillBuffer();

    }

    public boolean readRow() throws IOException {
        if (readLen == -1)
            return false;
        stringLen = 0;
        int startPos = pos;
        while (true) {
            if (pos == internaBuffer.length) {

                int len = pos - startPos;
                System.arraycopy(internaBuffer, startPos, readStr, stringLen, len);
                stringLen += len;
                startPos = 0;

                fillBuffer();
                if (isEof())
                    return false;
            }
            byte ch = internaBuffer[pos++];
            if (ch == '\n') {
                int len = pos - startPos - 1;
                System.arraycopy(internaBuffer, startPos, readStr, stringLen, len);
                stringLen += len;
                return true;
            }
        }
    }

    private void fillBuffer() throws IOException {
        readLen = input.read(internaBuffer, 0, SIZE_INTERNAL_BUFFER);
        pos = 0;
    }

    @Override
    public String toString() {
        return new String(readStr, 0, stringLen);
    }

    public byte[] getBytes() {
        byte[] result = Arrays.copyOfRange(readStr, 0, stringLen);
        return result;
    }
/*
    public static Stream<byte[]> fromFile(InputStream inputStream) throws IOException {
        StreamByteReader bufferStream = new StreamByteReader(inputStream);

        return
        IntStream.iterate(SIZE_INTERNAL_BUFFER, i->i+1)
                .takeWhile(i-> !bufferStream.isEof())
                .mapToObj(i->{
                    try {
                        if(bufferStream.readRow())
                            return bufferStream.getBytes();
                    } catch (IOException e) {

                    }
                    return new byte[0];
        });
    }*/

    private boolean isEof() {
        return readLen == -1;
    }
}
