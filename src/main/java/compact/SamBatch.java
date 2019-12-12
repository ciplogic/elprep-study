package compact;

import compact.reader.StringScanner;
import compact.writer.BatchWrapperWriter;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.IOException;

public class SamBatch {
    StringSequence QNAME;
    CharArrayList FLAG;
    DeduplicatedDictionary RNAME;
    IntArrayList POS;
    ByteArrayList MAPQ;
    DeduplicatedDictionary CIGAR;
    DeduplicatedDictionary RNEXT;
    IntArrayList PNEXT;
    IntArrayList TLEN;

    DnaEncodingSequences SeqPacked;
    StringSequence QUAL;
    TagSequence TAG;


    public SamBatch(int expectedLength) {
        QNAME = new StringSequence();
        FLAG = new CharArrayList(expectedLength);
        RNAME = new DeduplicatedDictionary();
        POS = new IntArrayList(expectedLength);
        MAPQ = new ByteArrayList(expectedLength);

        CIGAR = new DeduplicatedDictionary();
        RNEXT = new DeduplicatedDictionary();
        PNEXT = new IntArrayList(expectedLength);
        TLEN = new IntArrayList(expectedLength);
        SeqPacked = new DnaEncodingSequences(expectedLength);
        QUAL = new StringSequence();
        TAG = new TagSequence();
    }

    public void readRow(StringScanner sc) {
        QNAME.add(sc.doSlice());
        FLAG.add((char) sc.doInt());
        RNAME.add(new String(sc.doSlice()));

        POS.add(sc.doInt());
        MAPQ.add((byte) sc.doInt());
        CIGAR.add(new String(sc.doSlice()));
        RNEXT.add(new String(sc.doSlice()));
        PNEXT.add(sc.doInt());
        TLEN.add(sc.doInt());
        SeqPacked.add(sc.doSlice());
        QUAL.add(sc.doSlice());
        TAG.readRow(sc);
    }

    public void shrink() {
        QNAME.shrink();
        QUAL.shrink();
        SeqPacked.shrink();
        TAG.shrink();

    }

    public void writeToWriter(BatchWrapperWriter sc) throws IOException {
        var len = FLAG.size();
        for (var i = 0; i < len; i++) {
            QNAME.writeToWriter(sc, i);
            sc.writeByte((byte) '\t');
            sc.printInt(FLAG, i);
            sc.writeByte((byte) '\t');

            sc.writeStringByIndex(RNAME, i);
            sc.writeByte((byte) '\t');

            sc.printInt(POS, i);
            sc.writeByte((byte) '\t');
            sc.printInt(MAPQ, i);
            sc.writeByte((byte) '\t');

            sc.writeStringByIndex(CIGAR, i);
            sc.writeByte((byte) '\t');
            sc.writeStringByIndex(RNEXT, i);
            sc.writeByte((byte) '\t');
            sc.printInt(PNEXT, i);
            sc.writeByte((byte) '\t');
            sc.printInt(TLEN, i);
            sc.writeByte((byte) '\t');
            SeqPacked.writeSequence(sc, i);
            sc.writeByte((byte) '\t');
            QUAL.writeToWriter(sc, i);
            sc.writeByte((byte) '\n');
            TAG.writeToWriter(sc, i);

        }
    }

    public int size() {
        return FLAG.size();
    }
}