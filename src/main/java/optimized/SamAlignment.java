/*
	Copyright (c) 2018 by imec vzw, Leuven, Belgium. All rights reserverd.
*/

package optimized;


import optimized.utils.Slice;
import optimized.utils.StreamByteWriter;
import optimized.utils.StringScanner;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static optimized.Fields.*;


public class SamAlignment {
    public static final Slice RG = new Slice("RG");
    public static final Slice REFID = new Slice("REFID");
    public static final Slice LIBID = new Slice("LIBID");
    public static final int Multiple = 0x1;
    public static final int Proper = 0x2;
    public static final int Unmapped = 0x4;
    public static final int NextUnmapped = 0x8;
    public static final int Reversed = 0x10;
    public static final int NextReversed = 0x20;
    public static final int First = 0x40;
    public static final int Last = 0x80;
    public static final int Secondary = 0x100;
    public static final int QCFailed = 0x200;
    public static final int Duplicate = 0x400;
    public static final int Supplementary = 0x800;
    private static final Slice pos = new Slice("pos");
    private static final Slice score = new Slice("score");
    public static short[] phredScoreTable = new short[512];
    public static int[] clippedTable = new int[256];
    public static int[] referenceTable = new int[256];
    static byte[] tab = {'\t'};

    static {
        for (int c = 0; c < 256; ++c) {
            int pos = c << 1;
            if ((c < 33) || (c > 126)) {
                phredScoreTable[pos] = 0;
                phredScoreTable[pos + 1] = 1;
            } else {
                int qual = c - 33;
                if (qual >= 15) {
                    phredScoreTable[pos] = (short) qual;
                } else {
                    phredScoreTable[pos] = 0;
                }
                phredScoreTable[pos + 1] = 0;
            }
        }
    }

    static {
        for (int i = 0; i < 256; ++i) {
            clippedTable[i] = 0;
            referenceTable[i] = 0;
        }
        clippedTable['S'] = 1;
        clippedTable['H'] = 1;
        referenceTable['M'] = 1;
        referenceTable['D'] = 1;
        referenceTable['N'] = 1;
        referenceTable['='] = 1;
        referenceTable['X'] = 1;
    }

    public Slice QNAME;
    public Slice RNAME;
    public Slice CIGAR;
    public Slice RNEXT;
    public Slice SEQ;
    public Slice QUAL;
    public int PNEXT;
    public int TLEN;
    public int POS;
    public char FLAG;
    public byte MAPQ;
    public ArrayList<Field> TAGS = new ArrayList<>(16);
    public List<Field> temps = new ArrayList<>(4);

    public SamAlignment(byte[] sBytes) {
        StringScanner sc = new StringScanner(sBytes);

        QNAME = sc.doSlice();
        FLAG = (char) sc.doInt();
        RNAME = sc.doSlice();
        POS = sc.doInt();
        MAPQ = (byte) sc.doInt();
        CIGAR = sc.doSlice();
        RNEXT = sc.doSlice();
        PNEXT = sc.doInt();
        TLEN = sc.doInt();
        SEQ = sc.doSlice();
        QUAL = sc.doSlicen();

        while (sc.length() > 0) {
            TAGS.add(sc.parseSamAlignmentField());
        }
    }

    public Slice getRG() {
        return ((StringField) assoc(TAGS, RG)).value;
    }

    public void setRG(Slice rg) {
        Field f = assoc(TAGS, RG);
        if (f == null) {
            TAGS.add(new StringField(RG, rg));
        } else {
            ((StringField) f).value = rg;
        }
    }

    public int getREFID() {
        return ((IntegerField) assoc(temps, REFID)).value;
    }

    public void setREFID(int refid) {
        Field f = assoc(temps, REFID);
        if (f == null) {
            temps.add(new IntegerField(REFID, refid));
        } else {
            ((IntegerField) f).value = refid;
        }
    }

    public Slice getLIBID() {
        return ((StringField) assoc(temps, LIBID)).value;
    }

    public void setLIBID(Slice libid) {
        Field f = assoc(temps, LIBID);
        if (f == null) {
            temps.add(new StringField(LIBID, libid));
        } else {
            ((StringField) f).value = libid;
        }
    }

    public boolean isMultiple() {
        return (FLAG & Multiple) != 0;
    }

    public boolean isProper() {
        return (FLAG & Proper) != 0;
    }

    public boolean isUnmapped() {
        return (FLAG & Unmapped) != 0;
    }

    public boolean isNextUnmapped() {
        return (FLAG & NextUnmapped) != 0;
    }

    public boolean isReversed() {
        return (FLAG & Reversed) != 0;
    }

    public boolean isNextReversed() {
        return (FLAG & NextReversed) != 0;
    }

    public boolean isFirst() {
        return (FLAG & First) != 0;
    }

    public boolean isLast() {
        return (FLAG & Last) != 0;
    }

    public boolean isSecondary() {
        return (FLAG & Secondary) != 0;
    }

    public boolean isQCFailed() {
        return (FLAG & QCFailed) != 0;
    }

    public boolean isDuplicate() {
        return (FLAG & Duplicate) != 0;
    }

    public boolean isSupplementary() {
        return (FLAG & Supplementary) != 0;
    }

    public boolean flagEvery(int flag) {
        return (FLAG & flag) == flag;
    }

    public boolean flagSome(int flag) {
        return (FLAG & flag) != 0;
    }

    public boolean flagNotEvery(int flag) {
        return (FLAG & flag) != flag;
    }

    public boolean flagNotAny(int flag) {
        return (FLAG & flag) == 0;
    }


    public void formatBuffer(StreamByteWriter out) throws IOException {
        QNAME.writeBuffer(out);
        writeTab(out);

        out.printInt(FLAG);
        writeTab(out);
        RNAME.writeBuffer(out);
        writeTab(out);
        out.printInt(POS);
        writeTab(out);
        out.printInt(MAPQ);
        writeTab(out);
        CIGAR.writeBuffer(out);
        writeTab(out);
        RNEXT.writeBuffer(out);
        writeTab(out);
        out.printInt(PNEXT);
        writeTab(out);
        out.printInt(TLEN);
        writeTab(out);
        SEQ.writeBuffer(out);
        writeTab(out);
        QUAL.writeBuffer(out);

        for (Field f : TAGS) {
            f.format(out);
        }
        out.writeByte((byte) '\n');
    }

    private void writeTab(StreamByteWriter out) throws IOException {
        out.writeByte((byte) '\t');
    }

    public int computePhredScore() {
        int score = 0;
        int error = 0;
        for (int i = 0; i < QUAL.length; ++i) {
            char c = QUAL.charAt(i);
            int pos = c << 1;
            score += phredScoreTable[pos];
            error |= phredScoreTable[pos + 1];
        }
        assert error != 0 : "Invalid QUAL character in " + QUAL + ".";
        return score;
    }

    public int computeUnclippedPosition() {
        List<CigarOperation> cigar = CigarOperation.scanCigarString(CIGAR);
        if (cigar.isEmpty()) {
            return POS;
        }

        if (isReversed()) {
            int clipped = 1;
            int result = POS - 1;
            for (int i = cigar.size() - 1; i >= 0; --i) {
                CigarOperation op = cigar.get(i);
                char p = op.operation;
                int c = clippedTable[p];
                int r = referenceTable[p];
                clipped *= c;
                result += (r | clipped) * op.length;
            }
            return result;
        } else {
            int result = POS;
            for (CigarOperation op : cigar) {
                char p = op.operation;
                if (clippedTable[p] == 0) {
                    break;
                }
                result -= op.length;
            }
            return result;
        }
    }

    public int getAdaptedPos() {
        return ((IntegerField) assoc(temps, pos)).value;
    }

    public void setAdaptedPos(int p) {
        Field f = assoc(temps, pos);
        if (f == null) {
            temps.add(new IntegerField(pos, p));
        } else {
            ((IntegerField) f).value = p;
        }
    }

    public int getAdaptedScore() {
        return ((IntegerField) assoc(temps, score)).value;
    }

    public void setAdaptedScore(int s) {
        Field f = assoc(temps, score);
        if (f == null) {
            temps.add(new IntegerField(score, s));
        } else {
            ((IntegerField) f).value = s;
        }
    }

    public void adaptAlignment(Map<Slice, Slice> lbTable) {
        Slice rg = getRG();
        if (rg != null) {
            rg = rg.intern();
            setRG(rg);
            Slice lb = lbTable.get(rg);
            if (lb != null) {
                setLIBID(lb);
            }
        }
        setAdaptedPos(computeUnclippedPosition());
        setAdaptedScore(computePhredScore());
    }

    public boolean isTrueFragment() {
        return (FLAG & (Multiple | NextUnmapped)) != Multiple;
    }

    public boolean isTruePair() {
        return (FLAG & (Multiple | NextUnmapped)) == Multiple;
    }

    public static class CoordinateComparator implements Comparator<SamAlignment> {
        public int compare(SamAlignment aln1, SamAlignment aln2) {
            int refid1 = aln1.getREFID();
            int refid2 = aln2.getREFID();
            if (refid1 < refid2) {
                if (refid1 >= 0) {
                    return -1;
                } else {
                    return +1;
                }
            } else if (refid2 < refid1) {
                if (refid2 < 0) {
                    return -1;
                } else {
                    return +1;
                }
            } else {
                return aln1.POS - aln2.POS;
            }
        }
    }
}
