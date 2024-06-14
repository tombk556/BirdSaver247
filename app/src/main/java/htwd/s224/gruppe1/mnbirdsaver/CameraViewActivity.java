package htwd.s224.gruppe1.mnbirdsaver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Calendar;

import htwd.s224.gruppe1.mnbirdsaver.legacy.GPSActivity;
import htwd.s224.gruppe1.mnbirdsaver.util.DatabaseHelper;
import htwd.s224.gruppe1.mnbirdsaver.util.ExportCSVHelper;
import htwd.s224.gruppe1.mnbirdsaver.util.ImageFetcher;

public class CameraViewActivity extends AppCompatActivity implements ImageFetcher.RedPixelCoordinatesListener {
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
    ExportCSVHelper exportCSVHelper;
    SQLiteDatabase db;

    private int currentWindTurbineId;
    private String currentWindTurbineName;

    ImageFetcher imageFetcher;
    private boolean isDownloading = false;
    private Runnable imageDownloader;

    private int redPixelX = -1;
    private int redPixelY = -1;
    private boolean includeArcDot;
    private SwitchCompat simOnOffSwitch;

    private int locationCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        imageView = findViewById(R.id.view);
        tv_gps = findViewById(R.id.gpsValue);
        tv_timestamp = findViewById(R.id.dateValue);
        tv_name = findViewById(R.id.name);
        simOnOffSwitch = findViewById(R.id.SimOnOff);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(10000)
                .build();


        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateUI_values(location);
                    insertData(location);

                    locationCount++; // after 10 values the calculation should stop
                    if (locationCount >= 10) { // TODO check if 10 values are enough
                        updateInstructionText();
                    }
                }
            }
        };

        // DatabaseHelper initialisieren
        databaseHelper = new DatabaseHelper(this);
        exportCSVHelper = new ExportCSVHelper(this, databaseHelper);

        // Datenbank öffnen
        db = databaseHelper.getWritableDatabase();



        // Letzte WindTurbine_ID abrufen, Standardwert ist 0
        currentWindTurbineId = (int) databaseHelper.getCurrentWindTurbineId();

        if (currentWindTurbineId == 0) {
            Intent intent = new Intent(this, IpAddressActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        currentWindTurbineName = databaseHelper.getWindTurbineName(currentWindTurbineId);
        tv_name.setText(currentWindTurbineName);

        ip_address = databaseHelper.getWindTurbineIpAddress(currentWindTurbineId);


        try {
            toggleButton = findViewById(R.id.submitButton);
            toggleButton.setText("Start");

            // Set initial state of includeArcDot
            includeArcDot = true;

            // Initialize the switch with the current state
            simOnOffSwitch.setChecked(includeArcDot);

            // Set up the switch listener
            simOnOffSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                includeArcDot = isChecked;
                initializeImageFetcher();
            });

            // Initialize the ImageFetcher with the initial state of includeArcDot
            initializeImageFetcher();

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

    private void updateInstructionText() {
        TextView instructionTextView = findViewById(R.id.instruction);
        instructionTextView.setText("Kalibrierung erfolgreich abgeschlossen!");

        ImageView checkImageView = findViewById(R.id.check);
        checkImageView.setColorFilter(Color.parseColor("#2e6b12"));
    }

    private void resetDatabase() {
        databaseHelper.resetDatabase();
    }

    private void updateUI_values(Location location) {
        System.out.println("New Coordinates");
        String gps_coordinates = convertToDMS(location.getLatitude()) + "\n" + convertToDMS(location.getLongitude());
        tv_gps.setText(gps_coordinates);
        tv_timestamp.setText(Calendar.getInstance().getTime().toString());
    }

    private void insertData(Location location) {
        double gps_long = location.getLongitude();
        double gps_lat = location.getLatitude();
        String timestamp = Calendar.getInstance().getTime().toString();

        // Add the red pixel coordinates to the database
        if (redPixelX != -1 && redPixelY != -1) {
            databaseHelper.addMeasurement(redPixelX, redPixelY, gps_long, gps_lat, currentWindTurbineId, timestamp);
            redPixelX = -1;
            redPixelY = -1;
            imageFetcher.increment_arc_dot();
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
        tv_gps.setText("Loading...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
        }
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }


    public void startButtonClicked(View view) {
        Log.d("CameraView", "startButtonClicked() wurde aufgerufen");

        if (!isDownloading) {
            isDownloading = true;
            toggleButton.setText("Stop");
            handler.post(imageDownloader);
            startLocationUpdates();

            TextView instructionTextView = findViewById(R.id.instruction);
            instructionTextView.setText("Laufen Sie im Bild herum und versuchen Sie dabei möglichst den Großteil des Bildes auszufüllen.");
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

    private void initializeImageFetcher() {
        imageFetcher = new ImageFetcher(ip_address, imageView, this, includeArcDot);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            exportCSVHelper.setWindTurbineId(currentWindTurbineId);
            exportCSVHelper.setCSVDefaultNameName("export_"+currentWindTurbineName);

            exportCSVHelper.createFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        exportCSVHelper.handleActivityResult(requestCode, resultCode, data);
    }

}
