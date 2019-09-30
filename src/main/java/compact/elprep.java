package compact;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static commons.Utilities.timedRun;

class BatchWrapper{
    
    ArrayList<SamBatch> batches = new ArrayList<>();
    int counter;
    private int batchSize;
    SamBatch batch ;

    StringScanner sc = new StringScanner();
    BatchWrapper(int batchSize) {
        this.batchSize = batchSize;
        batch = new SamBatch(batchSize);
    }

    public void processRow(String row) {
        counter++;
        if(counter==batchSize){
            batch.shrink();
            batches.add(batch);
            batch = new SamBatch(batchSize);
            counter = 0;
            if(batches.size()% 100 == 0){
                System.out.println("Batch count: "+batches.size());
            }
        }
        sc.SetText(row);
        batch.readRow(sc);
    }
}

public class elprep {
    public static void main(String[] args) {
        timedRun(true, "Read file reactive.", () -> {
            var inputFileName = args[1];
            var batchSize = 10000;
            var batchWrapper = new BatchWrapper(batchSize);
            Files.lines(Paths.get(inputFileName)).forEach(row->{
                if(row.length()==0 || row.charAt(0)== '@')
                    return;
                batchWrapper.processRow(row);
            });
            System.out.println(batchWrapper.batches.size());
//            System.in.read();
//            System.out.println(batchWrapper.batches.size());
        });
    }
}
