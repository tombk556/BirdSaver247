package htwd.s224.gruppe1.mnbirdsaver;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.UUID;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Measurements.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "Measurement";
    public static final String MEASUREMENT_ID = "measurement_id";
    public static final String PIXEL_X_COORD = "pixel_x_coord";
    public static final String PIXEL_Y_COORD = "pixel_y_coord";

    public static final String GPS_X_COORD = "gps_x_coord";

    public static final String GPS_Y_COORD = "gps_y_coord";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
            + MEASUREMENT_ID + " TEXT PRIMARY KEY,"
            + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + PIXEL_X_COORD + " INTEGER NOT NULL,"
            + PIXEL_Y_COORD + " INTEGER NOT NULL,"
            + GPS_X_COORD + " FLOAT NOT NULL,"
            + GPS_Y_COORD + " FLOAT NOT NULL "
            + ");";
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addRedPixel(int pix_x, int pix_y, float gps_x, float gps_y ) {

        SQLiteDatabase db = this.getWritableDatabase();
        String uuid = UUID.randomUUID().toString();
        ContentValues values = new ContentValues();

        values.put(MEASUREMENT_ID, uuid);
        values.put(PIXEL_X_COORD, pix_x);
        values.put(PIXEL_Y_COORD, pix_y);
        values.put(GPS_X_COORD, gps_x);
        values.put(GPS_Y_COORD, gps_y);

        db.insert(TABLE_NAME, null, values);
        db.close();
    }
}
