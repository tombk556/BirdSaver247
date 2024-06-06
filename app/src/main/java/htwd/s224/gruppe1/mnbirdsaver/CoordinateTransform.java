package htwd.s224.gruppe1.mnbirdsaver;

import android.graphics.Matrix;
import java.util.ArrayList;
import java.util.List;

public class CoordinateTransform {

    // Inner class to hold pixel and GPS coordinates
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

    // List of known coordinate pairs
    private static final List<CoordinatePair> coordinates = new ArrayList<>();

    // Method to read data and populate coordinates list
    public static void readData() {
        coordinates.add(new CoordinatePair(279,358,13.7617f,51.0534f));
        coordinates.add(new CoordinatePair(576,712,13.7614f,51.0538f));
        coordinates.add(new CoordinatePair(999,359,13.7607f,51.0537f));
    }

    // Method to compute the affine transformation matrix
    private static Matrix computeAffineTransform() {
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

    // Method to convert pixel coordinates to GPS coordinates using the computed affine transformation
    public float[] pixelToGps(int pixelX, int pixelY) {
        Matrix transform = computeAffineTransform();
        float[] src = {pixelX, pixelY};
        float[] dst = new float[2];
        transform.mapPoints(dst, src);

        return new float[]{dst[0], dst[1]};
    }
}
