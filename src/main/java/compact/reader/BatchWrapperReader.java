package compact.reader;

import compact.SamBatch;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class BatchWrapperReader {
    private StringBufferBatches stringBufferBatches;

    public BatchWrapperReader(int batchSize, int bufferBatchesCount) {
        stringBufferBatches = new StringBufferBatches(batchSize, bufferBatchesCount);
    }

    public boolean processBatched(byte[] row) {
        return stringBufferBatches.readRow(row);
    }

    public void flush(ArrayList<SamBatch> batches) {
        BatchWrapperReader.flushAllToWrapper(batches, stringBufferBatches.getLines());
        stringBufferBatches.clear();
    }

    public static void flushAllToWrapper(ArrayList<SamBatch> batches, ArrayList<ArrayList<byte[]>> batchRows) {
        var startIndex = batches.size();
        int _size = batchRows.size();
        IntStream.range(0, _size).forEach(i -> {
            batches.add(null);
        });
        IntStream.range(0, _size)
                .parallel()
                .mapToObj(i -> {
            var rows = batchRows.get(i);
            return new IndexedValue<>(i + startIndex, flushBatch(rows));
        }).forEach(
                indexedValue -> {
                    batches.set(indexedValue.index, indexedValue.batch);
                });
    }

    public static SamBatch flushBatch(ArrayList<byte[]> rows) {
        var result = new SamBatch(rows.size());
        StringScanner sc = new StringScanner();
        for (byte[] row : rows) {
            sc.setText(row);
            result.readRow(sc);
        }
        rows.clear();
        result.shrink();
        return result;
    }

    public void clear() {
        stringBufferBatches.clear();;
    }
}
