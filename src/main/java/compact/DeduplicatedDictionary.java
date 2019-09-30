package compact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeduplicatedDictionary {

    Map<String, Integer> _table ;
    public List<String> Values;

    public DeduplicatedDictionary() {
        int expectedItemsCount = 16;
        _table = new HashMap<>(expectedItemsCount);
        Values = new ArrayList<>(expectedItemsCount);
    }

    public String add(String value) {
        Integer resultValue = _table.get(value);

        if (resultValue==null) {
            resultValue = Values.size();
            Values.add(value);
            _table.put(value, resultValue);
            return value;
        }

        return Values.get(resultValue);
    }
}

