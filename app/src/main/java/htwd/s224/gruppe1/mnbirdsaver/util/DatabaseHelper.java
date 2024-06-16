package htwd.s224.gruppe1.mnbirdsaver.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.graphics.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    // Database Name und Version
    private static final String DATABASE_NAME = "birdsaver.db";
    private static final int DATABASE_VERSION = 1;

    // Table name and columns for wind_turbine
    public static final String TABLE_WIND_TURBINE = "wind_turbine";
    public static final String COLUMN_WIND_TURBINE_ID = "id";
    public static final String COLUMN_WIND_TURBINE_NAME = "name";
    private static final String COLUMN_WIND_TURBINE_IP_ADDRESS = "ip_address";  // Neue Spalte

    // Table name and columns for measurement
    public static final String TABLE_MEASUREMENT = "measurement";
    public static final String COLUMN_MEASUREMENT_ID = "id";
    public static final String COLUMN_PIXEL_X = "pixel_x";
    public static final String COLUMN_PIXEL_Y = "pixel_y";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_LATITUDE = "latitude";

    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_WIND_TURBINE_ID_FK = "wind_turbine_id";

    // Table name and columns for matrix
    private static final String TABLE_MATRIX = "matrix_transform";

    private static final String COLUMN_MATRIX_DATA = "matrix_data";

    // Names of the Views
    public static final String VIEW_MEASUREMENT_WITH_WIND_TURBINE = "wind_turbine_measurement";
    public static final String VIEW_AVERAGE_COORDS = "wind_turbine_measurement_avg";


    // SharedPreferences
    private Context context;

    private static final String PREFS_NAME = "WindTurbinePrefs";
    private static final String CURRENT_WIND_TURBINE_ID = "CurrentWindTurbineId";

    private SharedPreferences sharedPreferences;

    // Strings for CREATING of all tables ----------------------------------------------------------

    //  Create wind_turbine table
    private static final String CREATE_TABLE_WIND_TURBINE = "CREATE TABLE " + TABLE_WIND_TURBINE + "("
            + COLUMN_WIND_TURBINE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_WIND_TURBINE_NAME + " TEXT NOT NULL, "
            + COLUMN_WIND_TURBINE_IP_ADDRESS + " TEXT NOT NULL" + ")";

    //  Create measurement table
    private static final String CREATE_TABLE_MEASUREMENT = "CREATE TABLE " + TABLE_MEASUREMENT + "("
            + COLUMN_MEASUREMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_PIXEL_X + " INTEGER NOT NULL, "
            + COLUMN_PIXEL_Y + " INTEGER NOT NULL, "
            + COLUMN_LONGITUDE + " DOUBLE NOT NULL, "
            + COLUMN_LATITUDE + " DOUBLE NOT NULL, "
            + COLUMN_WIND_TURBINE_ID_FK + " INTEGER NOT NULL, "
            + COLUMN_TIMESTAMP + " TEXT,"
            + "FOREIGN KEY(" + COLUMN_WIND_TURBINE_ID_FK + ") REFERENCES " + TABLE_WIND_TURBINE + "(" + COLUMN_WIND_TURBINE_ID + "));";


    //  Create matrix table (for Result of the Affine Transformation)
    private static final String CREATE_TABLE_MATRIX = "CREATE TABLE " + TABLE_MATRIX + "("
            + COLUMN_WIND_TURBINE_ID_FK + " INTEGER PRIMARY KEY, "
            + COLUMN_MATRIX_DATA + " TEXT NOT NULL, "
            + "FOREIGN KEY(" + COLUMN_WIND_TURBINE_ID_FK + ") REFERENCES " + TABLE_WIND_TURBINE + "(" + COLUMN_WIND_TURBINE_ID + "));";


    // Strings for CREATING of all Views -----------------------------------------------------------

    // Simple join of wind_turbine and measurement table
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

    // Join of of wind_turbine and measurement table, but if pixels have multiple GPS coordinates, we will get the average of it
    private static final String CREATE_AVERAGE_COORDS_VIEW = "CREATE VIEW " + VIEW_AVERAGE_COORDS + " AS " +
            "SELECT " +
            TABLE_MEASUREMENT + "." + COLUMN_WIND_TURBINE_ID_FK + " AS wind_turbine_id, " +
            TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_NAME + " AS wind_turbine_name, " +
            TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_IP_ADDRESS + " AS wind_turbine_ip_address, " +
            TABLE_MEASUREMENT + "." + COLUMN_PIXEL_X + " AS pixel_x, " +
            TABLE_MEASUREMENT + "." + COLUMN_PIXEL_Y + " AS pixel_y, " +
            "AVG(" + TABLE_MEASUREMENT + "." + COLUMN_LONGITUDE + ") AS longitude, " +
            "AVG(" + TABLE_MEASUREMENT + "." + COLUMN_LATITUDE + ") AS latitude " +
            "FROM " + TABLE_MEASUREMENT + " " +
            "JOIN " + TABLE_WIND_TURBINE + " ON " + TABLE_MEASUREMENT + "." + COLUMN_WIND_TURBINE_ID_FK + " = " + TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_ID + " " +
            "GROUP BY " + TABLE_MEASUREMENT + "." + COLUMN_PIXEL_X + ", " + TABLE_MEASUREMENT + "." + COLUMN_PIXEL_Y + ", " + TABLE_MEASUREMENT + "." + COLUMN_WIND_TURBINE_ID_FK + ";";


    // The DatabaseHelper Class --------------------------------------------------------------------
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); // for saving the last wind_turbine id, if new turbine is added
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create all tables
        Log.d(TAG, "Creating tables");
        db.execSQL(CREATE_TABLE_WIND_TURBINE);
        db.execSQL(CREATE_TABLE_MEASUREMENT);
        db.execSQL(CREATE_TABLE_MATRIX);

        // Create all views
        Log.d(TAG, "Creating view");
        db.execSQL(CREATE_VIEW_MEASUREMENT_WITH_WIND_TURBINE);
        db.execSQL(CREATE_AVERAGE_COORDS_VIEW);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        resetDatabase(db);
    }


    private void resetDatabase(SQLiteDatabase db) {
        Log.d(TAG, "Resetting database");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEASUREMENT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIND_TURBINE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MATRIX);

        db.execSQL("DROP VIEW IF EXISTS " + VIEW_MEASUREMENT_WITH_WIND_TURBINE);
        db.execSQL("DROP VIEW IF EXISTS " + VIEW_AVERAGE_COORDS);
        onCreate(db);

    }

    // Method for resetting the database
    public void resetDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        resetDatabase(db);
        db.close();
        saveCurrentWindTurbineId(0);
    }


    // WIND TURBINE --------------------------------------------------------------------------------

    // add new Wind Turbine and return the ID
    public long addWindTurbine(String name, String ipAddress) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WIND_TURBINE_NAME, name);
        values.put(COLUMN_WIND_TURBINE_IP_ADDRESS, ipAddress);

        long newId = db.insert(TABLE_WIND_TURBINE, null, values);
        if (newId == -1) {
            Log.e(TAG, "Wind turbine could not be inserted");
        } else {
            Log.d(TAG, "Wind turbine successfully inserted with ID: " + newId);
            saveCurrentWindTurbineId(newId);                            // save to sharedPreferences
        }
        db.close();
        return newId;
    }

    private void saveCurrentWindTurbineId(long windTurbineId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(CURRENT_WIND_TURBINE_ID, windTurbineId);
        editor.apply();
    }

    public long getCurrentWindTurbineId() {
        return sharedPreferences.getLong(CURRENT_WIND_TURBINE_ID, 0);
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
                Log.e(TAG, "Column " + COLUMN_WIND_TURBINE_IP_ADDRESS + " not found.");
                ipAddress = "0";
            }
        } else {
            Log.d(TAG, "WindTurbine ID " + windTurbineId + " not found.");
            ipAddress = "0";  // for error handling
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
                Log.e(TAG, "Column " + COLUMN_WIND_TURBINE_NAME + " not found.");
                windTurbineName = "0";
            }
        } else {
            Log.d(TAG, "WindTurbine ID " + windTurbineId + " not found.");
            windTurbineName = "0"; // for error handling
        }
        cursor.close();
        return windTurbineName;
    }


    @SuppressLint("Range")
    public List<WindTurbine> getAllWindTurbines() {
        List<WindTurbine> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WIND_TURBINE, new String[] {COLUMN_WIND_TURBINE_ID, COLUMN_WIND_TURBINE_NAME, COLUMN_WIND_TURBINE_IP_ADDRESS}, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(COLUMN_WIND_TURBINE_ID));
                String name = cursor.getString(cursor.getColumnIndex(COLUMN_WIND_TURBINE_NAME));
                String ipAddress = cursor.getString(cursor.getColumnIndex(COLUMN_WIND_TURBINE_IP_ADDRESS));
                WindTurbine turbine = new WindTurbine(id, name, ipAddress);
                list.add(turbine);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // MEASUREMENT ---------------------------------------------------------------------------------


    // add new measurement for certain wind turbine (fk)
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
            Log.e(TAG, "Measurement could not be inserted");
        } else {
            Log.d(TAG, "Measurement successfully inserted");
        }
        db.close();
    }

    public Cursor getAverageCoordsCursor(Integer windTurbineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (windTurbineId == null) {
            return db.rawQuery("SELECT * FROM " + VIEW_AVERAGE_COORDS, null);
        } else {
            return db.rawQuery("SELECT * FROM " + VIEW_AVERAGE_COORDS + " WHERE wind_turbine_id = ?", new String[]{String.valueOf(windTurbineId)});
        }
    }


    // MATRIX --------------------------------------------------------------------------------------

    // Method for inserting the matrix
    public void saveMatrixToDatabase(String matrixData, int windTurbineId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MATRIX_DATA, matrixData);
        values.put(COLUMN_WIND_TURBINE_ID_FK, windTurbineId);

        // Verwendung von insertWithOnConflict, um Konflikte zu handhaben
        long result = db.insertWithOnConflict(TABLE_MATRIX, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result == -1) {
            Log.e(TAG, "Matrix could not be inserted or updated");
        } else {
            Log.d(TAG, "Matrix successfully inserted or updated with WindTurbine ID: " + windTurbineId);
        }
        db.close();
    }

    public Cursor getMatrixCursor(Integer windTurbineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (windTurbineId == null) {
            // Retrieve all entries from the matrix table
            return db.rawQuery("SELECT * FROM " + TABLE_MATRIX, null);
        } else {
            // Retrieve specific entries from the matrix table based on the wind turbine ID
            return db.rawQuery("SELECT * FROM " + TABLE_MATRIX + " WHERE " + COLUMN_WIND_TURBINE_ID_FK + " = ?", new String[]{String.valueOf(windTurbineId)});
        }
    }

    // the Affine Transformation based on VIEW_AVERAGE_COORDS with windTurbineId
    // saves the Matrix to the Database
    public void getAffineTransformForWindTurbine(MatrixHelper MxTransformer, @NonNull Integer windTurbineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] selectionArgs = new String[]{windTurbineId.toString()};
        String query = "SELECT pixel_x, pixel_y, longitude, latitude FROM " + VIEW_AVERAGE_COORDS + " WHERE wind_turbine_id = ?";
        Cursor cursor = db.rawQuery(query, selectionArgs);


        if (cursor.getCount() < 4) {
            Log.e("DBHelper", "At least 4 points are required to compute an affine transform.");
            return;
        }

        float[] src = new float[8];
        float[] dst = new float[8];
        try {
            int pixelXIndex = cursor.getColumnIndex("pixel_x");
            int pixelYIndex = cursor.getColumnIndex("pixel_y");
            int longitudeIndex = cursor.getColumnIndex("longitude");
            int latitudeIndex = cursor.getColumnIndex("latitude");

            if (pixelXIndex == -1 || pixelYIndex == -1 || longitudeIndex == -1 || latitudeIndex == -1) {
                Log.e("DBHelper", "One or more columns are missing in the cursor.");
                return; // Early return to avoid further processing
            }

            if (cursor.moveToFirst()) {
                for (int i = 0; i < 4; i++) {
                    int pixelX = cursor.getInt(pixelXIndex);
                    int pixelY = cursor.getInt(pixelYIndex);
                    float longitude = cursor.getFloat(longitudeIndex);
                    float latitude = cursor.getFloat(latitudeIndex);
                    src[2 * i] = pixelX;
                    src[2 * i + 1] = pixelY;
                    dst[2 * i] = longitude;
                    dst[2 * i + 1] = latitude;
                    if (!cursor.moveToNext()) break;
                }
            }
        } finally {
            cursor.close();
        }
        Matrix matrix = new Matrix();
        matrix.setPolyToPoly(src, 0, dst, 0, 4);
        String matrixDataString = MxTransformer.serializeMatrix(matrix);
        saveMatrixToDatabase(matrixDataString, windTurbineId);                   // save to Database
    }


    public Matrix getMatrixByWindTurbineId(MatrixHelper MxTransformer, int windTurbineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_MATRIX_DATA + " FROM " + TABLE_MATRIX + " WHERE " + COLUMN_WIND_TURBINE_ID_FK + " = ?";
        Cursor cursor = null;
        Matrix matrix = null;
        try {
            cursor = db.rawQuery(query, new String[]{String.valueOf(windTurbineId)});
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(COLUMN_MATRIX_DATA);
                if (columnIndex != -1) {                          // Check whether the column exists
                    String matrixData = cursor.getString(columnIndex);
                    matrix = MxTransformer.deserializeMatrix(matrixData);
                } else {
                    Log.e(TAG, "Column " + COLUMN_MATRIX_DATA + " not found.");
                }
            } else {
                Log.d(TAG, "No matrix data for WindTurbine ID: " + windTurbineId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error when retrieving the matrix data: ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return matrix;
    }


}
