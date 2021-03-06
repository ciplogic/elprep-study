import commons.Utilities;

public class StartUp {
    public static void main(String[] args) {
        var algos = new String[]{
                "original",
                "buffers",
                "optimized",
                "compact",
                "compact_par",
        };
        System.out.println("Warmup ...");
        for (var algo : algos)
            runAlgorithmOnTest(args, algo, false, 1);
        System.out.println(" Finished.");
        for (var algo : algos) {
            System.out.println("Algorithm: " + algo + " ...");

            runAlgorithmOnTest(args, algo, true, 10);

            System.out.println("End algorithm: " + algo + " ...");
            System.out.println("=============================");
        }

    }

    private static void runAlgorithmOnTest(String[] args, String algo, boolean timed, int times) {

        Utilities.timedRun(timed, algo, () -> {
            for (int i = 0; i < times; i++) {
                System.out.println("Run: " + (i + 1) + " / " + times);
                switch (algo) {
                    case "original":
                        original.elprep.main(args);
                        break;
                    case "buffers":
                        buffers.elprep.main(args);
                        break;
                    case "optimized":
                        optimized.elprep.main(args);
                        break;
                    case "compact":
                        compact.elprep.main(args);
                        break;
                    case "compact_par":
                        compact.RowReaderBackground.main(args);
                        break;
                }
            }
            System.gc();
        });

    }
}
