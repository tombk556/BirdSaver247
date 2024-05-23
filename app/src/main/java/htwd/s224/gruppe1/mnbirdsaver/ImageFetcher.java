package htwd.s224.gruppe1.mnbirdsaver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.List;
import java.util.Random;

public class ImageFetcher {
    private String ip_address;
    private ImageView imageView;
    private List<PixelDetector.Coordinate> redPixelCoordinates;
    private RedPixelCoordinatesListener listener;

    public interface RedPixelCoordinatesListener {
        void onRedPixelCoordinatesDetected(int x, int y);
    }

    public ImageFetcher(String ip_address, ImageView imageView, RedPixelCoordinatesListener listener) {
        this.ip_address = ip_address;
        this.imageView = imageView;
        this.listener = listener;
    }

    public void startFetching() {
        new DownloadImageTask().execute("http://" + ip_address + "/take_picture");
    }

    public List<PixelDetector.Coordinate> getRedPixelCoordinates() {
        return redPixelCoordinates;
    }

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
                result = addRandomDot(result);
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

        private Bitmap addRandomDot(Bitmap bitmap) {
            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.RED);  // Set the dot color
            paint.setStyle(Paint.Style.FILL);

            Random random = new Random();
            int x = random.nextInt(bitmap.getWidth());
            int y = random.nextInt(bitmap.getHeight());

            canvas.drawCircle(x, y, 10, paint);  // Draw a dot with radius 10

            return mutableBitmap;
        }
    }
}
