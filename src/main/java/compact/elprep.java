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
    public static void main(String[] args) {
        var batchSize = 20000;
        var batchWrapper = new BatchWrapperReader(batchSize, Runtime.getRuntime().availableProcessors()*4);
        var headers = new ArrayList<String>();
        timedRun(true, "Read file stream.", () -> {
            var inputFileName = args[1];
            try(FileInputStream fileInputStream = new FileInputStream(inputFileName)){
                byte[] b;;
                byte[] remainder = new byte[0];
                while ((b = fileInputStream.readNBytes(1250000)).length>0)
                {
                    var startIndex = Utilities.indexOfByte(b, '\n', 0);
                    var row =startIndex!=-1? combine(remainder, Arrays.copyOfRange(b, 0, startIndex))
                            :remainder;
                    addRowToBatchWrapper(batchWrapper, row, headers);
                    var endIndex = -1;
                    startIndex++;
                    while ((endIndex = Utilities.indexOfByte(b, '\n', startIndex))>0)
                    {
                        row = Arrays.copyOfRange(b, startIndex, endIndex);
                        addRowToBatchWrapper(batchWrapper, row, headers);
                        startIndex = endIndex+1;
                    }
                    remainder = Arrays.copyOfRange(b, startIndex, b.length);
                }
                addRowToBatchWrapper(batchWrapper, remainder, headers);
            }
            batchWrapper.flushBatched();
        });
        var outputFile = args[2];

        timedRun(true, "Write file stream.", () -> {
            writeToDisk(outputFile, batchWrapper, headers);
        });
        return;

    }

    private static void writeToDisk(String outputFile, BatchWrapperReader batchWrapper, ArrayList<String> headers)  {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            var writer = new BatchWrapperWriter(outputStream);
            for(var it : headers)
            {
                writer.write(it.getBytes());
            }

            //writeBatches(writer, batchWrapper);
            writeBatchesParallel(writer, batchWrapper);

            writer.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private static void writeBatches(BatchWrapperWriter writer, BatchWrapperReader batchWrapper) throws IOException {
        ArrayList<SamBatch> batches = batchWrapper.batches;
        for (int i = 0, batchesSize = batches.size(); i < batchesSize; i++) {
            SamBatch batch = batches.get(i);
            batch.writeToWriter(writer);
        }
    }

    private static void writeBatchesParallel(BatchWrapperWriter writer, BatchWrapperReader batchWrapper) throws IOException {
        ArrayList<SamBatch> batches = batchWrapper.batches;

        IntStream.range(0, batches.size())
                .parallel()
                .mapToObj(i->{
                    try {
                        var batch = batches.get(i);
                        batches.set(i, null);
                        var outputStream = new ByteArrayOutputStream(400*batch.size());
                        BatchWrapperWriter localWriter = new BatchWrapperWriter(outputStream);
                        batch.writeToWriter(localWriter);
                        outputStream.close();
                        return outputStream.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .forEachOrdered(bytes->{
                    try {
                        writer.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void addRowToBatchWrapper(BatchWrapperReader batchWrapper, byte[] row, ArrayList<String> headers) {
        if(0 ==row.length || row[0]== '@') {
            if (0 != row.length)
                headers.add(new String(row));
            return;
        }
        batchWrapper.processBatched(row);
    }
}
