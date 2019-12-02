package compact;

import commons.Utilities;
import compact.writer.BatchWrapperWriter;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.IOException;
import java.util.Arrays;

public class StringSequence {
    StringBuilder stringBuilder = new StringBuilder();
    public byte[] Values;
    IntArrayList Items = new IntArrayList();
    public void add(byte[] text)
    {
        String str = new String(text);
        stringBuilder.append(str);
        Items.add(Utilities.lastItem(Items) + text.length);
    }

    public void shrink(){
        Values = stringBuilder.toString().getBytes();
        stringBuilder= null;
    }


    public void writeToWriter(BatchWrapperWriter writer, int index) throws IOException {
        var start = 0;
        if (index>0) start = Items.getInt(index-1);
        var len = Items.getInt(index) - start;
        writer.write(Values, start, len);
    }
}
