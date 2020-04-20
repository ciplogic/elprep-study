package geoiplocationreader;

import compact.DeduplicatedDictionary;
import compact.StringSequence;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class CompactLocationOptimizedRepo {
    IntArrayList ipLow = new IntArrayList();
    IntArrayList ipHigh = new IntArrayList();
    DeduplicatedDictionary countryCode = new DeduplicatedDictionary();
    DeduplicatedDictionary country = new DeduplicatedDictionary();
    DeduplicatedDictionary region = new DeduplicatedDictionary();
    DeduplicatedDictionary city = new DeduplicatedDictionary();
    FloatArrayList latitude = new FloatArrayList();
    FloatArrayList longitude = new FloatArrayList();
    StringSequence zipCode = new StringSequence();

    public void addLocation(String[] items) {

        ipLow.add((int)Long.parseLong(items[0]));
        ipHigh.add((int)Long.parseLong(items[1]));
        countryCode.add(items[2]);
        country.add(items[3]);
        region.add(items[4]);
        city.add(items[5]);
        latitude.add(Float.parseFloat(items[6]));
        longitude.add(Float.parseFloat(items[7]));
        zipCode.add(items[8].getBytes());
    }

    public void trim() {
        ipLow.trim();
        ipHigh.trim();
        latitude.trim();
        longitude.trim();
        zipCode.shrink();

    }

}
