package compact.reader;

import compact.SamBatch;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class BatchWrapperReader {

    public ArrayList<SamBatch> batches = new ArrayList<>();

    private StringBufferBatches stringBufferBatches;
    public BatchWrapperReader(int batchSize, int bufferBatchesCount) {
        stringBufferBatches = new StringBufferBatches(batchSize, bufferBatchesCount, this);
    }

    public void processBatched(byte[] row){
        stringBufferBatches.readRow(row);
    }

    public void flushBatched() {
        stringBufferBatches.flushAllToWrapper();
    }
}
