package compact;

import commons.Utilities;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class StringSequence {
    Object stringBuilder = new StringBuilder();
    IntArrayList Items = new IntArrayList();
    public void add(byte[] text)
    {
        String str = new String(text);
        ((StringBuilder)stringBuilder).append(str);
        Items.add(Utilities.lastItem(Items) + text.length);
    }

    public void shrink(){
        stringBuilder = ((StringBuilder)stringBuilder).toString().getBytes();
    }

}
