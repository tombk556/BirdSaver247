package htwd.s224.gruppe1.mnbirdsaver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Calendar;

// Importiere den DatabaseHelper aus dem Unterpaket
import htwd.s224.gruppe1.mnbirdsaver.util.DatabaseHelper;

public class Home extends AppCompatActivity implements ImageFetcher.RedPixelCoordinatesListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;


    private ImageView imageView;
    private String ip_address;
    private Button toggleButton;
    private Handler handler = new Handler();

    private static final int PERMISSION_FINE_LOCATION = 99;

    TextView tv_gps, tv_timestamp, tv_name;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;

    DatabaseHelper databaseHelper;
    SQLiteDatabase db;

    private int lastWindTurbineId;

    ImageFetcher imageFetcher;
    private boolean isDownloading = false;
    private Runnable imageDownloader;

    private int redPixelX = -1;
    private int redPixelY = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);

        requestLocationPermission();

        imageView = findViewById(R.id.view);
        tv_gps = findViewById(R.id.gpsValue);
        tv_timestamp = findViewById(R.id.dateValue);
        tv_name = findViewById(R.id.name);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(10000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateUI_values(location);
                    insertData(location);
                }
            }
        };

        // DatabaseHelper initialisieren
        databaseHelper = new DatabaseHelper(this);

        // Datenbank öffnen
        db = databaseHelper.getWritableDatabase();

        // Datenbank zurücksetzen
        //resetDatabase();

        // Letzte WindTurbine_ID abrufen, Standardwert ist 0
        lastWindTurbineId = (int) databaseHelper.getLastWindTurbineId();

        if (lastWindTurbineId == 0) {
            Intent intent = new Intent(this, IpAddressActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        tv_name.setText(databaseHelper.getWindTurbineName(lastWindTurbineId));

        ip_address =  databaseHelper.getWindTurbineIpAddress(lastWindTurbineId);

        Toast.makeText(this, "ID: " + lastWindTurbineId, Toast.LENGTH_LONG).show();
        Toast.makeText(this, "IP: " + ip_address, Toast.LENGTH_LONG).show();

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
    }


    private void resetDatabase() {
        databaseHelper.resetDatabase();
        Toast.makeText(this, "Datenbank zurückgesetzt", Toast.LENGTH_LONG).show();
    }

    private void updateUI_values(Location location) {
        System.out.println("New Coordinates");
        String gps_coordinates = convertToDMS(location.getLatitude()) + "\n" + convertToDMS(location.getLongitude());
        tv_gps.setText(gps_coordinates);
        tv_timestamp.setText(Calendar.getInstance().getTime().toString());
    }

    private void insertData(Location location) {
        double gps_long= location.getLongitude();
        double gps_lat = location.getLatitude();

        // Add the red pixel coordinates to the database
        if (redPixelX != -1 && redPixelY != -1) {
            databaseHelper.addMeasurement(redPixelX, redPixelY, gps_long, gps_lat, lastWindTurbineId);
        }
    }

    @SuppressLint("DefaultLocale")
    private String convertToDMS(double decimalDegree) {
        int degree = (int) decimalDegree;
        double tempMinutes = (decimalDegree - degree) * 60;
        int minutes = (int) tempMinutes;
        double seconds = (tempMinutes - minutes) * 60;
        return String.format("%d° %d' %.6f\"", degree, minutes, seconds);
    }

    private void startLocationUpdates() {
        tv_gps.setText("Start");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
        }
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Standortberechtigung bereits erteilt.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Standortberechtigung erteilt.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Standortberechtigung wurden verweigert.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void startButtonClicked(View view) {
        Log.d("CameraView", "startButtonClicked() wurde aufgerufen");

        if (!isDownloading) {
            isDownloading = true;
            toggleButton.setText("Stop");
            handler.post(imageDownloader);
            startLocationUpdates();
        } else {
            isDownloading = false;
            toggleButton.setText("Start");
            handler.removeCallbacks(imageDownloader);
            stopLocationUpdates();
        }
    }

    public void navigateToGPS(View view) {
        Intent intent = new Intent(this, GPSActivity.class);
        startActivity(intent);
    }

    public void navigateToHome(View view) {
        Log.d("CREATION", "nav");

        Intent intent = new Intent(this, Home.class);
        intent.putExtra("IPADDRESS", "141.56.131.15");
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Datenbank schließen
        db.close();
    }

    @Override
    public void onRedPixelCoordinatesDetected(int x, int y) {
        redPixelX = x;
        redPixelY = y;
    }
}
