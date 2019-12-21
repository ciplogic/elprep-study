package compact;

import compact.reader.BatchWrapperReader;
import compact.reader.InputStreamRowReader;
import compact.writer.BatchWrapperWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.stream.IntStream;

import static commons.Utilities.timedRun;

public class elprepRowReader {
    public static void main(String[] args) {
        var batchSize = 20000;
        var batchWrapper = new BatchWrapperReader(batchSize, Runtime.getRuntime().availableProcessors() * 2);
        var headers = new ArrayList<String>();
        ArrayList<SamBatch> batches = new ArrayList<>();
        timedRun(true, "Read file stream.", () -> {
            var inputFileName = args[1];
            var inputStream = new FileInputStream(inputFileName);
            var streamRowReader = new InputStreamRowReader(inputStream);
            while (streamRowReader.advance()){
                if(InputStreamRowReader.addRowToBatchWrapper(batchWrapper, streamRowReader.getCurrentRow(), headers))
                {
                    batchWrapper.flush(batches);
                }
            }
            streamRowReader.close();
            batchWrapper.flush(batches);
        });
        var outputFile = args[2];

        timedRun(true, "Write file stream.", () -> {
            writeToDisk(outputFile, headers, batches);
        });
    }

    private static void writeToDisk(String outputFile, ArrayList<String> headers, ArrayList<SamBatch> batches) {
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
    }

    private static void writeBatchesParallel(BatchWrapperWriter writer, ArrayList<SamBatch> batches) {
                IntStream.range(0, batches.size())
                .parallel()
                .mapToObj(i -> {
                    try {
                        var batch = batches.get(i);
                        batches.set(i, null);
                        var outputStream = new ByteArrayOutputStream(400 * batch.size());
                        BatchWrapperWriter localWriter = new BatchWrapperWriter(outputStream);
                        batch.writeToWriter(localWriter);
                        outputStream.close();
                        return outputStream.toByteArray();
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

}
