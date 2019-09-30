package compact.reader;

import commons.Utilities;

import java.util.Arrays;

public class StringScanner
{
    private byte[] _line;
    private int Pos;

    public void setText(byte[] line)
    {
        _line = line;
        Pos = 0;
    }


    public byte[] doSlice()
    {
        var endIndex = Utilities.indexOfByte(_line, '\t', Pos);
        if (endIndex == -1)
        {
            return Arrays.copyOf(_line, Pos);
        }

        var result = Arrays.copyOfRange(_line, Pos, endIndex);
        Pos = endIndex + 1;
        return result;
    }

    public int doInt()
    {
        int result = 0;
        var isNegative = false;
        if (_line[Pos] == '-')
        {
            isNegative = true;
            Pos++;
        }

        while (true)
        {
            var ch = _line[Pos];
            Pos++;
            if (ch == '\t')
                return isNegative ? -result : result;
            int digit = ch - '0';
            result = result * 10 + digit;
        }
    }
}
