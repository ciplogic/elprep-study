package compact.model;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class StringArrayList {
    ByteArrayList _texts;
    IntArrayList _lengths;

    public StringArrayList(int expectedLength) {
        _texts = new ByteArrayList(expectedLength*3);
        _lengths = new IntArrayList(expectedLength);
    }

    public void add(byte[] text){
        var oldLen = 0;
        if(_lengths.size()!=0)
            oldLen = _lengths.getInt(_lengths.size()-1);
        var newLen = oldLen + text.length;
        _lengths.add(newLen);
        _texts.addElements(_texts.size(), text);
    }

    public void shrink(){
        _texts.trim();
    }
}
