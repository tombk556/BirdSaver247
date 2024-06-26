/**
 * Class for fetching images from a specified IP address and detecting red pixels within the images.
 * Optionally, it can add an arc dot to the images for simulation purposes.
 */
package htwd.s224.gruppe1.mnbirdsaver.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageFetcher {
    private String ip_address;
    private ImageView imageView;
    private List<PixelDetector.Coordinate> redPixelCoordinates;
    private RedPixelCoordinatesListener listener;

    private boolean includeArcDot;
    private float currentAngle = 0.0f; // Start angle in degrees
    private static final float ANGLE_INCREMENT = 10.0f; // Angle increment in degrees

    /**
     * Listener interface for red pixel coordinates detection.
     */
    public interface RedPixelCoordinatesListener {
        /**
         * Called when red pixel coordinates are detected.
         *
         * @param x The x-coordinate of the red pixel.
         * @param y The y-coordinate of the red pixel.
         */
        void onRedPixelCoordinatesDetected(int x, int y);
    }

    /**
     * Constructs an ImageFetcher with the specified IP address, ImageView, listener, and arc dot inclusion flag.
     *
     * @param ip_address The IP address from which to fetch the image.
     * @param imageView The ImageView in which to display the fetched image.
     * @param listener The listener for red pixel coordinates detection.
     * @param includeArcDot A flag indicating whether to include an arc dot in the image.
     */
    public ImageFetcher(String ip_address, ImageView imageView, RedPixelCoordinatesListener listener, boolean includeArcDot) {
        this.ip_address = ip_address;
        this.imageView = imageView;
        this.listener = listener;
        this.includeArcDot = includeArcDot;
        this.redPixelCoordinates = new ArrayList<>();
    }

    /**
     * Starts the process of fetching an image from the specified IP address.
     */
    public void startFetching() {
        new DownloadImageTask().execute("http://" + ip_address + "/take_picture");
    }

    /**
     * Increments the arc dot's angle.
     */
    public void increment_arc_dot() {
        currentAngle += ANGLE_INCREMENT;
    }

    /**
     * Returns the list of red pixel coordinates detected in the image.
     *
     * @return A list of red pixel coordinates.
     */
    public List<PixelDetector.Coordinate> getRedPixelCoordinates() {
        return redPixelCoordinates;
    }

    /**
     * AsyncTask for downloading an image from a URL in the background.
     */
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {
            String urlDisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urlDisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                if (includeArcDot) {
                    result = addArcDot(result);
                }
                imageView.setImageBitmap(result);
                redPixelCoordinates = PixelDetector.isPixelRed(result);
                if (!redPixelCoordinates.isEmpty()) {
                    PixelDetector.Coordinate coord = redPixelCoordinates.get(0);
                    int x = coord.getX();
                    int y = coord.getY();
                    Log.d("X: " + x, "X");
                    Log.d("RedPixelLocation", coord.toString());

                    if (listener != null) {
                        listener.onRedPixelCoordinatesDetected(x, y);
                    }
                }
            }
        }

        /**
         * Adds an arc dot to the specified Bitmap image.
         *
         * @param bitmap The Bitmap image to which the arc dot will be added.
         * @return The Bitmap image with the arc dot added.
         */
        private Bitmap addArcDot(Bitmap bitmap) {
            int imageWidth = bitmap.getWidth();
            int imageHeight = bitmap.getHeight();

            float ARC_CENTER_X = imageWidth / 2.0f; // Center X of the image
            float ARC_CENTER_Y = imageHeight / 2.0f; // Center Y of the image
            float ARC_RADIUS = Math.min(imageWidth, imageHeight) / 2.0f; // Adjust radius to fit the image

            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.RED);  // Set the dot color
            paint.setStyle(Paint.Style.FILL);

            // Calculate the new dot position in arc format
            float radians = (float) Math.toRadians(currentAngle);
            int x = (int) (ARC_CENTER_X + ARC_RADIUS * Math.cos(radians));
            int y = (int) (ARC_CENTER_Y + ARC_RADIUS * Math.sin(radians));

            canvas.drawCircle(x, y, 10, paint);  // Draw a dot with radius 10

            return mutableBitmap;
        }
    }
}
