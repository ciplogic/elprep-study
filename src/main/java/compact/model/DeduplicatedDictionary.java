package compact.model;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;

import java.util.ArrayList;
import java.util.List;

public class DeduplicatedDictionary {
    private IntArrayList Indices;
    private Reference2IntArrayMap<String> _table ;
    private List<String> Values;

    public DeduplicatedDictionary() {
        int expectedItemsCount = 16;
        _table = new Reference2IntArrayMap<>(expectedItemsCount);
        Values = new ArrayList<>(expectedItemsCount);

        Indices =  new IntArrayList(expectedItemsCount);
    }

    public DeduplicatedDictionary(int expectedLength) {
        int expectedItemsCount = 16;
        _table = new Reference2IntArrayMap<>(expectedItemsCount);
        Values = new ArrayList<>(expectedItemsCount);

        Indices =  new IntArrayList(expectedLength);
    }

    public String add(byte[] valueBytes) {
        var value = new String(valueBytes);
        boolean hasValue = _table.containsKey(value);

        if (!hasValue) {
            int iResult = Values.size();
            Values.add(value);
            _table.put(value, iResult);
            Indices.add(iResult);
            return value;
        }
        var resultValue = _table.getInt(value);
        return Values.get(resultValue);
    }
}

