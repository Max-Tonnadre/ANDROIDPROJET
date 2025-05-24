package fr.android.projectandroid.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

public class LocationDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "locations.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_LOCATIONS = "localisation";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LAT = "latitude";
    public static final String COLUMN_LNG = "longitude";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_LOCATIONS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_LAT + " REAL, " +
                    COLUMN_LNG + " REAL);";

    public LocationDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        onCreate(db);
    }

    public void insertLocation(double latitude, double longitude) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAT, latitude);
        values.put(COLUMN_LNG, longitude);
        db.insert(TABLE_LOCATIONS, null, values);
        db.close();
    }
}
