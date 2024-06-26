package htwd.s224.gruppe1.mnbirdsaver.legacy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Calendar;

import htwd.s224.gruppe1.mnbirdsaver.Home;
import htwd.s224.gruppe1.mnbirdsaver.R;

public class GPSActivity extends AppCompatActivity {

    private static final int PERMISSION_FINE_LOCATION = 99;
    boolean updateOn = false; // informs about active tracking
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_time, tv_address;
    SwitchCompat sw_location_updates;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        // GUI Elements
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_alt);
        tv_accuracy = findViewById(R.id.tv_acc);
        tv_time = findViewById(R.id.tv_time);
        tv_address = findViewById(R.id.tv_address);

        sw_location_updates = findViewById(R.id.sw_location_updates);

        // Initializing of  FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Configuration LocationRequest
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(4000)
                .build();

        // Creating a Location Callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // GUI mit Standortdaten aktualisieren
                    updateUI_values(location);
                }
            }
        };

        sw_location_updates.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startLocationUpdates();
            } else {
                stopLocationUpdates();
            }
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            updateOn = true;
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
        }
    }


    private void updateLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
        }

    }

    private void stopLocationUpdates() {
        updateOn = false;
        tv_lat.setText("Not tracking location");
        tv_lon.setText("Not tracking location");
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void updateUI_values(Location location) {
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));

        if (location.hasAltitude()){
            tv_altitude.setText(String.valueOf(location.getAltitude()));
        } else {
            tv_altitude.setText("Not available");
        }

        tv_accuracy.setText(String.valueOf(location.getAccuracy()));
        tv_time.setText(Calendar.getInstance().getTime().toString());

        String formattedLocation = getString(R.string.location_coordinates,
                convertToDMS(location.getLatitude()),
                convertToDMS(location.getLongitude()));
        tv_address.setText(formattedLocation);
    }

    @SuppressLint("DefaultLocale")
    private String convertToDMS(double decimalDegree) {
        int degree = (int) decimalDegree;
        double tempMinutes = (decimalDegree - degree) * 60;
        int minutes = (int) tempMinutes;
        double seconds = (tempMinutes - minutes) * 60;

        return String.format("%d° %d' %.6f\"", degree, minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateOn) {
            stopLocationUpdates();
        }
    }

    // use home button to navigate to camera view
    public void navigateToHome(View view){
        // navigate to ip home view
        Intent intent = new Intent(this, Home.class);
        startActivity(intent);
    }
}
