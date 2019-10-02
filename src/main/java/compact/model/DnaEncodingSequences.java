package compact.model;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class DnaEncodingSequences {

    private IntArrayList _lengths;
    private LongArrayList _fullSequences;

    DnaEncodingSequences(int expectedLength)
    {
        _lengths = new IntArrayList(expectedLength);
        _fullSequences = new LongArrayList(expectedLength);
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
    private static char charLetterDecode(int ch)
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

    int roundUpValue(int value, int base){
        var rem = value% base;
        var div = value / base;
        if (rem==0)
            return value;
        return (div+1)*base;
    }

    public void add(byte[] seqText)
    {
        var encoded = EncodeSequence(seqText);
        for(var l : encoded)
        {
            _fullSequences.add(l);
        }
        var startIndex = 0;
        if (_lengths.size()!=0){
            startIndex = roundUpValue(_lengths.getInt(_lengths.size()-1), 21);

        }
        _lengths.add(startIndex+seqText.length);
    }

    public void shrink() {
        _fullSequences.trim();
    }
}
