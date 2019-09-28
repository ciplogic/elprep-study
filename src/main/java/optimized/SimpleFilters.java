/*
	Copyright (c) 2018 by imec vzw, Leuven, Belgium. All rights reserverd.
*/

package optimized;


import optimized.utils.Slice;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

public class SimpleFilters {

    private static final Slice SN = new Slice("SN");
    private static final Slice star = new Slice("*");
    private static final Slice atsr = new Slice("@sr");
    private static final Slice sr = new Slice("sr");
    private static final Slice ID = new Slice("ID");
    private static final Slice PP = new Slice("PP");
    private static final Slice eq = new Slice("=");

    public static Function<SamHeader, Predicate<SamAlignment>> replaceReferenceSequenceDictionary(List<Map<Slice, Slice>> dict) {
        return (header) -> {
            Slice sortingOrder = header.getHD_SO();
            if (sortingOrder.equals(SamHeader.coordinate)) {
                int previousPos = -1;
                List<Map<Slice, Slice>> oldDict = header.SQ;
                for (Map<Slice, Slice> entry : dict) {
                    Slice sn = entry.get(SN);
                    int pos = SamHeader.find(oldDict, (e) -> sn.equals(e.get(SN)));
                    if (pos >= 0) {
                        if (pos > previousPos) {
                            previousPos = pos;
                        } else {
                            header.setHD_SO(SamHeader.unknown);
                            break;
                        }
                    }
                }
            }
            Set<Slice> dictTable = new HashSet<Slice>();
            for (Map<Slice, Slice> entry : dict) {
                dictTable.add(entry.get(SN));
            }
            header.SQ = dict;
            return (aln) -> dictTable.contains(aln.RNAME);
        };
    }

    public static Function<SamHeader, Predicate<SamAlignment>> replaceReferenceSequenceDictionaryFromSamFile(String samFile) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(samFile), StandardCharsets.US_ASCII))) {
            SamHeader header = new SamHeader(reader);
            return replaceReferenceSequenceDictionary(header.SQ);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Predicate<SamAlignment> filterUnmappedReads(SamHeader header) {
        return (aln) -> (aln.FLAG & SamAlignment.Unmapped) == 0;
    }

    public static Predicate<SamAlignment> filterUnmappedReadsStrict(SamHeader header) {
        return (aln) -> ((aln.FLAG & SamAlignment.Unmapped) == 0) && (aln.POS != 0) && (!aln.RNAME.equals(star));
    }

    public static Predicate<SamAlignment> filterDuplicateReads(SamHeader header) {
        return (aln) -> (aln.FLAG & SamAlignment.Duplicate) == 0;
    }

    public static Predicate<SamAlignment> filterOptionalReads(SamHeader header) {
        if (header.userRecords.get(atsr) == null) {
            return null;
        } else {
            header.userRecords.remove(atsr);
            return (aln) -> Fields.assoc(aln.TAGS, sr) == null;
        }
    }

    public static Function<SamHeader, Predicate<SamAlignment>> addOrReplaceReadGroup(Map<Slice, Slice> readGroup) {
        return (header) -> {
            header.RG = new ArrayList<>();
            header.RG.add(readGroup);
            Slice id = readGroup.get(ID);
            return (aln) -> {
                aln.setRG(id);
                return true;
            };
        };
    }

    public static Function<SamHeader, Predicate<SamAlignment>> addPGLine(Map<Slice, Slice> newPG) {
        return (header) -> {
            String[] id = new String[]{newPG.get(ID).toString()};
            while (SamHeader.find(header.PG, (entry) -> id[0].equals(entry.get(ID))) >= 0) {
                id[0] += Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000));
            }
            newPG.put(ID, new Slice(id[0]));
            for (Map<Slice, Slice> pg : header.PG) {
                Slice nextId = pg.get(ID);
                int pos = SamHeader.find(header.PG, (entry) -> nextId.equals(entry.get(PP)));
                if (pos < 0) {
                    newPG.put(PP, nextId);
                    break;
                }
            }
            header.PG.add(newPG);
            return null;
        };
    }

    public static Predicate<SamAlignment> renameChromosomes(SamHeader header) {
        for (Map<Slice, Slice> entry : header.SQ) {
            Slice sn = entry.get(SN);
            if (sn != null) {
                entry.put(SN, new Slice("chr" + sn.toString()));
            }
        }
        return (aln) -> {
            if ((!aln.RNAME.equals(eq)) && (!aln.RNAME.equals(star))) {
                aln.RNAME = new Slice("chr" + aln.RNAME.toString());
            }
            if ((!aln.RNEXT.equals(eq)) && (!aln.RNEXT.equals(star))) {
                aln.RNEXT = new Slice("chr" + aln.RNEXT.toString());
            }
            return true;
        };
    }

    public static Predicate<SamAlignment> addREFID(SamHeader header) {
        Map<Slice, Integer> dictTable = new HashMap<Slice, Integer>();
        int index = -1;
        for (Map<Slice, Slice> entry : header.SQ) {
            ++index;
            dictTable.put(entry.get(SN), index);
        }
        return (aln) -> {
            Integer value = dictTable.get(aln.RNAME);
            int val = (value == null ? -1 : value);
            aln.setREFID(val);
            return true;
        };
    }
}
