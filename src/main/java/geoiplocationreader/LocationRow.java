package geoiplocationreader;

public class LocationRow {
    long ipLow;
    long ipHigh;
    String countryCode;
    String country;
    String region;
    String city;
    double latitude;
    double longitude;
    String zipCode;

    public LocationRow(String[] items){
        ipLow = Long.parseLong(items[0]);
        ipHigh = Long.parseLong(items[1]);
        countryCode = items[2];
        country = items[3];
        region = items[4];
        city = items[5];
        latitude = Double.parseDouble(items[6]);
        longitude = Double.parseDouble(items[7]);
        zipCode = items[8];
    }
}
