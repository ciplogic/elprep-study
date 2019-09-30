package compact;

public class StringScanner
{
    private String _line;
    private int Pos;

    void SetText(String line)
    {
        _line = line;
        Pos = 0;
    }


    public String doSlice()
    {
        var endIndex = _line.indexOf('\t', Pos);
        if (endIndex == -1)
        {
            return _line.substring(Pos);
        }

        var result = _line.substring(Pos, endIndex);
        Pos = endIndex + 1;
        return result;
    }

    public int doInt()
    {
        int result = 0;
        var isNegative = false;
        if (_line.charAt(Pos) == '-')
        {
            isNegative = true;
            Pos++;
        }

        while (true)
        {
            char ch = _line.charAt(Pos);
            Pos++;
            if (ch == '\t')
                return isNegative ? -result : result;
            int digit = ch - '0';
            result = result * 10 + digit;
        }
    }
}
