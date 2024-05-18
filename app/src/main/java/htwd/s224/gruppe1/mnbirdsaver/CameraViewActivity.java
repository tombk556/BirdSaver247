package htwd.s224.gruppe1.mnbirdsaver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
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

public class CameraViewActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private ImageView imageView;
    private String ip_address;
    private Button toggleButton;
    private Handler handler = new Handler();

    private static final int PERMISSION_FINE_LOCATION = 99;

    TextView tv_gps, tv_timestamp;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;

    private boolean isDownloading = false;
    private Runnable imageDownloader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);

        requestLocationPermission();

        imageView = findViewById(R.id.view);
        tv_gps = findViewById(R.id.gpsValue);
        tv_timestamp = findViewById(R.id.dateValue);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(4000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateUI_values(location);
                }
            }
        };

        try {
            Intent intent = getIntent();
            ip_address = intent.getStringExtra("IPADDRESS");

            if (ip_address == null || ip_address.isEmpty()) {
                throw new NullPointerException("IP address is not provided");
            }

            toggleButton = findViewById(R.id.submitButton);
            toggleButton.setText("Start");

            ImageFetcher imageFetcher = new ImageFetcher(ip_address, imageView);
            imageDownloader = new Runnable() {
                @Override
                public void run() {
                    imageFetcher.startFetching();
                    handler.postDelayed(this, 2000);
                }
            };

        } catch (NullPointerException e) {
            System.out.println("CATCH");

            Intent intent = new Intent(this, IpAddressActivity.class);
            startActivity(intent);
        }
    }

    private void updateUI_values(Location location) {
        System.out.println("New Coordinates");
        String gps_coordinates = convertToDMS(location.getLatitude()) + "\n" + convertToDMS(location.getLongitude());
        tv_gps.setText(gps_coordinates);
        tv_timestamp.setText(Calendar.getInstance().getTime().toString());
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
        Intent intent = new Intent(this, Home.class);
        intent.putExtra("IPADDRESS", "141.56.131.15");
        startActivity(intent);
    }
}