package compact;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeduplicatedDictionary {

    Map<String, Integer> _table ;
    public List<String> Values;
    IntArrayList Items;

    public DeduplicatedDictionary() {
        int expectedItemsCount = 16;
        _table = new HashMap<>(expectedItemsCount);
        Values = new ArrayList<>(expectedItemsCount);
        Items = new IntArrayList();
    }

    public String add(String value) {
        Integer resultValue = _table.get(value);

        if (resultValue==null) {
            resultValue = Values.size();
            Values.add(value);
            _table.put(value, resultValue);
            Items.add((int)resultValue);
            return value;
        }

        Items.add((int)resultValue);
        return Values.get(resultValue);
    }
}

