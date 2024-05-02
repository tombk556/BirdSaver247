package htwd.s224.gruppe1.mnbirdsaver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;

public class CameraView extends AppCompatActivity {

    private ImageView imageView;
    private String ip_address;
    private Button toggleButton;  // Reference to the button
    private Handler handler = new Handler();
    private boolean isDownloading = false;
    private Runnable imageDownloader = new Runnable() {
        @Override
        public void run() {
            new DownloadImageTask().execute("http://" + ip_address + "/take_picture");
            handler.postDelayed(this, 2000); // schedule next download in 2 seconds
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);

        Intent intent = getIntent();
        ip_address = intent.getStringExtra("IPADRRESS");

        Button button = findViewById(R.id.button);
        button.setEnabled(false);

        imageView = findViewById(R.id.view);

        toggleButton = findViewById(R.id.submitButton);  // Assuming the button's ID is submitButton
        toggleButton.setText("Start");  // Set initial text to "Start"
    }

    public void startButtonClicked(View view){
        Log.d("CameraView", "startButtonClicked() wurde aufgerufen"); // Log-Nachricht hinzufügen

        if (!isDownloading) {
            isDownloading = true;
            toggleButton.setText("Stop");
            handler.post(imageDownloader); // start downloading
        } else {
            isDownloading = false;
            toggleButton.setText("Start");
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
