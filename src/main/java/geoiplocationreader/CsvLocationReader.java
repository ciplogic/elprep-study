package geoiplocationreader;

import java.util.ArrayList;

public class CsvLocationReader {
    public CsvLocationReader(){
    }

    int posOfQuote(String text, int pos){
        for(var i = pos; i<text.length(); i++) {
            var currChar =text.charAt(i);
            if (currChar == '"')
                return i;
        }
        return -1;
    }

    String[] splitByCommasNoQuotes(String _row){
        var items = new ArrayList<String>();

        int pos = 0;

        while (pos < _row.length())
        {
            var endQuote = posOfQuote(_row, pos+1);
            var textInQuotes = _row.substring(pos+1, endQuote);
            items.add(textInQuotes);
            pos = endQuote + 2;
        }
        var resultArray = new String[items.size()];
        items.toArray(resultArray);
        return resultArray;
    }
}
