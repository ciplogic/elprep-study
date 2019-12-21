package compact.reader;

import commons.Utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class InputStreamRowReader {
    byte[] b = new byte[100_000];

    public byte[] getCurrentRow() {
        return currentRow;
    }

    byte[] currentRow;
    FileInputStream fileInputStream;
    int startIndex = 0;

    public InputStreamRowReader(FileInputStream inputStream) throws IOException {
        fileInputStream = inputStream;
        readBuf(-1);
    }

    public boolean advance() throws IOException {
        var nextIndex = Utilities.indexOfByte(b, '\n', startIndex);
        if (nextIndex != -1) {
            currentRow = Arrays.copyOfRange(b, startIndex, nextIndex);
            startIndex = nextIndex + 1;
            return true;
        }
        if (!readBuf(startIndex))
            return false;
        startIndex = 0;
        return advance();
    }

    private boolean readBuf(int startIndex) throws IOException {
        byte[] remainder = startIndex != -1 ? Arrays.copyOfRange(b, startIndex, b.length) : new byte[0];
        var readLen = fileInputStream.read(b, 0, b.length - remainder.length);
        if (readLen != -1) {
            b = Utilities.combine(remainder, Arrays.copyOfRange(b, 0, readLen));
            return true;
        }
        return false;
    }

    public static boolean addRowToBatchWrapper(BatchWrapperReader batchWrapper, byte[] row, ArrayList<String> headers) {
        if (0 != row.length && row[0] != '@') {
            return batchWrapper.processBatched(row);
        }
        if (0 != row.length)
        {
            headers.add(new String(row));
        }
        return false;
    }

    public void close() throws IOException {
        fileInputStream.close();
    }
}
