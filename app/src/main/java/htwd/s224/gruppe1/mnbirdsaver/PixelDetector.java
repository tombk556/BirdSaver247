package htwd.s224.gruppe1.mnbirdsaver;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;

public class PixelDetector {

    static class Coordinate {
        int x;
        int y;

        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "{\"x\": " + x + ", \"y\": " + y + "}";
        }
    }

    public static List<Coordinate> isPixelRed(Bitmap image) {
        List<Coordinate> redPixels = new ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = image.getPixel(x, y);
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                if (red > 200 && green < 50 && blue < 50) {
                    redPixels.add(new Coordinate(x, y));
                }
            }
        }

        if (!redPixels.isEmpty()) {
            int sumX = 0, sumY = 0;
            for (Coordinate coord : redPixels) {
                sumX += coord.x;
                sumY += coord.y;
            }
            int centerX = sumX / redPixels.size();
            int centerY = sumY / redPixels.size();
            List<Coordinate> center = new ArrayList<>();
            center.add(new Coordinate(centerX, centerY));
            return center;
        }
        return redPixels;
    }
}