package htwd.s224.gruppe1.mnbirdsaver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.InputStream;


public class Home extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private ImageView imageView;
    private String ip_address;
    private Button toggleButton;  // Reference to the button
    private Handler handler = new Handler();

    private TextView textView;
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

        imageView = findViewById(R.id.view);
        textView = findViewById(R.id.gpsValue);

        requestLocationPermission();  // necessary for GPS

        try {
            // if user put in an ip address in ip_address_view, the input is fetched here,
            // if not the ip_address is undefined and the app will navigate to ip_address_view
            Intent intent = getIntent();
            ip_address = intent.getStringExtra("IPADDRESS");

            Log.d("CREATION", ip_address);

            ip_address.length(); // make sure exception is caused if no ip address is available

            toggleButton = findViewById(R.id.submitButton);
            toggleButton.setText("Start");

        } catch(NullPointerException e){
            // navigate to ip address view if no ip address is available
            Intent intent = new Intent(this, IpAddressActivity.class);
            startActivity(intent);
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Berechtigung bereits erteilt, eventuell direkt zur nächsten Activity wechseln
            Toast.makeText(this, "Standortberechtigung bereits erteilt.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Berechtigung wurde erteilt
                Toast.makeText(this, "Standortberechtigung erteilt.", Toast.LENGTH_LONG).show();
            } else {
                // Berechtigung wurde verweigert
                Toast.makeText(this, "Standortberechtigung wurden verweigert.", Toast.LENGTH_LONG).show();
            }
        }
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

    public void navigateToGPS(View view){
        Intent intent = new Intent(this, GPSActivity.class);
        startActivity(intent);
    }

    // use home button to navigate to camera view - here it goes back to itself
    // TODO replace hardcoded ip address / disable Button
    public void navigateToHome(View view){
        Log.d("CREATION", "nav");

        // navigate to ip home view
        Intent intent = new Intent(this, Home.class);
        intent.putExtra("IPADDRESS", "141.56.131.15"); // make sure value for ip address can be used in Home / camera_view
        startActivity(intent);
    }

}