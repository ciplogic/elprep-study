/*
	Copyright (c) 2018 by imec vzw, Leuven, Belgium. All rights reserverd.
*/

package optimized;


import optimized.utils.Slice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CigarOperation {
    public static final String CigarOperations = "MmIiDdNnSsHhPpXx=";

    private static Map<Character, Character> cigarOperationsTable = new HashMap<>();
    private static Map<Slice, List<CigarOperation>> cigarListCache = new ConcurrentHashMap<>();

    static {
        for (int i = 0; i < CigarOperations.length(); ++i) {
            char c = CigarOperations.charAt(i);
            cigarOperationsTable.put(c, Character.toUpperCase(c));
        }
    }

    static {
        cigarListCache.put(new Slice("*".getBytes()), new ArrayList<>());
    }

    public final int length;
    public final char operation;

    public CigarOperation(int length, char operation) {
        this.length = length;
        this.operation = operation;
    }

    public static boolean isDigit(char c) {
        return (('0' <= c) && (c <= '9'));
    }

    private static List<CigarOperation> slowScanCigarString(Slice cigar) {
        ArrayList<CigarOperation> list = new ArrayList<CigarOperation>();
        cigarScanner scanner = new cigarScanner(cigar);
        while (scanner.index < cigar.length()) {
            list.add(scanner.makeCigarOperation());
        }
        List<CigarOperation> old = cigarListCache.putIfAbsent(cigar, list);
        if (old == null) {
            return list;
        } else {
            return old;
        }
    }

    public static List<CigarOperation> scanCigarString(Slice cigar) {
        List<CigarOperation> list = cigarListCache.get(cigar);
        if (list == null) {
            return slowScanCigarString(cigar);
        } else {
            return list;
        }
    }

    static class cigarScanner {
        final Slice cigar;
        int index = 0;

        cigarScanner(Slice cigar) {
            this.cigar = cigar;
        }

        CigarOperation makeCigarOperation() {
            for (int j = index; ; ++j) {
                char c = cigar.charAt(j);
                if (!isDigit(c)) {
                    int length = Integer.parseUnsignedInt(cigar.toString().substring(index, j), 10);
                    char operation = cigarOperationsTable.get(c);
                    index = j + 1;
                    return new CigarOperation(length, operation);
                }
            }
        }
    }
}
