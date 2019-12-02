package compact;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;

public class DeduplicatedDictionary {

    Object2IntOpenHashMap<String> _table ;
    public ArrayList<byte[]> Values;
    public IntArrayList Items;

    public DeduplicatedDictionary() {
        int expectedItemsCount = 16;
        _table = new Object2IntOpenHashMap<>(expectedItemsCount);
        Values = new ArrayList<>(expectedItemsCount);
        Items = new IntArrayList();
    }

    public String add(String value) {
        int resultValue = _table.getOrDefault(value, -1);

        if (resultValue==-1) {
            resultValue = Values.size();
            Values.add(value.getBytes());
            _table.put(value, resultValue);
            Items.add((int)resultValue);
            return value;
        }

        Items.add(resultValue);
        return new String(Values.get(resultValue));
    }

}

