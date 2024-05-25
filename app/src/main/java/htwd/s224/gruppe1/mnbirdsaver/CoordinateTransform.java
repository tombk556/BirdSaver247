package htwd.s224.gruppe1.mnbirdsaver;

import android.util.Log;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;

public class CoordinateTransform {
    private RealMatrix transformMatrix;

    public CoordinateTransform(double[][] gpsCoordinates, double[][] pixelCoordinates) {
        int n = gpsCoordinates.length;
        // jede Zeile dieser beiden Arrays enthält ein GPS- oder Pixelkoordinatenpaar
        double[][] gpsMatrix = new double[n][3]; // neues Array mit n Zeilen & 3 Spalten => x,y, & letzte Spalte für 1 (für affine transformation)
        double[][] pixelMatrix = new double[n][2]; // neues Array mit n Zeilen & 2 Spateln => x,y - Pixel Koordinaten

        // loop durch Wertepaare & füllen mit bestehenden Werten aus DB
        for (int i = 0; i < n; i++) {
            gpsMatrix[i][0] = gpsCoordinates[i][0];
            gpsMatrix[i][1] = gpsCoordinates[i][1];
            gpsMatrix[i][2] = 1;  // Homogene Koordinaten für affine Transformation
            pixelMatrix[i][0] = pixelCoordinates[i][0];
            pixelMatrix[i][1] = pixelCoordinates[i][1];
        }

        RealMatrix gpsMat = new Array2DRowRealMatrix(gpsMatrix);
        RealMatrix pixelMat = new Array2DRowRealMatrix(pixelMatrix);

        // Berechnung der Transformationsmatrix
        DecompositionSolver solver = new LUDecomposition(gpsMat).getSolver(); // Hilfe, um Gleichungsssysteme zu lösen (Gleichungssystem: gpsMat * transformmatrix = pixelMat)
        transformMatrix = solver.solve(pixelMat); // matrix wird dann als Multiplikationsfaktor benutzt, um Pixel- in GPS Werte umzurechnen & anders rum
    }

    public double[] gpsToPixel(double lat, double lon) {
        double[][] coord = {{lat, lon, 1}};
        RealMatrix coordMatrix = new Array2DRowRealMatrix(coord);
        RealMatrix result = coordMatrix.multiply(transformMatrix);

        return new double[]{result.getEntry(0, 0), result.getEntry(0, 1)};
    }

    // TODO fix this method
    public double[] pixelToGps(double x, double y) {
        try {
            double[][] coord = {{x, y, 1}};
            RealMatrix coordMatrix = new Array2DRowRealMatrix(coord);
            DecompositionSolver solver = new LUDecomposition(transformMatrix).getSolver();
            RealMatrix inverseTransform = solver.getInverse();
            RealMatrix result = coordMatrix.multiply(inverseTransform);
            return new double[]{result.getEntry(0, 0), result.getEntry(0, 1)}; // gibt array mit zwei Werten zurück
        } catch (Exception e) {
            Log.e("ERROR:)", "Error in pixelToGps: " + e.getMessage());
            return new double[]{0, 0}; // Rückgabe eines Standardwerts oder einer anderen geeigneten Fehlerbehandlung
        }
    }

    public static void main(String[] args) {
        // TODO: Werte aus DB in gpsCoordinates & pixelCoordinates
        double[][] gpsCoordinates = {
                {37.7749, -122.4194},
                {34.0522, -118.2437},
                {40.7128, -74.0060},
                // Weitere GPS-Koordinaten hier hinzufügen
        };

        double[][] pixelCoordinates = {
                {512, 256},
                {600, 300},
                {800, 400},
                // Entsprechende Pixelkoordinaten hier hinzufügen
        };

        CoordinateTransform transformer = new CoordinateTransform(gpsCoordinates, pixelCoordinates);

        // Beispiel GPS-Koordinaten
        double lat = 37.7749;
        double lon = -122.4194;

        // Umrechnung von GPS zu Pixel
        double[] pixel = transformer.gpsToPixel(lat, lon);
        Log.d("CALCULATION 1: ", "Pixel: x=" + pixel[0] + ", y=" + pixel[1]);

        // Beispiel Pixelkoordinaten für Umrechnung zurück zu GPS
        double x = 512;
        double y = 256;

        // Umrechnung von Pixel zu GPS
        double[] gps = transformer.pixelToGps(x, y);
        Log.d("CALCULATION 2: ", "GPS: lat=" + gps[0] + ", lon=" + gps[1]);
    }
}
