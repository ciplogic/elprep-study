package geoiplocationreader;

import java.io.*;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

class LogInstance{
    Stream<String> rowReader;
    void runDiagnostics(long start){

        Runtime rt = Runtime.getRuntime();
        var end = System.currentTimeMillis();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println("Ramp-up milliseconds: "+(end-start));
        System.out.println("Used MB: "+usedMB);
    }
}
public class CompactGeolocationImplementation {

    public static void main(String[] args) throws IOException {

        var fileName = args[0];
        String algo =
//            "default"
//                "compact"
 "compactOpt"
                ;

        switch (algo) {
            case "default": {
                runAlgo(fileName, CompactGeolocationImplementation::defaultReader);
                break;
            }
            case "compact": {
                runAlgo(fileName, CompactGeolocationImplementation::compactReader);
                break;
            }
            case "compactOpt": {
                runAlgo(fileName, CompactGeolocationImplementation::compactOptReader);
                break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + algo);
        }
        System.in.read();
    }
    static void defaultReader(LogInstance logInstance)  {
        var start = System.currentTimeMillis();
        Stream<String> rowReader = logInstance.rowReader;

        var locations = new ArrayList<LocationRow>();
        var reader = new CsvLocationReader();
        rowReader.forEach(row->{
            var rowsCleaned = reader.splitByCommasNoQuotes(row);
            LocationRow location = new LocationRow(rowsCleaned);
            locations.add(location);
        });
        locations.trimToSize();
        logInstance.runDiagnostics(start);
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static void compactReader(LogInstance logInstance){
        var start = System.currentTimeMillis();
        Stream<String> rowReader = logInstance.rowReader;
        var locations = new CompactLocationsRepo();
        var reader = new CsvLocationReader();
        rowReader.forEach(row->{
            var rowsCleaned = reader.splitByCommasNoQuotes(row);
            locations.addLocation(rowsCleaned);
        });
        locations.trim();
        logInstance.runDiagnostics(start);
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    static void compactOptReader(LogInstance logInstance){
        var start = System.currentTimeMillis();
        Stream<String> rowReader = logInstance.rowReader;
        var locations = new CompactLocationOptimizedRepo();
        var reader = new CsvLocationReader();
        rowReader.forEach(row->{
            var rowsCleaned = reader.splitByCommasNoQuotes(row);
            locations.addLocation(rowsCleaned);
        });
        locations.trim();
        logInstance.runDiagnostics(start);
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static void runAlgo(String fileName, Consumer<LogInstance> action) throws IOException {

        var file = new File(fileName);
        var input = new FileInputStream(file);
        try (var in = new BufferedReader(new InputStreamReader(input))) {
            var lines = in.lines();
            var logInstance = new LogInstance();
            logInstance.rowReader = lines;
            action.accept(logInstance);
        } catch (IOException e) {
        }
    }

}
