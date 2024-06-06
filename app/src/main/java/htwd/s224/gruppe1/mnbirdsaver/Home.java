package htwd.s224.gruppe1.mnbirdsaver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;

// Importiere den DatabaseHelper aus dem Unterpaket
import htwd.s224.gruppe1.mnbirdsaver.util.DatabaseHelper;

public class Home extends AppCompatActivity implements ImageFetcher.RedPixelCoordinatesListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;


    private ImageView imageView;
    private String ip_address;
    private Button toggleButton;
    private Handler handler = new Handler();
    TextView tv_name;
    LocationRequest locationRequest;

    DatabaseHelper databaseHelper;

    private int lastWindTurbineId;

    ImageFetcher imageFetcher;
    private boolean isDownloading = false;
    private Runnable imageDownloader;

    private int redPixelX = -1;
    private int redPixelY = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        requestLocationPermission();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageView = findViewById(R.id.view);
        tv_name = findViewById(R.id.name);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(10000)
                .build();

        // DatabaseHelper initialisieren
        databaseHelper = new DatabaseHelper(this);

        // Letzte WindTurbine_ID abrufen, Standardwert ist 0
        lastWindTurbineId = (int) databaseHelper.getCurrentWindTurbineId();

        if (lastWindTurbineId == 0) {
            Intent intent = new Intent(this, IpAddressActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        tv_name.setText(databaseHelper.getWindTurbineName(lastWindTurbineId));

        ip_address =  databaseHelper.getWindTurbineIpAddress(lastWindTurbineId);


        try {
            toggleButton = findViewById(R.id.submitButton);
            toggleButton.setText("Start");

            imageFetcher = new ImageFetcher(ip_address, imageView,this, true);
            imageDownloader = new Runnable() {
                @Override
                public void run() {
                    imageFetcher.startFetching();
                    handler.postDelayed(this, 2000);
                }
            };

        } catch (NullPointerException e) {
            Intent intent = new Intent(this, IpAddressActivity.class);
            startActivity(intent);
        }

        testTransformer();
    }

    // TODO replace double[][] arrays with data base calls
    public void testTransformer(){

        CoordinateTransform coordinateTransform = new CoordinateTransform();

        // Read the CSV file
        coordinateTransform.readData();

//        int testPixelX = 977;
//        int testPixelY = 482;
        int testPixelX = 284;
        int testPixelY = 296;

        float[] gpsCoords = coordinateTransform.pixelToGps(testPixelX, testPixelY);
        Log.d("CALCULATION: ", "Longitude: " + gpsCoords[0] + ", Latitude: " + gpsCoords[1]);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Standortberechtigung bereits erteilt.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Standortberechtigung erteilt.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Standortberechtigung wurden verweigert.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void startButtonClicked(View view) {
        if (!isDownloading) {
            isDownloading = true;
            toggleButton.setText("Stop");
            handler.post(imageDownloader);
        } else {
            isDownloading = false;
            toggleButton.setText("Start");
            handler.removeCallbacks(imageDownloader);
        }
    }

    public void continueButtonClicked(View view) {
        Intent intent = new Intent(this, CameraViewActivity.class);
        startActivity(intent);
    }

    public void navigateToHome(View view) {
        Intent intent = new Intent(this, Home.class);
        intent.putExtra("IPADDRESS", "141.56.131.15");
        startActivity(intent);
    }

    @Override
    public void onRedPixelCoordinatesDetected(int x, int y) {
        redPixelX = x;
        redPixelY = y;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, IpAddressActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.action_export) {
            exportData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportData() {
        // Implementiere die Logik zum Exportieren von Daten
        Toast.makeText(this, "Exporting data...", Toast.LENGTH_SHORT).show();
    }
}
