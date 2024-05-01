package htwd.s224.gruppe1.mnbirdsaver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;

public class IpAddressView extends AppCompatActivity {

    private ImageView imageView;
    private EditText editText;
    private Handler handler = new Handler();
    private boolean isDownloading = false;
    private Runnable imageDownloader = new Runnable() {
        @Override
        public void run() {
            String ipAddress = editText.getText().toString();
            new DownloadImageTask().execute("http://" + ipAddress + "/take_picture");
            handler.postDelayed(this, 2000); // schedule next download in 2 seconds
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ip_address_view);
        imageView = findViewById(R.id.imageView3);
        editText = findViewById(R.id.editText);
    }

    public void submitIpAddress(View view) {
        if (!isDownloading) {
            isDownloading = true;
            handler.post(imageDownloader); // start downloading
        } else {
            isDownloading = false;
            handler.removeCallbacks(imageDownloader); // stop downloading
        }
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

        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }
}
