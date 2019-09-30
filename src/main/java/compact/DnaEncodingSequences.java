package compact;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class DnaEncodingSequences {

    ShortArrayList lengths;
    LongArrayList fullSequences;
    IntArrayList startIndex;

    public DnaEncodingSequences(int expectedLength)
    {
        lengths = new ShortArrayList(expectedLength);
        fullSequences = new LongArrayList(expectedLength);
        startIndex = new IntArrayList(expectedLength);
    }

    static long CharLetterEncode(char ch)
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
    static char CharLetterDecode(int ch)
    {
        switch (ch)
        {
            case 0: return 'A';
            case 1: return 'C';
            case 2: return 'G';
            case 3: return 'T';
            case 4: return 'N';
            default:
                return 'A';
        }
    }
    public static long[] EncodeSequence(String sequence)
    {
        var sequenceLength = sequence.length();
        var result = new LongArrayList(sequenceLength/21+1);
        long combinedCode = 0;
        var remainder = 0;
        var shifter = 0;
        for (var index = 0; index < sequenceLength; index++)
        {
            var dnaLetter = sequence.charAt(index);
            if (remainder == 0)
            {
                if (index != 0)
                {
                    result.set(result.size()-1, combinedCode);
                }
                result.add(0);
                combinedCode = 0;
            }

            var encodedLetter = CharLetterEncode(dnaLetter);
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

    public static String DecodeSequence(long[] input, int length)
    {
        var sb = new StringBuilder();
        for (var i = 0; i < length; i++)
        {
            var divide = i / 21;
            var remainderIndex = i % 21;
            var charSeq = input[divide];
            var charShifted = (int)(charSeq >> (remainderIndex * 3)) & 7;
            var charDecoded = CharLetterDecode(charShifted);
            sb.append(charDecoded);
        }
        return sb.toString();
    }

    public void Add(String seqText)
    {
        var encoded = EncodeSequence(seqText);
        for(var l : encoded)
        {
            fullSequences.add(l);
        }
        startIndex.add(lengths.size());
        lengths.add((short) seqText.length());
    }

    public void shrink() {
        fullSequences.trim();
    }
}
