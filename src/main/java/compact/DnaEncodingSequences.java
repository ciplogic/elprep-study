package compact;

import compact.writer.BatchWrapperWriter;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import java.io.IOException;

public class DnaEncodingSequences {

    ShortArrayList lengths;
    LongArrayList fullSequences;
    IntArrayList startIndex;

    DnaEncodingSequences(int expectedLength)
    {
        lengths = new ShortArrayList(expectedLength);
        fullSequences = new LongArrayList(expectedLength);
        startIndex = new IntArrayList(expectedLength);
    }

    private static long charLetterEncode(byte ch)
    {
        switch (ch)
        {
            case 'C': return 1;
            case 'G': return 2;
            case 'T': return 3;
            case 'N': return 4;
            default:
                return 0;
        }
    }
    public static char charLetterDecode(int ch)
    {
        switch (ch)
        {
            case 1: return 'C';
            case 2: return 'G';
            case 3: return 'T';
            case 4: return 'N';
            default:
                return 'A';
        }
    }
    public static long[] EncodeSequence(byte[] sequence)
    {
        var sequenceLength = sequence.length;
        var result = new LongArrayList(sequenceLength/21+1);
        long combinedCode = 0;
        var remainder = 0;
        var shifter = 0;
        for (var index = 0; index < sequenceLength; index++)
        {
            var dnaLetter = sequence[index];
            if (remainder == 0)
            {
                if (index != 0)
                {
                    result.set(result.size()-1, combinedCode);
                }
                result.add(0);
                combinedCode = 0;
            }

            var encodedLetter = charLetterEncode(dnaLetter);
            combinedCode += encodedLetter << (shifter);

            remainder ++;


            if (remainder==21) {
                remainder = 0;
                shifter = 0;
            } else {
                shifter += 3;
            }

        }

        result.set(result.size()-1, combinedCode);
        var encodeSequence = result.toLongArray();
//        assert(sequence.equals(DecodeSequence(encodeSequence, sequenceLength)));
        return encodeSequence;
    }

    public static String decodeSequence(long[] input, int length)
    {
        var sb = new StringBuilder();
        for (var i = 0; i < length; i++)
        {
            var divide = i / 21;
            var remainderIndex = i % 21;
            var charSeq = input[divide];
            var charShifted = (int)(charSeq >> (remainderIndex * 3)) & 7;
            var charDecoded = charLetterDecode(charShifted);
            sb.append(charDecoded);
        }
        return sb.toString();
    }

    public void add(byte[] seqText)
    {
        var encoded = EncodeSequence(seqText);
        startIndex.add(fullSequences.size());
        for(var l : encoded)
        {
            fullSequences.add(l);
        }
        lengths.add((short) seqText.length);
    }

    public void shrink() {
        fullSequences.trim();
    }

    public void writeSequence(BatchWrapperWriter sc, int index) throws IOException {
        var start = startIndex.getInt(index);
        var len = (int) lengths.getShort(index);
        sc.writeSequence(this.fullSequences, start, len);

    }
}
