package geoiplocationreader;

import compact.DeduplicatedDictionary;
import compact.StringSequence;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class CompactLocationsRepo {
    LongArrayList ipLow = new LongArrayList();
    LongArrayList ipHigh = new LongArrayList();
    DeduplicatedDictionary countryCode = new DeduplicatedDictionary();
    DeduplicatedDictionary country = new DeduplicatedDictionary();
    DeduplicatedDictionary region = new DeduplicatedDictionary();
    StringSequence city = new StringSequence();
    DoubleArrayList latitude = new DoubleArrayList();
    DoubleArrayList longitude = new DoubleArrayList();
    StringSequence zipCode = new StringSequence();

    public void addLocation(String[] items) {

        ipLow.add(Long.parseLong(items[0]));
        ipHigh.add(Long.parseLong(items[1]));
        countryCode.add(items[2]);
        country.add(items[3]);
        region.add(items[4]);
        city.add(items[5].getBytes());
        latitude.add(Double.parseDouble(items[6]));
        longitude.add(Double.parseDouble(items[7]));
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
