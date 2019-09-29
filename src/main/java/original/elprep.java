/*
	Copyright (c) 2018 by imec vzw, Leuven, Belgium. All rights reserverd.
*/

package original;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static commons.Utilities.timedRun;
import static original.MarkDuplicates.markDuplicates;
import static original.Pipeline.composeFilters;
import static original.Pipeline.effectiveSortingOrder;
import static original.SimpleFilters.addOrReplaceReadGroup;
import static original.SimpleFilters.replaceReferenceSequenceDictionaryFromSamFile;
import static original.StringScanner.parseSamHeaderLineFromString;

public class elprep {


    public static void runBestPracticesPipelineIntermediateSam
            (InputStream input, OutputStream output, Slice sortingOrder,
             List<Function<SamHeader, Predicate<SamAlignment>>> preFilters,
             List<Function<SamHeader, Predicate<SamAlignment>>> postFilters,
             boolean timed) {
        var filteredReads = new Sam();
        timedRun(timed, "Reading SAM into memory and applying filters.", () -> {
            try (var in = new BufferedReader(new InputStreamReader(input, StandardCharsets.US_ASCII))) {
                filteredReads.header = new SamHeader(in);
                var originalSO = filteredReads.header.getHD_SO();
                var preFilter = composeFilters(filteredReads.header, preFilters);
                var effectiveSO = effectiveSortingOrder(sortingOrder, filteredReads.header, originalSO);
                var inputStream = in.lines();

                if (effectiveSO.equals(SamHeader.keep) ||
                        effectiveSO.equals(SamHeader.unknown)) {
                    // inputStream remains ordered
                } else if (effectiveSO.equals(SamHeader.coordinate) ||
                        effectiveSO.equals(SamHeader.queryname) ||
                        effectiveSO.equals(SamHeader.unsorted)) {
                    inputStream = inputStream.unordered();
                } else {
                    throw new RuntimeException("Unknown sorting order.");
                }

                var alnStream = inputStream.parallel().map((s) -> new SamAlignment(s));

                if (preFilter != null) {
                    alnStream = alnStream.filter(preFilter);
                }

                filteredReads.alignments = alnStream.toArray(SamAlignment[]::new); // we need an intermediate array because output can only commence after mark duplicates has seen /all/ reads

                if (effectiveSO.equals(SamHeader.coordinate)) {
                    Arrays.parallelSort(filteredReads.alignments, new SamAlignment.CoordinateComparator());
                } else if (effectiveSO.equals(SamHeader.queryname)) {
                    Arrays.parallelSort(filteredReads.alignments, (aln1, aln2) -> aln1.QNAME.compareTo(aln2.QNAME));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        timedRun(timed, "Write to file.", () -> {
            try (var out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.US_ASCII)))) {
                var originalSO = filteredReads.header.getHD_SO();
                var postFilter = composeFilters(filteredReads.header, postFilters);
                var effectiveSO = effectiveSortingOrder(sortingOrder.equals(SamHeader.unsorted) ? SamHeader.unsorted : SamHeader.keep, filteredReads.header, originalSO);
                var alnStream = Arrays.stream(filteredReads.alignments).parallel();

                if (postFilter != null) {
                    alnStream = alnStream.filter(postFilter);
                }

                filteredReads.header.format(out);

                var outputStream = alnStream.parallel().map((aln) -> {
                    var sw = new StringWriter();
                    try (var swout = new PrintWriter(sw)) {
                        aln.format(swout);
                    }
                    return sw.toString();
                });

                if (effectiveSO.equals(SamHeader.keep) || effectiveSO.equals(SamHeader.unknown)) {
                    outputStream.forEachOrdered((s) -> out.println(s));
                } else if (effectiveSO.equals(SamHeader.coordinate) || effectiveSO.equals(SamHeader.queryname)) {
                    throw new RuntimeException("Sorting on files not supported.");
                } else if (effectiveSO.equals(SamHeader.unsorted)) {
                    outputStream.forEach((s) -> out.println(s));
                } else {
                    throw new RuntimeException("Unknown sorting order.");
                }
            }
        });
    }

    public static void runBestPracticesPipeline
            (InputStream input, OutputStream output, Slice sortingOrder,
             List<Function<SamHeader, Predicate<SamAlignment>>> filters,
             boolean timed) {
        timedRun(timed, "Running pipeline.", () -> {
            try (var in = new BufferedReader(new InputStreamReader(input, StandardCharsets.US_ASCII));
                 var out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.US_ASCII)))) {
                var header = new SamHeader(in);
                var originalSO = header.getHD_SO();
                var filter = composeFilters(header, filters);
                var effectiveSO = effectiveSortingOrder(sortingOrder, header, originalSO);
                var inputStream = in.lines();

                if (effectiveSO.equals(SamHeader.keep) ||
                        effectiveSO.equals(SamHeader.unknown)) {
                    // inputStream remains ordered
                } else if (effectiveSO.equals(SamHeader.coordinate) ||
                        effectiveSO.equals(SamHeader.queryname) ||
                        effectiveSO.equals(SamHeader.unsorted)) {
                    inputStream = inputStream.unordered();
                } else {
                    throw new RuntimeException("Unknown sorting order.");
                }

                var alnStream = inputStream.parallel().map((s) -> new SamAlignment(s));

                if (filter != null) {
                    alnStream = alnStream.filter(filter);
                }

                if (sortingOrder.equals(SamHeader.coordinate)) {
                    alnStream = alnStream.sorted(new SamAlignment.CoordinateComparator());
                } else if (sortingOrder.equals(SamHeader.queryname)) {
                    alnStream = alnStream.sorted((aln1, aln2) -> aln1.QNAME.compareTo(aln2.QNAME));
                }

                header.format(out);

                var outputStream = alnStream.parallel().map((aln) -> {
                    var sw = new StringWriter();
                    try (var swout = new PrintWriter(sw)) {
                        aln.format(swout);
                    }
                    return sw.toString();
                });

                if (effectiveSO.equals(SamHeader.keep) || effectiveSO.equals(SamHeader.unknown)) {
                    outputStream.forEachOrdered((s) -> out.println(s));
                } else if (effectiveSO.equals(SamHeader.coordinate) || effectiveSO.equals(SamHeader.queryname)) {
                    throw new RuntimeException("Sorting on files not supported.");
                } else if (effectiveSO.equals(SamHeader.unsorted)) {
                    outputStream.forEach((s) -> out.println(s));
                } else {
                    throw new RuntimeException("Unknown sorting order.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void elPrepFilterScript(String[] args) throws IOException {
        Slice sortingOrder = SamHeader.keep;
        boolean timed = true;
        Function<SamHeader, Predicate<SamAlignment>> replaceRefSeqDictFilter = null;
        Function<SamHeader, Predicate<SamAlignment>> removeUnmappedReadsFilter = null;
        Function<SamHeader, Predicate<SamAlignment>> replaceReadGroupFilter = null;
        Function<SamHeader, Predicate<SamAlignment>> markDuplicatesFilter = null;
        Function<SamHeader, Predicate<SamAlignment>> removeDuplicatesFilter = null;
        String input = args[1];
        String output = args[2];
        int argsIndex = 3;
        while (argsIndex < args.length) {
            String entry = args[argsIndex];
            argsIndex++;
            if (entry.equals("--replace-reference-sequences")) {
                String refSeqDict = args[argsIndex];
                argsIndex++;
                replaceRefSeqDictFilter = replaceReferenceSequenceDictionaryFromSamFile(refSeqDict);
            } else if (entry.equals("--filter-unmapped-reads")) {
                removeUnmappedReadsFilter = SimpleFilters::filterUnmappedReads;
            } else if (entry.equals("--filter-unmapped-reads-strict")) {
                removeUnmappedReadsFilter = SimpleFilters::filterUnmappedReadsStrict;
            } else if (entry.equals("--replace-read-group")) {
                String readGroupString = args[argsIndex];
                argsIndex++;
                replaceReadGroupFilter = addOrReplaceReadGroup(parseSamHeaderLineFromString(readGroupString));
            } else if (entry.equals("--mark-duplicates")) {
                markDuplicatesFilter = markDuplicates(false);
            } else if (entry.equals("--mark-duplicates-deterministic")) {
                markDuplicatesFilter = markDuplicates(true);
            } else if (entry.equals("--remove-duplicates")) {
                removeDuplicatesFilter = SimpleFilters::filterDuplicateReads;
            } else if (entry.equals("--sorting-order")) {
                String so = args[argsIndex];
                argsIndex++;
                if (so.equals("keep")) {
                    sortingOrder = SamHeader.keep;
                } else if (so.equals("unknown")) {
                    sortingOrder = SamHeader.unknown;
                } else if (so.equals("unsorted")) {
                    sortingOrder = SamHeader.unsorted;
                } else if (so.equals("queryname")) {
                    sortingOrder = SamHeader.queryname;
                } else if (so.equals("coordinate")) {
                    sortingOrder = SamHeader.coordinate;
                } else {
                    throw new RuntimeException("Unknown sorting order.");
                }
            } else if (entry.equals("--nr-of-threads")) {
                // ignore
                argsIndex++;
            } else if (entry.equals("--timed")) {
                timed = true;
            } else if (entry.equals("--filter-non-exact-mapping-reads") ||
                    entry.equals("--filter-non-exact-mapping-reads-strict") ||
                    entry.equals("--filter-non-overlapping-reads") ||
                    entry.equals("--remove-optional-fields") ||
                    entry.equals("--keep-optional-fields") ||
                    entry.equals("--clean-sam") ||
                    entry.equals("--profile") ||
                    entry.equals("--reference-t") ||
                    entry.equals("--reference-T") ||
                    entry.equals("--rename-chromosomes")) {
                throw new RuntimeException(entry + " not supported");
            } else {
                throw new RuntimeException("unknown command line option");
            }
        }
        List<Function<SamHeader, Predicate<SamAlignment>>> filters = new ArrayList<Function<SamHeader, Predicate<SamAlignment>>>();
        List<Function<SamHeader, Predicate<SamAlignment>>> filters2 = new ArrayList<Function<SamHeader, Predicate<SamAlignment>>>();
        if (removeUnmappedReadsFilter != null) {
            filters.add(removeUnmappedReadsFilter);
        }
        if (replaceRefSeqDictFilter != null) {
            filters.add(replaceRefSeqDictFilter);
        }
        if (replaceReadGroupFilter != null) {
            filters.add(replaceReadGroupFilter);
        }
        if ((replaceRefSeqDictFilter != null) || (markDuplicatesFilter != null) ||
                (sortingOrder.equals("coordinate")) || (sortingOrder.equals("queryname"))) {
            filters.add(SimpleFilters::addREFID);
        }
        if (markDuplicatesFilter != null) {
            filters.add(markDuplicatesFilter);
        }
        filters.add(SimpleFilters::filterOptionalReads);
        if (removeDuplicatesFilter != null) {
            filters2.add(removeDuplicatesFilter);
        }

        FileInputStream inputStream = null;
        OutputStream outputStream = System.out;
        File initialFile = new File(input);
        File outputFile = new File(output);
        try {
            inputStream = new FileInputStream(initialFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            outputStream = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (markDuplicatesFilter != null) {
            runBestPracticesPipelineIntermediateSam(inputStream, outputStream, sortingOrder, filters, filters2, timed);
        } else {
            runBestPracticesPipeline(inputStream, outputStream, sortingOrder, filters, timed);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new RuntimeException("Incorrect number of parameters.");
        } else {
            if (args[0].equals("split")) {
                throw new RuntimeException("split command not implemented yet.");
            } else if (args[0].equals("merge")) {
                throw new RuntimeException("merge command not implemented yet.");
            } else if (args[0].equals("filter")) {
                elPrepFilterScript(args);
            } else {
                throw new RuntimeException("unknown elprep command");
            }
        }
    }
}
