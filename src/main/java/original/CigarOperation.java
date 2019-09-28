/*
	Copyright (c) 2018 by imec vzw, Leuven, Belgium. All rights reserverd.
*/

package original;

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
        for (var i = 0; i < CigarOperations.length(); ++i) {
            var c = CigarOperations.charAt(i);
            cigarOperationsTable.put(c, Character.toUpperCase(c));
        }
    }

    static {
        cigarListCache.put(new Slice("*"), new ArrayList<>());
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
        var list = new ArrayList<CigarOperation>();
        var scanner = new cigarScanner(cigar);
        while (scanner.index < cigar.length()) {
            list.add(scanner.makeCigarOperation());
        }
        var old = cigarListCache.putIfAbsent(cigar, list);
        if (old == null) {
            return list;
        } else {
            return old;
        }
    }

    public static List<CigarOperation> scanCigarString(Slice cigar) {
        var list = cigarListCache.get(cigar);
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
            for (var j = index; ; ++j) {
                var c = cigar.charAt(j);
                if (!isDigit(c)) {
                    var length = Integer.parseUnsignedInt(cigar, index, j, 10);
                    var operation = cigarOperationsTable.get(c);
                    index = j + 1;
                    return new CigarOperation(length, operation);
                }
            }
        }
    }
}
