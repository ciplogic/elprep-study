package compact;

import compact.reader.StringScanner;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;

public class SamBatch {
    ArrayList<byte[]> QNAME;
    CharArrayList FLAG;
    DeduplicatedDictionary RNAME;
    IntArrayList POS;
    ByteArrayList MAPQ;
    DeduplicatedDictionary CIGAR;
    DeduplicatedDictionary RNEXT;
    IntArrayList PNEXT;
    IntArrayList TLEN;

    //ArrayList<byte[]> SEQ;
    DnaEncodingSequences SeqPacked;
    ArrayList<byte[]> QUAL;


    public SamBatch(int expectedLength) {
        QNAME = new ArrayList<>();
        FLAG = new CharArrayList(expectedLength);
        RNAME = new DeduplicatedDictionary();
        POS = new IntArrayList(expectedLength);
        MAPQ = new ByteArrayList(expectedLength);

        CIGAR = new DeduplicatedDictionary();
        RNEXT = new DeduplicatedDictionary();
        PNEXT = new IntArrayList(expectedLength);
        TLEN = new IntArrayList(expectedLength);
        SeqPacked = new DnaEncodingSequences(expectedLength);
        QUAL = new ArrayList<>(expectedLength);
    }

    public void readRow(StringScanner sc) {
        QNAME.add(sc.doSlice());
        FLAG.add((char) sc.doInt());
        RNAME.add(new String(sc.doSlice()));

        POS.add(sc.doInt());
        MAPQ.add ((byte) sc.doInt());
        CIGAR.add(new String(sc.doSlice()));
        RNEXT.add(new String(sc.doSlice()));
        PNEXT.add(sc.doInt());
        TLEN.add(sc.doInt());
        var seqText = sc.doSlice();
        SeqPacked.add(seqText);
        QUAL.add(sc.doSlice());
    }

    public void shrink(){
        SeqPacked.shrink();
    }
}