package compact.reader;

import compact.SamBatch;

import java.util.ArrayList;
import java.util.stream.IntStream;

class StringBufferBatches
{
    private ArrayList<ArrayList<byte[]>> _bufferBatches;
    private final int _batchSize;
    private int _batchesCount;
    private final BatchWrapperReader _wrapperReader;
    private ArrayList<byte[]> _current;
    private int _size;

    StringBufferBatches(int batchSize, int batchesCount, BatchWrapperReader wrapperReader){
        _batchSize = batchSize;
        _batchesCount = batchesCount;
        _wrapperReader = wrapperReader;
        _bufferBatches = new ArrayList<>(_batchesCount);
        IntStream.range(0, _batchesCount).forEach(i->_bufferBatches.add(new ArrayList<>()));
        _current = _bufferBatches.get(0);
    }

    void readRow(byte[] row){
        _current.add(row);
        if(_current.size()==_batchSize){
            _size++;
            if (_size==_batchesCount){
                flushAllToWrapper();
            }
            _current = _bufferBatches.get(_size);
        }
    }

    void flushAllToWrapper() {
        IntStream.range(0, _size).parallel().mapToObj(i -> {
            var rows = _bufferBatches.get(i);
            return flushBatch(rows);
        }).forEachOrdered(
                batch->{
                    _wrapperReader.batches.add(batch);
                });
        _size = 0;
    }

    private SamBatch flushBatch(ArrayList<byte[]> rows){
        var result = new SamBatch(_batchSize);
        StringScanner sc = new StringScanner();
        for(var row: rows){
            sc.setText(row);
            result.readRow(sc);
        }
        rows.clear();
        result.shrink();
        return result;
    }
}

public class BatchWrapperReader {

    ArrayList<SamBatch> batches = new ArrayList<>();

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
