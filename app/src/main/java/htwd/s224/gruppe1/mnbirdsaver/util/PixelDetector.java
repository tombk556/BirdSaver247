/**
 * Utility class for detecting red pixels in an image.
 */
package htwd.s224.gruppe1.mnbirdsaver.util;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;

public class PixelDetector {

    /**
     * Represents a coordinate with x and y values.
     */
    static class Coordinate {
        int x;
        int y;

        /**
         * Constructs a Coordinate with the specified x and y values.
         *
         * @param x The x-coordinate.
         * @param y The y-coordinate.
         */
        private Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Returns a string representation of the Coordinate in JSON format.
         *
         * @return A string representing the coordinate in JSON format.
         */
        @Override
        public String toString() {
            return "{\"x\": " + this.x + ", \"y\": " + this.y + "}";
        }

        /**
         * Returns the x-coordinate.
         *
         * @return The x-coordinate.
         */
        public int getX() {
            return this.x;
        }

        /**
         * Returns the y-coordinate.
         *
         * @return The y-coordinate.
         */
        public int getY() {
            return this.y;
        }
    }

    /**
     * Detects red pixels in the given Bitmap image.
     *
     * Red pixels are defined as those with red values greater than 200,
     * green values less than 50, and blue values less than 50.
     *
     * @param image The Bitmap image to be analyzed.
     * @return A list of Coordinates of the red pixels found in the image. If red pixels are found,
     *         the list will contain a single Coordinate representing the center of the red pixels.
     *         If no red pixels are found, the list will be empty.
     */
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
                // Adjust the color settings for pixel recognition, here: red
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
