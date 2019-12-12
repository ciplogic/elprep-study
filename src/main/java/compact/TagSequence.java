package compact;

import compact.reader.StringScanner;
import compact.writer.BatchWrapperWriter;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2CharOpenHashMap;

import java.io.IOException;
import java.util.ArrayList;

public class TagSequence {
    public ArrayList<byte[]> Values = new ArrayList<>();
    private IntArrayList _lengths = new IntArrayList();
    private CharArrayList _tagSequence = new CharArrayList();
    private Object2CharOpenHashMap<String> _table = new Object2CharOpenHashMap<>();

    public void shrink() {
        _lengths.trim();
        _tagSequence.trim();
        Values.trimToSize();
    }

    public int addAndGetIndex(String value) {
        var resultValue = _table.getOrDefault(value, (char) 0xffff);

        if (resultValue == (char) 0xffff) {
            resultValue = (char) Values.size();
            Values.add(value.getBytes());
            _table.put(value, resultValue);
            return resultValue;
        }

        return resultValue;
    }

    public void readRow(StringScanner sc) {
        String text;
        do {
            text = sc.doSliceStr();
            if (text.length() == 0)
                break;
            var indexText = addAndGetIndex(text);
            _tagSequence.add((char) indexText);
        } while (true);
        _lengths.add(_tagSequence.size());
    }

    public void writeToWriter(BatchWrapperWriter writer, int index) throws IOException {
        var start = 0;
        if (index > 0) start = _lengths.getInt(index - 1);
        var len = _lengths.getInt(index) - start;
        for (var i = 0; i < len; i++) {
            var indexInValues = (int) _tagSequence.getChar(start + i);
            writer.write(Values.get(indexInValues));
            if (i != len - 1) {
                writer.writeByte((byte) '\t');
            }
        }

    }
}
