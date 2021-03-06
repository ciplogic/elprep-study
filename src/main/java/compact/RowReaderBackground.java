package compact;

import compact.reader.BatchWrapperReader;
import compact.reader.InputStreamRowReader;
import compact.writer.BatchWrapperWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.stream.IntStream;

import static commons.Utilities.timedRun;

class Flusher{
    public BatchWrapperReader front, back;
    ArrayList<String> headers = new ArrayList<>();
    private volatile boolean midFile;

    Flusher(int batchSize){
        front = new BatchWrapperReader(batchSize, Runtime.getRuntime().availableProcessors() * 4);
        back  = new BatchWrapperReader(batchSize, Runtime.getRuntime().availableProcessors() * 4);
    }
    boolean fillFrontReader(InputStreamRowReader streamRowReader, BatchWrapperReader front) throws IOException {
        front.clear();
        while (streamRowReader.advance()) {
            if (InputStreamRowReader.addRowToBatchWrapper(front, streamRowReader.getCurrentRow(), headers))
            {
                return true;
            }
        }
        return false;
    }

    void flush(ArrayList<SamBatch> batches) {
        front.flush(batches);
    }

    boolean flushAndSwap(InputStreamRowReader streamRowReader, ArrayList<SamBatch> batches){
        IntStream.range(0, 2)
                .parallel()
                .forEach(idx->{
                    if(idx==0) {
                        try {
                            this.midFile = fillFrontReader(streamRowReader, back);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        flush(batches);
                    }
                });
        var aux = front;
        front = back;
        back = aux;
        return midFile;
    }
}
public class RowReaderBackground{
        public static void main(String[] args) throws IOException {
            var batchSize = 20000;
            var headers = new ArrayList<String>();
            ArrayList<SamBatch> batches = new ArrayList<>();
            timedRun(true, "Read file stream.", () -> {
                var inputFileName = args[1];
                var flusher = new Flusher(batchSize);
                var inputStream = new FileInputStream(inputFileName);
                var streamRowReader = new InputStreamRowReader(inputStream);
                flusher.fillFrontReader(streamRowReader, flusher.front);
                do {}
                while (flusher.flushAndSwap(streamRowReader, batches));
                streamRowReader.close();
                flusher.front.flush(batches);
            });
            var outputFile = args[2];

            timedRun(true, "Write file stream.", () -> {
                elprep.writeToDisk(outputFile, headers, batches);
            });
        }

}
