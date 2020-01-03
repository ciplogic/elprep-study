package compact;

import commons.Utilities;
import compact.reader.BatchWrapperReader;
import compact.writer.BatchWrapperWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import static commons.Utilities.combine;
import static commons.Utilities.timedRun;

public class elprep {
    public static void main(String[] args) throws IOException {
        var batchSize = 20000;
        var batchWrapper = new BatchWrapperReader(batchSize, Runtime.getRuntime().availableProcessors() * 2);
        var headers = new ArrayList<String>();
        ArrayList<SamBatch> batches = new ArrayList<>();
        timedRun(true, "Read file stream.", () -> {
            var inputFileName = args[1];
            try (FileInputStream fileInputStream = new FileInputStream(inputFileName)) {
                byte[] b = new byte[1_000_000];
                byte[] remainder = new byte[0];
                int readLen;
                while ((readLen = fileInputStream.read(b)) > 0) {
                    if (readLen != b.length) {
                        b = Arrays.copyOfRange(b, 0, readLen);
                    }
                    var startIndex = Utilities.indexOfByte(b, '\n', 0);
                    var row = startIndex != -1 ? combine(remainder, Arrays.copyOfRange(b, 0, startIndex))
                            : remainder;
                    addRowToBatchWrapper(batchWrapper, row, headers, batches);
                    var endIndex = -1;
                    startIndex++;
                    while ((endIndex = Utilities.indexOfByte(b, '\n', startIndex)) > 0) {
                        row = Arrays.copyOfRange(b, startIndex, endIndex);
                        addRowToBatchWrapper(batchWrapper, row, headers, batches);
                        startIndex = endIndex + 1;
                    }
                    remainder = Arrays.copyOfRange(b, startIndex, b.length);
                }
                addRowToBatchWrapper(batchWrapper, remainder, headers, batches);
            }
            batchWrapper.flush(batches);
        });
        var outputFile = args[2];

        timedRun(true, "Write to file.", () -> {
            writeToDisk(outputFile, headers, batches);
        });
    }

    public static void writeToDisk(String outputFile, ArrayList<String> headers, ArrayList<SamBatch> batches) throws IOException {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            var writer = new BatchWrapperWriter(outputStream);
            for (var it : headers) {
                writer.write(it.getBytes());
            }

            writeBatchesParallel(writer, batches);

            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        outputStream.close();
    }

    private static void writeBatchesParallel(BatchWrapperWriter writer, ArrayList<SamBatch> batches) {
        IntStream.range(0, batches.size())
            .parallel()
            .mapToObj(i -> {
                try {
                    var batch = batches.get(i);
                    batches.set(i, null);
                    var outputStream = new ByteArrayOutputStream(100 * batch.size());
                    var localWriter = new BatchWrapperWriter(outputStream);
                    batch.writeToWriter(localWriter);
                    localWriter.close();
                    var result = outputStream.toByteArray();
                    outputStream.close();
                    return result;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            })
            .forEachOrdered(bytes -> {
                try {
                    writer.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }

    private static void addRowToBatchWrapper(BatchWrapperReader batchWrapper, byte[] row, ArrayList<String> headers,
                                             ArrayList<SamBatch> batches) {
        if (0 == row.length || row[0] == '@') {
            if (0 != row.length)
                headers.add(new String(row));
            return;
        }
        if(batchWrapper.processBatched(row)){
            batchWrapper.flush(batches);
        }
    }
}
