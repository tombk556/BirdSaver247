package htwd.s224.gruppe1.mnbirdsaver.util;

import android.graphics.Matrix;
import java.util.ArrayList;
import java.util.List;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

public class CoordinateTransform {

    static class CoordinatePair {
        int pixelX, pixelY;
        float longitude, latitude;

        CoordinatePair(int pixelX, int pixelY, float longitude, float latitude) {
            this.pixelX = pixelX;
            this.pixelY = pixelY;
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }

    private DatabaseHelper dbHelper; // Angenommen, DatabaseHelper ist deine Klasse für Datenbankoperationen
    private int windTurbineId;

    public CoordinateTransform(DatabaseHelper dbHelper, int windTurbineId) {
        this.dbHelper = dbHelper;
        this.windTurbineId = windTurbineId;
    }

    private List<CoordinatePair> fetchCoordinatePairs() {
        List<CoordinatePair> list = new ArrayList<>();
        try (Cursor cursor = dbHelper.getAverageCoordsCursor(this.windTurbineId)) {
            int pixelXIndex = cursor.getColumnIndex("pixel_x");
            int pixelYIndex = cursor.getColumnIndex("pixel_y");
            int longitudeIndex = cursor.getColumnIndex("longitude");
            int latitudeIndex = cursor.getColumnIndex("latitude");

            if (pixelXIndex == -1 || pixelYIndex == -1 || longitudeIndex == -1 || latitudeIndex == -1) {
                Log.e("CoordinateTransform", "One or more columns are missing in the cursor.");

                Log.e("pixelXIndex", String.valueOf(pixelXIndex));
                Log.e("pixelYIndex", String.valueOf(pixelYIndex));
                Log.e("longitudeIndex", String.valueOf(longitudeIndex));
                Log.e("latitudeIndex", String.valueOf(latitudeIndex));
                return list;  // Early return to avoid further processing
            }

            if (cursor.moveToFirst()) {
                do {
                    int pixelX = cursor.getInt(pixelXIndex);
                    int pixelY = cursor.getInt(pixelYIndex);
                    float longitude = cursor.getFloat(longitudeIndex);
                    float latitude = cursor.getFloat(latitudeIndex);
                    list.add(new CoordinatePair(pixelX, pixelY, longitude, latitude));
                } while (cursor.moveToNext());
            }
        }
        return list;
    }


    private Matrix computeAffineTransform(List<CoordinatePair> coordinates) {
        if (coordinates.size() < 3) {
            throw new IllegalArgumentException("At least 3 points are required to compute an affine transform.");
        }

        float[] src = new float[6];
        float[] dst = new float[6];

        for (int i = 0; i < 3; i++) {
            CoordinatePair pair = coordinates.get(i);
            src[2 * i] = pair.pixelX;
            src[2 * i + 1] = pair.pixelY;
            dst[2 * i] = pair.longitude;
            dst[2 * i + 1] = pair.latitude;
        }

        Matrix matrix = new Matrix();
        matrix.setPolyToPoly(src, 0, dst, 0, 3);
        return matrix;
    }

    public float[] pixelToGps(int pixelX, int pixelY) {
        List<CoordinatePair> coordinates = fetchCoordinatePairs();
        Matrix transform = computeAffineTransform(coordinates);
        float[] src = {pixelX, pixelY};
        float[] dst = new float[2];
        transform.mapPoints(dst, src);

        return new float[]{dst[0], dst[1]};
    }
}
