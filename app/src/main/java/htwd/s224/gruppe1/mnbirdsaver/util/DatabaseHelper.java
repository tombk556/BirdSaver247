package htwd.s224.gruppe1.mnbirdsaver.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    // Datenbank Name und Version
    private static final String DATABASE_NAME = "birdsaver.db";
    private static final int DATABASE_VERSION = 1;

    // Tabellenname und Spalten für wind_turbine
    public static final String TABLE_WIND_TURBINE = "wind_turbine";
    public static final String COLUMN_WIND_TURBINE_ID = "id";
    public static final String COLUMN_WIND_TURBINE_NAME = "name";
    private static final String COLUMN_WIND_TURBINE_IP_ADDRESS = "ip_address";  // Neue Spalte

    // Tabellenname und Spalten für measurement
    public static final String TABLE_MEASUREMENT = "measurement";
    public static final String COLUMN_MEASUREMENT_ID = "id";
    public static final String COLUMN_PIXEL_X = "pixel_x";
    public static final String COLUMN_PIXEL_Y = "pixel_y";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_LATITUDE = "latitude";

    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_WIND_TURBINE_ID_FK = "wind_turbine_id";

    // View Name
    public static final String VIEW_MEASUREMENT_WITH_WIND_TURBINE = "wind_turbine_measurement";

    private Context context;

    private static final String PREFS_NAME = "WindTurbinePrefs";
    private static final String LAST_WIND_TURBINE_ID = "LastWindTurbineId";

    private SharedPreferences sharedPreferences;

    // SQL-Befehl zum Erstellen der Tabelle wind_turbine
    private static final String CREATE_TABLE_WIND_TURBINE = "CREATE TABLE " + TABLE_WIND_TURBINE + "("
            + COLUMN_WIND_TURBINE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_WIND_TURBINE_NAME + " TEXT NOT NULL, "
            + COLUMN_WIND_TURBINE_IP_ADDRESS + " TEXT NOT NULL" + ")";

    // SQL-Befehl zum Erstellen der Tabelle measurement
    private static final String CREATE_TABLE_MEASUREMENT = "CREATE TABLE " + TABLE_MEASUREMENT + "("
            + COLUMN_MEASUREMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_PIXEL_X + " INTEGER NOT NULL, "
            + COLUMN_PIXEL_Y + " INTEGER NOT NULL, "
            + COLUMN_LONGITUDE + " DOUBLE NOT NULL, "
            + COLUMN_LATITUDE + " DOUBLE NOT NULL, "
            + COLUMN_WIND_TURBINE_ID_FK + " INTEGER NOT NULL, "
            + COLUMN_TIMESTAMP + " TEXT,"
            + "FOREIGN KEY(" + COLUMN_WIND_TURBINE_ID_FK + ") REFERENCES " + TABLE_WIND_TURBINE + "(" + COLUMN_WIND_TURBINE_ID + "));";

    // SQL-Befehl zum Erstellen der View
    private static final String CREATE_VIEW_MEASUREMENT_WITH_WIND_TURBINE = "CREATE VIEW " + VIEW_MEASUREMENT_WITH_WIND_TURBINE + " AS "
            + "SELECT "
            + TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_ID + " AS wind_turbine_id, "
            + TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_NAME + " AS wind_turbine_name, "
            + TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_IP_ADDRESS + " AS wind_turbine_ip_adress, "
            + TABLE_MEASUREMENT + "." + COLUMN_MEASUREMENT_ID + " AS measurement_id, "
            + TABLE_MEASUREMENT + "." + COLUMN_PIXEL_X + " AS pixel_x, "
            + TABLE_MEASUREMENT + "." + COLUMN_PIXEL_Y + " AS pixel_y, "
            + TABLE_MEASUREMENT + "." + COLUMN_LONGITUDE + " AS longitude, "
            + TABLE_MEASUREMENT + "." + COLUMN_LATITUDE + " AS latitude "
            + "FROM " + TABLE_MEASUREMENT + " "
            + "JOIN " + TABLE_WIND_TURBINE + " ON " + TABLE_MEASUREMENT + "." + COLUMN_WIND_TURBINE_ID_FK + " = " + TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_ID + ";";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabellen erstellen
        Log.d(TAG, "Creating tables");
        db.execSQL(CREATE_TABLE_WIND_TURBINE);
        db.execSQL(CREATE_TABLE_MEASUREMENT);

        // View erstellen
        Log.d(TAG, "Creating view");
        db.execSQL(CREATE_VIEW_MEASUREMENT_WITH_WIND_TURBINE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        resetDatabase(db);
    }

    // Methode zum Einfügen eines Windrads und Rückgabe der neuen ID
    public long addWindTurbine(String name, String ipAddress) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WIND_TURBINE_NAME, name);
        values.put(COLUMN_WIND_TURBINE_IP_ADDRESS, ipAddress);

        long newId = db.insert(TABLE_WIND_TURBINE, null, values);
        if (newId == -1) {
            Log.e(TAG, "Wind Turbine konnte nicht eingefügt werden");
        } else {
            Log.d(TAG, "Wind Turbine erfolgreich eingefügt mit ID: " + newId);
            saveLastWindTurbineId(newId);  // Speichere die ID der neu eingefügten Windturbine
        }
        db.close();
        return newId;
    }

    // Methode zum Einfügen einer Messung
    public void addMeasurement(int pixelX, int pixelY, double longitude, double latitude, int windTurbineId, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PIXEL_X, pixelX);
        values.put(COLUMN_PIXEL_Y, pixelY);
        values.put(COLUMN_LONGITUDE, longitude);
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_WIND_TURBINE_ID_FK, windTurbineId);
        values.put(COLUMN_TIMESTAMP, timestamp);

        long result = db.insert(TABLE_MEASUREMENT, null, values);
        if (result == -1) {
            Log.e(TAG, "Measurement konnte nicht eingefügt werden");
        } else {
            Log.d(TAG, "Measurement erfolgreich eingefügt");
        }
        db.close();
    }

    // Methode zum Löschen der Datenbank
    public void deleteDatabase() {
        context.deleteDatabase(DATABASE_NAME);
        Log.d(TAG, "Database deleted");
    }

    // Methode zum Zurücksetzen der Datenbank
    public void resetDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        resetDatabase(db);
        db.close();

        saveLastWindTurbineId(0);
    }

    // Interne Methode zum Zurücksetzen der Datenbank
    private void resetDatabase(SQLiteDatabase db) {
        Log.d(TAG, "Resetting database");
        db.execSQL("DROP VIEW IF EXISTS " + VIEW_MEASUREMENT_WITH_WIND_TURBINE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEASUREMENT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIND_TURBINE);
        onCreate(db);


    }

    private void saveLastWindTurbineId(long windTurbineId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(LAST_WIND_TURBINE_ID, windTurbineId);
        editor.apply();
    }

    public long getLastWindTurbineId() {
        return sharedPreferences.getLong(LAST_WIND_TURBINE_ID, 0);
    }

    public String getWindTurbineIpAddress(int windTurbineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_WIND_TURBINE_IP_ADDRESS + " FROM " + TABLE_WIND_TURBINE + " WHERE " + COLUMN_WIND_TURBINE_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(windTurbineId)});
        String ipAddress = null;
        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(COLUMN_WIND_TURBINE_IP_ADDRESS);
            if (columnIndex != -1) {
                ipAddress = cursor.getString(columnIndex);
            } else {
                Log.e(TAG, "Spalte " + COLUMN_WIND_TURBINE_IP_ADDRESS + " nicht gefunden.");
                ipAddress = "0";
            }
        } else {
            Log.d(TAG, "WindTurbine ID " + windTurbineId + " nicht gefunden.");
            ipAddress = "0"; // Rückgabe "0" wenn die ID nicht gefunden wird
        }
        cursor.close();
        return ipAddress;
    }

    public String getWindTurbineName(int windTurbineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_WIND_TURBINE_NAME + " FROM " + TABLE_WIND_TURBINE + " WHERE " + COLUMN_WIND_TURBINE_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(windTurbineId)});
        String windTurbineName = null;
        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(COLUMN_WIND_TURBINE_NAME);
            if (columnIndex != -1) {
                windTurbineName = cursor.getString(columnIndex);
            } else {
                Log.e(TAG, "Spalte " + COLUMN_WIND_TURBINE_NAME + " nicht gefunden.");
                windTurbineName = "0";
            }
        } else {
            Log.d(TAG, "WindTurbine ID " + windTurbineId + " nicht gefunden.");
            windTurbineName = "0"; // Rückgabe "0" wenn die ID nicht gefunden wird
        }
        cursor.close();
        return windTurbineName;
    }


    public String getLocalTimestamp() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(calendar.getTimeZone()); // Setzt die Zeitzone des SimpleDateFormat auf die Zeitzone des Kalenders
        return dateFormat.format(calendar.getTime());
    }



}
