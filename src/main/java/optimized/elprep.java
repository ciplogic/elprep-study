/*
	Copyright (c) 2018 by imec vzw, Leuven, Belgium. All rights reserverd.
*/

package optimized;

import optimized.utils.Slice;
import optimized.utils.StreamByteWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static commons.Utilities.timedRun;
import static optimized.MarkDuplicates.markDuplicates;
import static optimized.Pipeline.composeFilters;
import static optimized.Pipeline.effectiveSortingOrder;
import static optimized.SimpleFilters.addOrReplaceReadGroup;
import static optimized.SimpleFilters.replaceReferenceSequenceDictionaryFromSamFile;
import static optimized.utils.StringScanner.parseSamHeaderLineFromString;

public class elprep {

    public static File initialFile;

    public static void runBestPracticesPipelineIntermediateSam
            (FileInputStream input, OutputStream output, Slice sortingOrder,
             List<Function<SamHeader, Predicate<SamAlignment>>> preFilters,
             List<Function<SamHeader, Predicate<SamAlignment>>> postFilters,
             boolean timed) throws IOException {


        Sam filteredReads = new Sam();
        timedRun(timed, "Reading SAM into memory and applying filters.", () -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(input, StandardCharsets.ISO_8859_1), 4000_000)) {
                filteredReads.header = new SamHeader(in);
                Slice originalSO = filteredReads.header.getHD_SO();
                Predicate<SamAlignment> preFilter = composeFilters(filteredReads.header, preFilters);
                Slice effectiveSO = effectiveSortingOrder(sortingOrder, filteredReads.header, originalSO);
                Stream<String> inputStream = in.lines();

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

                Stream<SamAlignment> alnStream = inputStream
                        .parallel()
                        .map((s) -> (s.getBytes()))
                        .map((s) -> new SamAlignment(s));

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
            BufferedWriter buf = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.ISO_8859_1), 4000_000);
            try (PrintWriter out = new PrintWriter(buf)) {
                Slice originalSO = filteredReads.header.getHD_SO();
                Predicate<SamAlignment> postFilter = composeFilters(filteredReads.header, postFilters);
                Slice effectiveSO = effectiveSortingOrder(sortingOrder.equals(SamHeader.unsorted) ? SamHeader.unsorted : SamHeader.keep, filteredReads.header, originalSO);
                Stream<SamAlignment> alnStream = Arrays.stream(filteredReads.alignments).parallel();

                if (postFilter != null) {
                    alnStream = alnStream.filter(postFilter);
                }

                filteredReads.header.format(out);
                try {
                    buf.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                SamAlignment[] alignments = filteredReads.alignments;
                StreamByteWriter streamByteWriter = new StreamByteWriter(output);
                for (int i = 0, alignmentsLength = alignments.length; i < alignmentsLength; i++) {
                    SamAlignment aln = alignments[i];
                    aln.formatBuffer(streamByteWriter);
                    alignments[i] = null;
                    /*
                    var sw = new StringWriter(2000);
                    try (var swout = new PrintWriter(sw)) {
                        aln.format(swout);
                    }
                    return sw.toString();*/
                }
                /*
                if (effectiveSO.equals(SamHeader.keep) || effectiveSO.equals(SamHeader.unknown)) {
                    outputStream.forEachOrdered((s) -> out.println(s));
                } else if (effectiveSO.equals(SamHeader.coordinate) || effectiveSO.equals(SamHeader.queryname)) {
                    throw new RuntimeException("Sorting on files not supported.");
                } else if (effectiveSO.equals(SamHeader.unsorted)) {
                    outputStream.forEach((s) -> out.println(s));
                } else {
                    throw new RuntimeException("Unknown sorting order.");
                }*/

                streamByteWriter.close();
            }
        });
    }

    public static void runBestPracticesPipeline
            (InputStream input, OutputStream output, Slice sortingOrder,
             List<Function<SamHeader, Predicate<SamAlignment>>> filters,
             boolean timed) {


        timedRun(timed, "Running pipeline.", () -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(input, StandardCharsets.US_ASCII));
                 BufferedWriter bufferVar = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.US_ASCII));
                 PrintWriter out = new PrintWriter(bufferVar)) {
                SamHeader header = new SamHeader(in);
                Slice originalSO = header.getHD_SO();
                Predicate<SamAlignment> filter = composeFilters(header, filters);
                Slice effectiveSO = effectiveSortingOrder(sortingOrder, header, originalSO);
                Stream<String> inputStream = in.lines();

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

                Stream<SamAlignment> alnStream = inputStream.parallel().map((s) -> new SamAlignment(s.getBytes()));

                if (filter != null) {
                    alnStream = alnStream.filter(filter);
                }

                if (sortingOrder.equals(SamHeader.coordinate)) {
                    alnStream = alnStream.sorted(new SamAlignment.CoordinateComparator());
                } else if (sortingOrder.equals(SamHeader.queryname)) {
                    alnStream = alnStream.sorted((aln1, aln2) -> aln1.QNAME.compareTo(aln2.QNAME));
                }

                header.format(out);

                StreamByteWriter outputStr = new StreamByteWriter(output);
                Stream<String> outputStream = alnStream.map((aln) -> {
                    StringWriter sw = new StringWriter();
                    try (PrintWriter swout = new PrintWriter(sw)) {
//                        aln.format(swout);
                    }
                    try {
                        aln.formatBuffer(outputStr);
                    } catch (IOException e) {

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
        initialFile = new File(input);
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
