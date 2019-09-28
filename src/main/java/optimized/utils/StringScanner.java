/*
	Copyright (c) 2018 by imec vzw, Leuven, Belgium. All rights reserverd.
*/

package optimized.utils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static optimized.Fields.*;

public class StringScanner {
    public static FieldParser[] optionalFieldParseTable = new FieldParser[41];

    static {
        optionalFieldParseTable['A' - 'A'] = StringScanner::parseSamChar;
        optionalFieldParseTable['i' - 'A'] = StringScanner::parseSamInteger;
        optionalFieldParseTable['f' - 'A'] = StringScanner::parseSamFloat;
        optionalFieldParseTable['Z' - 'A'] = StringScanner::parseSamString;
        optionalFieldParseTable['H' - 'A'] = StringScanner::parseSamByteArray;
        optionalFieldParseTable['B' - 'A'] = StringScanner::parseSamNumericArray;
    }

    private byte[] data;
    private int pos;
    private int length;
    private char charResult;
    private Slice sliceResult;

    public StringScanner() {
    }

    public StringScanner(byte[] sBytes) {
        this(sBytes, 0, sBytes.length);
    }

    public StringScanner(byte[] sBytes, int pos, int length) {
        this.data = sBytes;
        this.pos = pos;
        this.length = length;
    }

    public static Map<Slice, Slice> parseSamHeaderLineFromString(String line) {
        Map<Slice, Slice> record = new HashMap<Slice, Slice>(8);
        String[] fields = line.split("\\s+");
        for (String field : fields) {
            assert field.charAt(2) == ':' : "Incorrectly formatted SAM file field: " + field + " .";
            Slice tag = new Slice(field.substring(0, 2));
            Slice value = new Slice(field.substring(3));
            if (!Slice.setUniqueEntry(record, tag, value)) {
                throw new RuntimeException("Duplicate field tag " + tag + " in SAM header line.");
            }
        }
        return record;
    }

    public void reset(byte[] sBytes, int pos, int length) {
        this.data = sBytes;
        this.pos = pos;
        this.length = length;
    }

    public int length() {
        return length - pos;
    }

    public boolean readCharUntil(char c) {
        int start = pos;
        int next = pos + 1;
        if (next >= length) {
            pos = length;
            charResult = (char) data[start];
            return false;
        } else if (data[(next)] != c) {
            throw new RuntimeException("Unexpected character " + data[next] + " in StringScanner.readCharUntil.");
        } else {
            pos = next + 1;
            charResult = (char) data[start];
            return true;
        }
    }

    public boolean readUntil(char c) {
        int start = pos;
        for (int end = pos; end < length; ++end) {
            if (data[end] == c) {
                pos = end + 1;
                sliceResult = new Slice(data, start, end - start);
                return true;
            }
        }
        pos = length;
        sliceResult = new Slice(data, start, length - start);
        return false;
    }

    public char readUntil2(char c1, char c2) {
        int start = pos;
        for (int end = pos; end < length; ++end) {
            byte c = data[end];
            if ((c == c1) || (c == c2)) {
                pos = end + 1;
                sliceResult = new Slice(data, start, end - start);
                return (char) c;
            }
        }
        pos = length;
        sliceResult = new Slice(data, start, length - start);
        return 0;
    }

    public Map<Slice, Slice> parseSamHeaderLine() {
        Map<Slice, Slice> record = new HashMap<Slice, Slice>(8);
        while (length() > 0) {
            boolean ok = readUntil(':');
            Slice tag = sliceResult;
            assert ok && (tag.length() == 2) : "Invalid field type tag " + tag + ".";
            readUntil('\t');
            Slice value = sliceResult;
            if (!Slice.setUniqueEntry(record, tag, value)) {
                throw new RuntimeException("Duplicate field tag " + tag + " in SAM header line.");
            }
        }
        return record;
    }

    public Field parseSamChar(Slice tag) {
        readCharUntil('\t');
        return new CharacterField(tag, charResult);
    }

    public Field parseSamInteger(Slice tag) {
        readUntil('\t');
        return new IntegerField(tag, Integer.parseInt(sliceResult.toString(), 10));
    }

    public Field parseSamFloat(Slice tag) {
        readUntil('\t');
        return new FloatField(tag, Float.parseFloat(sliceResult.toString()));
    }

    public Field parseSamString(Slice tag) {
        readUntil('\t');
        return new StringField(tag, sliceResult);
    }

    public Field parseSamByteArray(Slice tag) {
        readUntil('\t');
        Slice value = sliceResult;
        short[] byteArray = new short[value.length >> 1];
        for (int i = 0; i < value.length; i += 2) {
            byteArray[i >> 1] = (short) Integer.parseUnsignedInt(sliceResult.toString().substring(i, i + 2), 16);
        }
        return new ByteArrayField(tag, byteArray);
    }

    public Field parseSamNumericArray(Slice tag) {
        boolean ok = readUntil(',');
        assert ok : "Missing entry in numeric array.";
        char ntype = charResult;
        char sep;
        switch (ntype) {
            case 'c': {
                List<Byte> nums = new ArrayList<Byte>();
                do {
                    sep = readUntil2(',', '\t');
                    nums.add((byte) Integer.parseInt(sliceResult.toString(), 10));
                } while (sep == ',');
                return new NumericArrayField<Byte>(tag, ntype, nums);
            }
            case 'C': {
                List<Short> nums = new ArrayList<Short>();
                do {
                    sep = readUntil2(',', '\t');
                    nums.add((short) Integer.parseUnsignedInt(sliceResult.toString(), 10));
                } while (sep == ',');
                return new NumericArrayField<Short>(tag, ntype, nums);
            }
            case 's': {
                List<Short> nums = new ArrayList<Short>();
                do {
                    sep = readUntil2(',', '\t');
                    nums.add((short) Integer.parseInt(sliceResult.toString(), 10));
                } while (sep == ',');
                return new NumericArrayField<Short>(tag, ntype, nums);
            }
            case 'S': {
                List<Integer> nums = new ArrayList<Integer>();
                do {
                    sep = readUntil2(',', '\t');
                    nums.add(Integer.parseUnsignedInt(sliceResult.toString(), 10));
                } while (sep == ',');
                return new NumericArrayField<Integer>(tag, ntype, nums);
            }
            case 'i': {
                List<Integer> nums = new ArrayList<Integer>();
                do {
                    sep = readUntil2(',', '\t');
                    nums.add(Integer.parseInt(sliceResult.toString(), 10));
                } while (sep == ',');
                return new NumericArrayField<Integer>(tag, ntype, nums);
            }
            case 'I': {
                List<Long> nums = new ArrayList<Long>();
                do {
                    sep = readUntil2(',', '\t');
                    nums.add(Long.parseUnsignedLong(sliceResult.toString(), 10));
                } while (sep == ',');
                return new NumericArrayField<Long>(tag, ntype, nums);
            }
            case 'f': {
                List<Float> nums = new ArrayList<Float>();
                do {
                    sep = readUntil2(',', '\t');
                    nums.add(Float.parseFloat(sliceResult.toString()));
                } while (sep == ',');
                return new NumericArrayField<Float>(tag, ntype, nums);
            }
            default:
                throw new RuntimeException("Invalid numeric array type " + ntype + ".");
        }
    }

    public Slice doSlice() {
        boolean ok = readUntil('\t');
        assert ok : "Missing tabulator in SAM alignment line.";
        return sliceResult;
    }

    public Slice doSlicen() {
        readUntil('\t');
        return sliceResult;
    }

    public int doInt() {
        int result = 0;
        boolean isNegative = false;
        if (this.data[pos] == '-') {
            isNegative = true;
            pos++;
        }
        while (true) {
            byte ch = this.data[pos++];
            if (ch == '\t')
                return isNegative ? -result : result;
            int digit = ch - '0';
            result = result * 10 + digit;
        }
        //doSlice(); return Integer.parseInt(sliceResult, 0, sliceResult.length(), 10);
    }

    public Field parseSamAlignmentField() {
        boolean ok = readUntil(':');
        Slice tag = sliceResult;
        assert ok && tag.length == 2 : "Invalid field tag " + tag + " in SAM alignment line.";
        ok = readCharUntil(':');
        char typebyte = charResult;
        assert ok : "Invalid field type " + typebyte + " in SAM alignment line.";
        return optionalFieldParseTable[typebyte - 'A'].parse(this, tag);
    }

    public interface FieldParser {
        Field parse(StringScanner sc, Slice tag);
    }
}
