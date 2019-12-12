package compact;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2CharOpenHashMap;

import java.util.ArrayList;

public class DeduplicatedDictionary {

    public ArrayList<byte[]> Values;
    public IntArrayList Items;
    Object2CharOpenHashMap<String> _table;

    public DeduplicatedDictionary() {
        int expectedItemsCount = 16;
        _table = new Object2CharOpenHashMap<>(expectedItemsCount);
        Values = new ArrayList<>(expectedItemsCount);
        Items = new IntArrayList();
    }

    public String add(String value) {
        var resultValue = _table.getOrDefault(value, (char) 0xffff);

        if (resultValue == (char) 0xffff) {
            resultValue = (char) Values.size();
            Values.add(value.getBytes());
            _table.put(value, resultValue);
            Items.add(resultValue);
            return value;
        }

        Items.add(resultValue);
        return new String(Values.get(resultValue));
    }

}

