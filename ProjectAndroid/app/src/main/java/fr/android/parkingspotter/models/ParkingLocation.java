package fr.android.parkingspotter.models;

public class ParkingLocation {
    private long localId; // id local SQLite
    private String remoteId; // UUID distant Supabase
    private double latitude;
    private double longitude;
    private String address;
    private String datetime;
    private String photoPath;

    public ParkingLocation(long localId, String remoteId, double latitude, double longitude, String address, String datetime, String photoPath) {
        this.localId = localId;
        this.remoteId = remoteId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.datetime = datetime;
        this.photoPath = photoPath;
    }
    public ParkingLocation(String remoteId, double latitude, double longitude, String address, String datetime, String photoPath) {
        this(-1, remoteId, latitude, longitude, address, datetime, photoPath);
    }
    public ParkingLocation(double latitude, double longitude, String address, String datetime, String photoPath) {
        this(-1, null, latitude, longitude, address, datetime, photoPath);
    }
    public long getLocalId() { return localId; }
    public void setLocalId(long localId) { this.localId = localId; }
    public String getRemoteId() { return remoteId; }
    public void setRemoteId(String remoteId) { this.remoteId = remoteId; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDatetime() { return datetime; }
    public void setDatetime(String datetime) { this.datetime = datetime; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
}
