package compact;

import gnu.trove.TIntArrayList;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.chars.CharArrayList;

import java.util.ArrayList;

public class SamBatch {
    private int _expectedLength;
    private int _size;

    ArrayList<byte[]> QNAME;
    CharArrayList FLAG;
    DeduplicatedDictionary RNAME;
    TIntArrayList POS;
    ByteArrayList MAPQ;
    DeduplicatedDictionary CIGAR;
    DeduplicatedDictionary RNEXT;
    TIntArrayList PNEXT;
    TIntArrayList TLEN;

    //ArrayList<byte[]> SEQ;
    DnaEncodingSequences SeqPacked;
    ArrayList<byte[]> QUAL;


    public SamBatch(int expectedLength) {
        _expectedLength = expectedLength;
        QNAME = new ArrayList<>();
        FLAG = new CharArrayList(expectedLength);
        RNAME = new DeduplicatedDictionary();
        POS = new TIntArrayList(expectedLength);
        MAPQ = new ByteArrayList(expectedLength);

        CIGAR = new DeduplicatedDictionary();
        RNEXT = new DeduplicatedDictionary();
        PNEXT = new TIntArrayList(expectedLength);
        TLEN = new TIntArrayList(expectedLength);
        //SEQ = new ArrayList<>(expectedLength);
        SeqPacked = new DnaEncodingSequences(expectedLength);
        QUAL = new ArrayList<>(expectedLength);
    }

    void readRow(StringScanner sc) {
        QNAME.add(sc.doSlice().getBytes());
        FLAG.add((char) sc.doInt());
        RNAME.add(sc.doSlice());

        POS.add(sc.doInt());
        MAPQ.add ((byte) sc.doInt());
        CIGAR.add(sc.doSlice());
        RNEXT.add(sc.doSlice());
        PNEXT.add(sc.doInt());
        TLEN.add(sc.doInt());
        var seqText = sc.doSlice();
        SeqPacked.Add(seqText);
//        SEQ.add(seqText.getBytes());
        QUAL.add(sc.doSlice().getBytes());
        _size++;
    }

    void shrink(){
        SeqPacked.shrink();
    }
}