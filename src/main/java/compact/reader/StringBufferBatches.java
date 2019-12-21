package compact.reader;

import compact.SamBatch;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class StringBufferBatches {
    private final int _batchSize;
    private ArrayList<ArrayList<byte[]>> _bufferBatches;
    private int _batchesCount;
    private ArrayList<byte[]> _current;
    private int _size;

    public ArrayList<ArrayList<byte[]>> getLines() {
        return _bufferBatches;
    }

    public void clear(){
        _size = 0;
        _current = _bufferBatches.get(_size);
        _current.clear();
    }


    StringBufferBatches(int batchSize, int batchesCount) {
        _batchSize = batchSize;
        _batchesCount = batchesCount;
        _bufferBatches = new ArrayList<>(_batchesCount);
        IntStream.range(0, _batchesCount).forEach(i -> _bufferBatches.add(new ArrayList<>()));
        _current = _bufferBatches.get(0);
    }

    boolean readRow(byte[] row) {
        _current.add(row);
        boolean result = false;
        if (_current.size() == _batchSize) {
            _size++;
            if (_size == _batchesCount) {

                result = true;
            }
            _current = _bufferBatches.get(_size);
            _current.clear();;
        }
        return result;
    }
}
