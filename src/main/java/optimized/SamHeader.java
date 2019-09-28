/*
	Copyright (c) 2018 by imec vzw, Leuven, Belgium. All rights reserverd.
*/

package optimized;


import optimized.utils.Slice;
import optimized.utils.StringScanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class SamHeader {

    public static final Slice SamFileFormatVersion = new Slice("1.5");
    public static final Slice SamFileFormatDate = new Slice("1 Jun 2017");
    public static final Slice LN = new Slice("LN");
    public static final Slice VN = new Slice("VN");
    public static final Slice SO = new Slice("SO");
    public static final Slice GO = new Slice("GO");
    public static final Slice coordinate = new Slice("coordinate");
    public static final Slice queryname = new Slice("queryname");
    public static final Slice unsorted = new Slice("unsorted");
    public static final Slice unknown = new Slice("unknown");
    public static final Slice keep = new Slice("keep");
    public static final Slice none = new Slice("none");
    public static final Slice query = new Slice("query");
    public static final Slice reference = new Slice("reference");
    private static Slice atHD = new Slice("@HD");
    private static Slice atSQ = new Slice("@SQ");
    private static Slice atRG = new Slice("@RG");
    private static Slice atPG = new Slice("@PG");
    private static Slice atCO = new Slice("@CO");
    public Map<Slice, Slice> HD = null;
    public List<Map<Slice, Slice>> SQ = new ArrayList<>(32);
    public List<Map<Slice, Slice>> RG = new ArrayList<>();
    public List<Map<Slice, Slice>> PG = new ArrayList<>(2);
    public List<Slice> CO = new ArrayList<>();
    public Map<Slice, List<Map<Slice, Slice>>> userRecords = new HashMap<>();

    public SamHeader(BufferedReader reader) {
        try {
            StringScanner sc = new StringScanner();
            for (boolean first = true; ; first = false) {
                reader.mark(2);
                int peek = reader.read();
                reader.reset();
                if ((peek < 0) || (peek != '@')) {
                    return;
                } else {
                    String line = reader.readLine();
                    sc.reset(line.getBytes(), 4, line.length() - 4);
                    if (line.startsWith("@HD\t")) {
                        assert first : "@HD line not in first line when parsing a SAM header.";
                        HD = sc.parseSamHeaderLine();
                    } else if (line.startsWith("@SQ\t")) {
                        SQ.add(sc.parseSamHeaderLine());
                    } else if (line.startsWith("@RG\t")) {
                        RG.add(sc.parseSamHeaderLine());
                    } else if (line.startsWith("@PG\t")) {
                        PG.add(sc.parseSamHeaderLine());
                    } else if (line.startsWith("@CO\t")) {
                        CO.add(new Slice(line.getBytes(), 4, line.length() - 4));
                    } else if (line.startsWith("@CO")) {
                        CO.add(new Slice(line.getBytes(), 3, line.length() - 3));
                    } else if (isSamHeaderUserTag(line, 1, 2)) {
                        assert line.charAt(0) == '@' : "Header code " + line.substring(0, 3) + " does not start with @ when parsing a SAM header.";
                        assert line.charAt(3) == '\t' : "Header code " + line.substring(0, 3) + " not followed by a tab when parsing a SAM header.";
                        addUserRecord(new Slice(line.getBytes(), 0, 3), sc.parseSamHeaderLine());
                    } else {
                        throw new RuntimeException("Unknown SAM record type code " + line.substring(0, 3));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isSamHeaderUserTag(String code, int pos, int length) {
        int end = pos + length;
        for (int i = 0; i < end; ++i) {
            char c = code.charAt(i);
            if (('a' <= c) && (c <= 'z')) {
                return true;
            }
        }
        return false;
    }

    public static int find(List<Map<Slice, Slice>> l, Predicate<Map<Slice, Slice>> p) {
        int index = -1;
        for (Map<Slice, Slice> e : l) {
            ++index;
            if (p.test(e)) {
                return index;
            }
        }
        return -1;
    }

    public static void skipSamHeader(BufferedReader reader) {
        try {
            while (true) {
                reader.mark(1024);
                String line = reader.readLine();
                if ((line == null) || (line.charAt(0) != '@')) {
                    reader.reset();
                    return;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getSQ_LN(Map<Slice, Slice> record) {
        Slice ln = record.get(LN);
        assert ln != null : "LN entry in a SQ header line missing.";
        return Integer.parseInt(ln.toString(), 10);
    }

    public static int getSQ_LN(Map<Slice, Slice> record, int defaultValue) {
        Slice ln = record.get(LN);
        if (ln == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(ln.toString(), 10);
        }
    }

    public static void setSQ_LN(Map<Slice, Slice> record, int value) {
        record.put(LN, new Slice(Integer.toString(value)));
    }

    public static void formatSamString(PrintWriter out, Slice tag, Slice value) {
        out.print('\t');
        tag.write(out);
        out.print(':');
        value.write(out);
    }

    public static void formatSamHeaderLine(PrintWriter out, String code, Map<Slice, Slice> record) {
        out.print(code);
        record.forEach((key, value) -> formatSamString(out, key, value));
        out.print('\n');
    }

    public static void formatSamHeaderLine(PrintWriter out, Slice code, Map<Slice, Slice> record) {
        code.write(out);
        record.forEach((key, value) -> formatSamString(out, key, value));
        out.print('\n');
    }

    public static void formatSamComment(PrintWriter out, String code, Slice comment) {
        out.print(code);
        out.print('\t');
        comment.write(out);
        out.print('\n');
    }

    public Map<Slice, Slice> ensureHD() {
        if (HD == null) {
            HD = new HashMap<>();
            HD.put(VN, SamFileFormatVersion);
        }
        return HD;
    }

    public Slice getHD_SO() {
        Map<Slice, Slice> hd = ensureHD();
        Slice sortingOrder = hd.get(SO);
        if (sortingOrder == null) {
            return unknown;
        } else {
            return sortingOrder;
        }
    }

    public void setHD_SO(Slice value) {
        Map<Slice, Slice> hd = ensureHD();
        hd.remove(GO);
        hd.put(SO, value);
    }

    public Slice getHD_GO() {
        Map<Slice, Slice> hd = ensureHD();
        Slice groupingOrder = hd.get(GO);
        if (groupingOrder == null) {
            return none;
        } else {
            return groupingOrder;
        }
    }

    public void setHD_GO(Slice value) {
        Map<Slice, Slice> hd = ensureHD();
        hd.remove(SO);
        hd.put(GO, value);
    }

    public void addUserRecord(Slice code, Map<Slice, Slice> record) {
        List<Map<Slice, Slice>> records = userRecords.get(code);
        if (records == null) {
            records = new ArrayList<>();
            userRecords.put(code, records);
        }
        records.add(record);
    }

    public void format(PrintWriter out) {
        if (HD != null) {
            formatSamHeaderLine(out, "@HD", HD);
        }
        for (Map<Slice, Slice> record : SQ) {
            formatSamHeaderLine(out, "@SQ", record);
        }
        for (Map<Slice, Slice> record : RG) {
            formatSamHeaderLine(out, "@RG", record);
        }
        for (Map<Slice, Slice> record : PG) {
            formatSamHeaderLine(out, "@PG", record);
        }
        for (Slice comment : CO) {
            formatSamComment(out, "@CO", comment);
        }
        userRecords.forEach((code, records) -> {
            for (Map<Slice, Slice> record : records) {
                formatSamHeaderLine(out, code, record);
            }
        });
    }
}
