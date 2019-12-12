package compact.reader;

import commons.Utilities;

import java.util.Arrays;

public class StringScanner {
    private byte[] _line;
    private int Pos;

    public void setText(byte[] line) {
        _line = line;
        Pos = 0;
    }

    @Override
    public String toString() {
        if (Pos >= _line.length)
            return Pos + ": empty";
        return Pos + ": '" + new String(_line).substring(Pos) + "'";
    }

    public byte[] doSlice() {
        var endIndex = Utilities.indexOfByte(_line, '\t', Pos);
        if (endIndex == -1) {
            if (Pos == _line.length)
                return new byte[0];
            var pos = Pos;
            Pos = _line.length;
            return Arrays.copyOf(_line, pos);
        }

        var result = Arrays.copyOfRange(_line, Pos, endIndex);
        Pos = endIndex + 1;
        return result;
    }


    public String doSliceStr() {
        var endIndex = Utilities.indexOfByte(_line, '\t', Pos);
        if (endIndex == -1) {
            if (Pos == _line.length)
                return "";
            var pos = Pos;
            Pos = _line.length;
            var str = new String(Arrays.copyOfRange(_line, pos, Pos));
            return str;
        }

        var result = Arrays.copyOfRange(_line, Pos, endIndex);
        Pos = endIndex + 1;
        var str = new String(result);
        return str;
    }

    public int doInt() {
        int result = 0;
        var isNegative = false;
        if (_line[Pos] == '-') {
            isNegative = true;
            Pos++;
        }

        while (true) {
            var ch = _line[Pos];
            Pos++;
            if (ch == '\t')
                return isNegative ? -result : result;
            int digit = ch - '0';
            result = result * 10 + digit;
        }
    }
}
