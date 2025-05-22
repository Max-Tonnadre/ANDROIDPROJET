package fr.android.parkingspotter.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import fr.android.parkingspotter.models.ParkingLocation;
import java.util.ArrayList;
import java.util.List;

public class ParkingDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "parking.db";
    private static final int DATABASE_VERSION = 2;
    public static final String TABLE_PARKING = "parking_locations";
    public static final String COL_ID = "id";
    public static final String COL_REMOTE_ID = "remote_id"; // UUID distant (String)
    public static final String COL_LAT = "latitude";
    public static final String COL_LNG = "longitude";
    public static final String COL_ADDRESS = "address";
    public static final String COL_DATETIME = "datetime";
    public static final String COL_PHOTO = "photoPath";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_PARKING + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_REMOTE_ID + " TEXT, " +
            COL_LAT + " REAL, " +
            COL_LNG + " REAL, " +
            COL_ADDRESS + " TEXT, " +
            COL_DATETIME + " TEXT, " +
            COL_PHOTO + " TEXT)";

    public ParkingDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PARKING);
        onCreate(db);
    }

    public long addParkingLocation(ParkingLocation location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_REMOTE_ID, location.getRemoteId());
        values.put(COL_LAT, location.getLatitude());
        values.put(COL_LNG, location.getLongitude());
        values.put(COL_ADDRESS, location.getAddress());
        values.put(COL_DATETIME, location.getDatetime());
        values.put(COL_PHOTO, location.getPhotoPath());
        long localId = db.insert(TABLE_PARKING, null, values);
        db.close();
        return localId;
    }
    public long addParkingLocationWithRemoteId(ParkingLocation location, String remoteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_REMOTE_ID, remoteId);
        values.put(COL_LAT, location.getLatitude());
        values.put(COL_LNG, location.getLongitude());
        values.put(COL_ADDRESS, location.getAddress());
        values.put(COL_DATETIME, location.getDatetime());
        values.put(COL_PHOTO, location.getPhotoPath());
        long rowId = db.insertWithOnConflict(TABLE_PARKING, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return rowId;
    }
    public List<ParkingLocation> getAllParkingLocations() {
        List<ParkingLocation> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PARKING, null, null, null, null, null, COL_ID + " DESC");
        if (cursor.moveToFirst()) {
            do {
                long localId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                String remoteId = cursor.getString(cursor.getColumnIndexOrThrow(COL_REMOTE_ID));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LAT));
                double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LNG));
                String address = cursor.getString(cursor.getColumnIndexOrThrow(COL_ADDRESS));
                String datetime = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATETIME));
                String photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO));
                list.add(new ParkingLocation(localId, remoteId, lat, lng, address, datetime, photoPath));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
    public void deleteParkingLocationByLocalId(long localId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PARKING, COL_ID + "=?", new String[]{String.valueOf(localId)});
        db.close();
    }
    public void deleteParkingLocationByRemoteId(String remoteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PARKING, COL_REMOTE_ID + "=?", new String[]{remoteId});
        db.close();
    }
    public void clearAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PARKING, null, null);
        db.close();
    }
}
