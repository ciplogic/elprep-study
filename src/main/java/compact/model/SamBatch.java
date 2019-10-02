package compact.model;

import compact.reader.StringScanner;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class SamBatch {
    CharArrayList FLAG;
    DeduplicatedDictionary RNAME;
    IntArrayList POS;
    ByteArrayList MAPQ;
    StringArrayList QNAME;
    StringArrayList CIGAR;
    DeduplicatedDictionary RNEXT;
    IntArrayList PNEXT;
    IntArrayList TLEN;

    DnaEncodingSequences SeqPacked;
//    StringArrayList SeqPacked;
    StringArrayList QUAL;


    public SamBatch(int expectedLength) {
        QNAME = new StringArrayList(expectedLength);
        FLAG = new CharArrayList(expectedLength);
        RNAME = new DeduplicatedDictionary();
        POS = new IntArrayList(expectedLength);
        MAPQ = new ByteArrayList(expectedLength);

        CIGAR = new StringArrayList(expectedLength);
        RNEXT = new DeduplicatedDictionary(expectedLength);
        PNEXT = new IntArrayList(expectedLength);
        TLEN = new IntArrayList(expectedLength);
        SeqPacked = new DnaEncodingSequences(expectedLength);
//        SeqPacked = new StringArrayList(expectedLength);
        QUAL = new StringArrayList(expectedLength);
    }

    public void readRow(StringScanner sc) {
        QNAME.add(sc.doSlice());
        FLAG.add((char) sc.doInt());
        RNAME.add(sc.doSlice());

        POS.add(sc.doInt());
        MAPQ.add ((byte) sc.doInt());
        CIGAR.add(sc.doSlice());
        RNEXT.add(sc.doSlice());
        PNEXT.add(sc.doInt());
        TLEN.add(sc.doInt());
        var seqText = sc.doSlice();
        SeqPacked.add(seqText);
        QUAL.add(sc.doSlice());
    }

    public void shrink(){
        QNAME.shrink();
        CIGAR.shrink();
        QUAL.shrink();
        SeqPacked.shrink();
    }
}