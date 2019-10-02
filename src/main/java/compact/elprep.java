package compact;

import commons.Utilities;
import compact.reader.BatchWrapperReader;

import java.io.FileInputStream;
import java.util.Arrays;

import static commons.Utilities.combine;
import static commons.Utilities.timedRun;

public class elprep {
    public static void main(String[] args) {
        timedRun(true, "Read file stream.", () -> {
            var inputFileName = args[1];
            var batchSize = 2000;
            var batchWrapper = new BatchWrapperReader(batchSize, Runtime.getRuntime().availableProcessors()*4);
            try(FileInputStream fileInputStream = new FileInputStream(inputFileName)){
                byte[] b;;
                byte[] remainder = new byte[0];
                while ((b = fileInputStream.readNBytes(1250000)).length>0)
                {
                    var startIndex = Utilities.indexOfByte(b, '\n', 0);
                    var row =startIndex!=-1? combine(remainder, Arrays.copyOfRange(b, 0, startIndex))
                            :remainder;
                    addRowToBatchWrapper(batchWrapper, row);
                    var endIndex = -1;
                    startIndex++;
                    while ((endIndex = Utilities.indexOfByte(b, '\n', startIndex))>0)
                    {
                        row = Arrays.copyOfRange(b, startIndex, endIndex);
                        addRowToBatchWrapper(batchWrapper, row);
                        startIndex = endIndex+1;
                    }
                    remainder = Arrays.copyOfRange(b, startIndex, b.length);
                }
                addRowToBatchWrapper(batchWrapper, remainder);
            }
            batchWrapper.flushBatched();
        });
    }

    private static void addRowToBatchWrapper(BatchWrapperReader batchWrapper, byte[] row) {
        if(0 ==row.length || row[0]== '@')
            return;
        batchWrapper.processBatched(row);
    }
}
