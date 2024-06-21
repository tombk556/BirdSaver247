/**
 * Helper class for managing the SQLite database used in the Bird Saver application.
 * This class handles the creation, updating, and querying of database tables and views.
 */
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

    // Database Name and Version
    private static final String DATABASE_NAME = "birdsaver.db";
    private static final int DATABASE_VERSION = 1;

    // Table names and columns
    private static final String TABLE_WIND_TURBINE = "wind_turbine";
    private static final String COLUMN_WIND_TURBINE_ID = "id";
    private static final String COLUMN_WIND_TURBINE_NAME = "name";
    private static final String COLUMN_WIND_TURBINE_IP_ADDRESS = "ip_address";

    private static final String TABLE_MEASUREMENT = "measurement";
    private static final String COLUMN_MEASUREMENT_ID = "id";
    private static final String COLUMN_PIXEL_X = "pixel_x";
    private static final String COLUMN_PIXEL_Y = "pixel_y";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_WIND_TURBINE_ID_FK = "wind_turbine_id";

    private static final String TABLE_MATRIX = "matrix_transform";
    private static final String COLUMN_MATRIX_DATA = "matrix_data";

    // View names
    private static final String VIEW_MEASUREMENT_WITH_WIND_TURBINE = "wind_turbine_measurement";
    private static final String VIEW_AVERAGE_COORDS = "wind_turbine_measurement_avg";

    // SharedPreferences
    private static final String PREFS_NAME = "WindTurbinePrefs";
    private static final String CURRENT_WIND_TURBINE_ID = "CurrentWindTurbineId";

    private SharedPreferences sharedPreferences;
    private Context context;

    // SQL statements for creating tables and views
    private static final String CREATE_TABLE_WIND_TURBINE = "CREATE TABLE " + TABLE_WIND_TURBINE + "("
            + COLUMN_WIND_TURBINE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_WIND_TURBINE_NAME + " TEXT NOT NULL, "
            + COLUMN_WIND_TURBINE_IP_ADDRESS + " TEXT NOT NULL)";

    private static final String CREATE_TABLE_MEASUREMENT = "CREATE TABLE " + TABLE_MEASUREMENT + "("
            + COLUMN_MEASUREMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_PIXEL_X + " INTEGER NOT NULL, "
            + COLUMN_PIXEL_Y + " INTEGER NOT NULL, "
            + COLUMN_LONGITUDE + " DOUBLE NOT NULL, "
            + COLUMN_LATITUDE + " DOUBLE NOT NULL, "
            + COLUMN_WIND_TURBINE_ID_FK + " INTEGER NOT NULL, "
            + COLUMN_TIMESTAMP + " TEXT, "
            + "FOREIGN KEY(" + COLUMN_WIND_TURBINE_ID_FK + ") REFERENCES " + TABLE_WIND_TURBINE + "(" + COLUMN_WIND_TURBINE_ID + "))";

    private static final String CREATE_TABLE_MATRIX = "CREATE TABLE " + TABLE_MATRIX + "("
            + COLUMN_WIND_TURBINE_ID_FK + " INTEGER PRIMARY KEY, "
            + COLUMN_MATRIX_DATA + " TEXT NOT NULL, "
            + "FOREIGN KEY(" + COLUMN_WIND_TURBINE_ID_FK + ") REFERENCES " + TABLE_WIND_TURBINE + "(" + COLUMN_WIND_TURBINE_ID + "))";

    private static final String CREATE_VIEW_MEASUREMENT_WITH_WIND_TURBINE = "CREATE VIEW " + VIEW_MEASUREMENT_WITH_WIND_TURBINE + " AS "
            + "SELECT "
            + TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_ID + " AS wind_turbine_id, "
            + TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_NAME + " AS wind_turbine_name, "
            + TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_IP_ADDRESS + " AS wind_turbine_ip_address, "
            + TABLE_MEASUREMENT + "." + COLUMN_MEASUREMENT_ID + " AS measurement_id, "
            + TABLE_MEASUREMENT + "." + COLUMN_PIXEL_X + " AS pixel_x, "
            + TABLE_MEASUREMENT + "." + COLUMN_PIXEL_Y + " AS pixel_y, "
            + TABLE_MEASUREMENT + "." + COLUMN_LONGITUDE + " AS longitude, "
            + TABLE_MEASUREMENT + "." + COLUMN_LATITUDE + " AS latitude "
            + "FROM " + TABLE_MEASUREMENT + " "
            + "JOIN " + TABLE_WIND_TURBINE + " ON " + TABLE_MEASUREMENT + "." + COLUMN_WIND_TURBINE_ID_FK + " = " + TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_ID;

    private static final String CREATE_AVERAGE_COORDS_VIEW = "CREATE VIEW " + VIEW_AVERAGE_COORDS + " AS "
            + "SELECT "
            + TABLE_MEASUREMENT + "." + COLUMN_MEASUREMENT_ID + " AS measurement_id, "
            + TABLE_MEASUREMENT + "." + COLUMN_WIND_TURBINE_ID_FK + " AS wind_turbine_id, "
            + TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_NAME + " AS wind_turbine_name, "
            + TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_IP_ADDRESS + " AS wind_turbine_ip_address, "
            + TABLE_MEASUREMENT + "." + COLUMN_PIXEL_X + " AS pixel_x, "
            + TABLE_MEASUREMENT + "." + COLUMN_PIXEL_Y + " AS pixel_y, "
            + "AVG(" + TABLE_MEASUREMENT + "." + COLUMN_LONGITUDE + ") AS longitude, "
            + "AVG(" + TABLE_MEASUREMENT + "." + COLUMN_LATITUDE + ") AS latitude "
            + "FROM " + TABLE_MEASUREMENT + " "
            + "JOIN " + TABLE_WIND_TURBINE + " ON " + TABLE_MEASUREMENT + "." + COLUMN_WIND_TURBINE_ID_FK + " = " + TABLE_WIND_TURBINE + "." + COLUMN_WIND_TURBINE_ID + " "
            + "GROUP BY " + TABLE_MEASUREMENT + "." + COLUMN_WIND_TURBINE_ID_FK + ", " + TABLE_MEASUREMENT + "." + COLUMN_PIXEL_X + ", " + TABLE_MEASUREMENT + "." + COLUMN_PIXEL_Y + " "
            + "ORDER BY " + TABLE_MEASUREMENT + "." + COLUMN_WIND_TURBINE_ID_FK;

    /**
     * Constructs a new DatabaseHelper with the specified context.
     *
     * @param context The context to use for locating paths to the database.
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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

    /**
     * Resets the database by dropping all tables and views and recreating them.
     *
     * @param db The database to reset.
     */
    private void resetDatabase(SQLiteDatabase db) {
        Log.d(TAG, "Resetting database");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEASUREMENT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIND_TURBINE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MATRIX);

        db.execSQL("DROP VIEW IF EXISTS " + VIEW_MEASUREMENT_WITH_WIND_TURBINE);
        db.execSQL("DROP VIEW IF EXISTS " + VIEW_AVERAGE_COORDS);
        onCreate(db);
    }

    /**
     * Resets the matrix table and the average coordinates view.
     */
    public void reset_matrix() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP VIEW IF EXISTS " + VIEW_AVERAGE_COORDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MATRIX);
        db.execSQL(CREATE_TABLE_MATRIX);
        db.execSQL(CREATE_AVERAGE_COORDS_VIEW);
    }

    /**
     * Resets the database and saves the current wind turbine ID to 0.
     */
    public void resetDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        resetDatabase(db);
        db.close();
        saveCurrentWindTurbineId(0);
    }

    // WIND TURBINE --------------------------------------------------------------------------------

    /**
     * Adds a new wind turbine to the database.
     *
     * @param name The name of the wind turbine.
     * @param ipAddress The IP address of the wind turbine.
     * @return The ID of the newly added wind turbine.
     */
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
            saveCurrentWindTurbineId(newId);  // save to sharedPreferences
        }
        db.close();
        return newId;
    }

    /**
     * Saves the current wind turbine ID to shared preferences.
     *
     * @param windTurbineId The ID of the wind turbine to save.
     */
    private void saveCurrentWindTurbineId(long windTurbineId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(CURRENT_WIND_TURBINE_ID, windTurbineId);
        editor.apply();
    }

    /**
     * Retrieves the current wind turbine ID from shared preferences.
     *
     * @return The current wind turbine ID.
     */
    public long getCurrentWindTurbineId() {
        return sharedPreferences.getLong(CURRENT_WIND_TURBINE_ID, 0);
    }

    /**
     * Retrieves the IP address of a wind turbine by its ID.
     *
     * @param windTurbineId The ID of the wind turbine.
     * @return The IP address of the wind turbine.
     */
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

    /**
     * Retrieves the name of a wind turbine by its ID.
     *
     * @param windTurbineId The ID of the wind turbine.
     * @return The name of the wind turbine.
     */
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

    /**
     * Retrieves a list of all wind turbines in the database.
     *
     * @return A list of all wind turbines.
     */
    @SuppressLint("Range")
    public List<WindTurbine> getAllWindTurbines() {
        List<WindTurbine> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WIND_TURBINE, new String[]{COLUMN_WIND_TURBINE_ID, COLUMN_WIND_TURBINE_NAME, COLUMN_WIND_TURBINE_IP_ADDRESS}, null, null, null, null, null);
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

    /**
     * Adds a new measurement for a specific wind turbine.
     *
     * @param pixelX The x-coordinate of the pixel.
     * @param pixelY The y-coordinate of the pixel.
     * @param longitude The longitude coordinate.
     * @param latitude The latitude coordinate.
     * @param windTurbineId The ID of the associated wind turbine.
     * @param timestamp The timestamp of the measurement.
     */
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

    /**
     * Retrieves a cursor for the average coordinates view filtered by the wind turbine ID.
     *
     * @param windTurbineId The ID of the wind turbine to filter by, or null to retrieve all records.
     * @return A cursor for the average coordinates view.
     */
    public Cursor getAverageCoordsCursor(Integer windTurbineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (windTurbineId == null) {
            return db.rawQuery("SELECT * FROM " + VIEW_AVERAGE_COORDS, null);
        } else {
            return db.rawQuery("SELECT * FROM " + VIEW_AVERAGE_COORDS + " WHERE wind_turbine_id = ?", new String[]{String.valueOf(windTurbineId)});
        }
    }

    /**
     * Retrieves a cursor for the filtered average coordinates view based on the specified wind turbine ID.
     *
     * @param windTurbineId The ID of the wind turbine to filter by.
     * @return A cursor for the filtered average coordinates view.
     */
    public Cursor getFilteredAverageCoordsCursor(Integer windTurbineId) {
        SQLiteDatabase db = this.getReadableDatabase();

        String avgLongitudeQuery = "SELECT wind_turbine_id, AVG(longitude) AS avg_longitude FROM " + VIEW_AVERAGE_COORDS + " GROUP BY wind_turbine_id";
        String avgLatitudeQuery = "SELECT wind_turbine_id, AVG(latitude) AS avg_latitude FROM " + VIEW_AVERAGE_COORDS + " GROUP BY wind_turbine_id";

        String mainQuery = "WITH OrderedData AS (" +
                "SELECT M.wind_turbine_id, M.pixel_x, M.pixel_y, M.longitude, Lo.avg_longitude, M.latitude, La.avg_latitude, " +
                "LAG(M.longitude, 1, M.longitude) OVER (PARTITION BY M.wind_turbine_id) AS prev_longitude, " +
                "LAG(M.latitude, 1, M.latitude) OVER (PARTITION BY M.wind_turbine_id) AS prev_latitude " +
                "FROM " + VIEW_AVERAGE_COORDS + " AS M " +
                "INNER JOIN (" + avgLongitudeQuery + ") AS Lo ON M.wind_turbine_id = Lo.wind_turbine_id " +
                "INNER JOIN (" + avgLatitudeQuery + ") AS La ON M.wind_turbine_id = La.wind_turbine_id " +
                "WHERE M.wind_turbine_id = ? AND " +
                "ABS(M.latitude - La.avg_latitude) >= 0.00002 AND " +
                "ABS(M.longitude - Lo.avg_longitude) >= 0.00002 " +
                ") " +
                "SELECT wind_turbine_id, pixel_x, pixel_y, longitude, avg_longitude, latitude, avg_latitude, prev_longitude, prev_latitude " +
                "FROM OrderedData " +
                "WHERE (ABS(longitude - prev_longitude) >= 0.0001 AND ABS(latitude - prev_latitude) >= 0.00002) "+
                "OR (ABS(longitude - prev_longitude) >= 0.0002 AND ABS(latitude - prev_latitude) >= 0.00001)";

        if (windTurbineId != null) {
            return db.rawQuery(mainQuery, new String[]{String.valueOf(windTurbineId)});
        } else {
            return db.rawQuery(mainQuery, new String[]{"M.wind_turbine_id"});
        }
    }

    // MATRIX --------------------------------------------------------------------------------------

    /**
     * Saves the matrix data to the database for a specific wind turbine.
     *
     * @param matrixData The matrix data to save.
     * @param windTurbineId The ID of the associated wind turbine.
     */
    public void saveMatrixToDatabase(String matrixData, int windTurbineId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MATRIX_DATA, matrixData);
        values.put(COLUMN_WIND_TURBINE_ID_FK, windTurbineId);

        long result = db.insertWithOnConflict(TABLE_MATRIX, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result == -1) {
            Log.e(TAG, "Matrix could not be inserted or updated");
        } else {
            Log.d(TAG, "Matrix successfully inserted or updated with WindTurbine ID: " + windTurbineId);
        }
        db.close();
    }

    /**
     * Retrieves a cursor for the matrix data filtered by the wind turbine ID.
     *
     * @param windTurbineId The ID of the wind turbine to filter by, or null to retrieve all records.
     * @return A cursor for the matrix data.
     */
    public Cursor getMatrixCursor(Integer windTurbineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (windTurbineId == null) {
            return db.rawQuery("SELECT * FROM " + TABLE_MATRIX, null);
        } else {
            return db.rawQuery("SELECT * FROM " + TABLE_MATRIX + " WHERE " + COLUMN_WIND_TURBINE_ID_FK + " = ?", new String[]{String.valueOf(windTurbineId)});
        }
    }

    /**
     * Computes the affine transformation for a specific wind turbine and saves the result to the database.
     *
     * @param MxTransformer The MatrixHelper instance used for serialization.
     * @param windTurbineId The ID of the wind turbine.
     */
    public void getAffineTransformForWindTurbine(MatrixHelper MxTransformer, @NonNull Integer windTurbineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = getFilteredAverageCoordsCursor(windTurbineId);

        if (cursor.getCount() < 4) {
            Log.e("DBHelper", "Four points are required to compute this affine transformation.");
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
        saveMatrixToDatabase(matrixDataString, windTurbineId);  // save to Database
    }

    /**
     * Retrieves the matrix for a specific wind turbine from the database.
     *
     * @param MxTransformer The MatrixHelper instance used for deserialization.
     * @param windTurbineId The ID of the wind turbine.
     * @return The matrix for the specified wind turbine.
     */
    public Matrix getMatrixByWindTurbineId(MatrixHelper MxTransformer, int windTurbineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_MATRIX_DATA + " FROM " + TABLE_MATRIX + " WHERE " + COLUMN_WIND_TURBINE_ID_FK + " = ?";
        Cursor cursor = null;
        Matrix matrix = null;
        try {
            cursor = db.rawQuery(query, new String[]{String.valueOf(windTurbineId)});
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(COLUMN_MATRIX_DATA);
                if (columnIndex != -1) {  // Check whether the column exists
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
