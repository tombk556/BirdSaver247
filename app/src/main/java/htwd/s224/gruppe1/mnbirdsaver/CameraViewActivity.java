package htwd.s224.gruppe1.mnbirdsaver;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Matrix;
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
import android.widget.Toast;

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
import htwd.s224.gruppe1.mnbirdsaver.util.MatrixHelper;

/**
 * The {@code CameraViewActivity} class provides the main camera view for the application.
 * It manages the user interface for displaying the camera feed, handling location updates,
 * and processing red pixel coordinates.
 */
public class CameraViewActivity extends AppCompatActivity implements ImageFetcher.RedPixelCoordinatesListener {

    private ImageView imageView;
    private String ip_address;
    private Button toggleButton;
    private Handler handler = new Handler();
    private static final int PERMISSION_FINE_LOCATION = 99;
    private TextView tv_gps, tv_timestamp, tv_name;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private DatabaseHelper databaseHelper;
    private ExportCSVHelper exportCSVHelper;
    private int currentWindTurbineId;
    private String currentWindTurbineName;
    private ImageFetcher imageFetcher;
    private boolean isDownloading = false;
    private Runnable imageDownloader;
    private int redPixelX = -1;
    private int redPixelY = -1;
    private boolean includeArcDot;
    private SwitchCompat simOnOffSwitch;
    private int locationCount = 0;
    private MatrixHelper mxHelper;

    /**
     * Called when the activity is first created. Initializes the UI components, sets up the
     * location request, initializes the image fetcher, and handles user interactions.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *                           shut down then this Bundle contains the data it most recently
     *                           supplied in {@link #onSaveInstanceState}. Otherwise, it is null.
     */
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
                    updateInstructionText();
                }
            }
        };

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);

        // Get the last wind turbine ID, default is 0
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
        mxHelper = new MatrixHelper();
        updateInstructionText();
    }


    // on Location Change --------------------------------------------------------------------------
    /**
     * Updates the UI with the current GPS coordinates and timestamp.
     *
     * @param location The current location.
     */
    private void updateUI_values(Location location) {
        System.out.println("New Coordinates");
        String gps_coordinates = _convertToDMS(location.getLatitude()) + "\n" + _convertToDMS(location.getLongitude());
        tv_gps.setText(gps_coordinates);
        tv_timestamp.setText(Calendar.getInstance().getTime().toString());
    }

    /**
     * Inserts the red pixel coordinates and GPS data into the database.
     *
     * @param location The current location.
     */
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

    /**
     * Updates the instruction text based on the number of measurements in the database.
     */
    private void updateInstructionText() {
        if (databaseHelper.getFilteredAverageCoordsCursor(currentWindTurbineId).getCount() >=4) {
            TextView instructionTextView = findViewById(R.id.instruction);
            instructionTextView.setText("Kalibrierung erfolgreich abgeschlossen!");

            ImageView checkImageView = findViewById(R.id.check);
            checkImageView.setColorFilter(Color.parseColor("#2e6b12"));
        }
    }

    /**
     * Converts the coordinates into the known form with degrees, minutes, and seconds.
     *
     * @param decimalDegree The coordinate in decimal degrees.
     * @return The coordinate as a string in degrees, minutes, and seconds.
     */
    @SuppressLint("DefaultLocale")
    private String _convertToDMS(double decimalDegree) {
        int degree = (int) decimalDegree;
        double tempMinutes = (decimalDegree - degree) * 60;
        int minutes = (int) tempMinutes;
        double seconds = (tempMinutes - minutes) * 60;
        return String.format("%d° %d' %.6f\"", degree, minutes, seconds);
    }

    // Image and Red Pixel -------------------------------------------------------------------------
    /**
     * Initializes the {@code ImageFetcher} with the current IP address and arc dot inclusion setting.
     */
    private void initializeImageFetcher() {
        imageFetcher = new ImageFetcher(ip_address, imageView, this, includeArcDot);
    }

    /**
     * Callback method for when red pixel coordinates are detected.
     *
     * @param x The X coordinate of the red pixel.
     * @param y The Y coordinate of the red pixel.
     */
    @Override
    public void onRedPixelCoordinatesDetected(int x, int y) {
        redPixelX = x;
        redPixelY = y;
    }

    // GPS -----------------------------------------------------------------------------------------
    /**
     * Starts the location updates.
     */
    private void startLocationUpdates() {
        tv_gps.setText("Loading...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
        }
    }

    /**
     * Stops the location updates.
     */
    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }


    // Buttons -------------------------------------------------------------------------------------
    /**
     * Handles the start button click to start or stop image downloading and location updates.
     *
     * @param view The button view.
     */
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


    // Navigation ----------------------------------------------------------------------------------
    /**
     * Navigates to the home activity.
     *
     * @param view The button view.
     */
    public void navigateToHome(View view) {
        Intent intent = new Intent(this, Home.class);
        startActivity(intent);
        finish();
    }

    /**
     * Navigates to the GPS activity.
     *
     * @param view The button view.
     */
    public void navigateToGPS(View view) {
        Intent intent = new Intent(this, GPSActivity.class);
        startActivity(intent);
    }

    // Matrix --------------------------------------------------------------------------------------
    /**
     * Updates the transformation matrix in the database.
     */
    private void updateMatrix_inDB() {
        MatrixHelper mxHelper =  new MatrixHelper();
        databaseHelper.getAffineTransformForWindTurbine(mxHelper, currentWindTurbineId);

    }

    /**
     * Retrieves the transformation matrix from the database.
     *
     * @return The transformation matrix.
     */
    private Matrix getMatrix_fromDB() {
        MatrixHelper mxHelper =  new MatrixHelper();
        return databaseHelper.getMatrixByWindTurbineId(mxHelper, currentWindTurbineId);
    }

    /**
     * Tests the transformation matrix from the database by converting pixel coordinates to GPS coordinates.
     * This method can be used later on to get any pixel values in the camera view and its related GPS coordinates.
     */
    private void testMatrix_fromDB() {
        MatrixHelper mxHelper =  new MatrixHelper();
        Matrix transformMatrix =  databaseHelper.getMatrixByWindTurbineId(mxHelper, currentWindTurbineId);
        if (transformMatrix != null) {
            float[] gpsCoords = mxHelper.pixelToGps(transformMatrix, 284, 296);
            String my_toast = "Input: " + 284 + "x "+ 296 + "y\n"
                    +"Result: Longitude: " + gpsCoords[0] + ", Latitude: " + gpsCoords[1];
            Toast.makeText(this, my_toast, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Matrix ist empty", Toast.LENGTH_SHORT).show();
        }
    }


    // Menu ----------------------------------------------------------------------------------------
    /**
     * Inflates the options menu.
     *
     * @param menu The options menu.
     * @return True if the menu was successfully created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Handles item selections from the options menu.
     *
     * @param item The selected menu item.
     * @return True if the item was successfully handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, IpAddressActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.action_export_measurement) {
            exportCSVHelper = new ExportCSVHelper(this, databaseHelper.getAverageCoordsCursor(currentWindTurbineId));
            exportCSVHelper.setCSVDefaultNameName("export_"+currentWindTurbineName);
            exportCSVHelper.createFile();
            return true;
        } else if (id == R.id.action_export_matrix) {
            databaseHelper.getAffineTransformForWindTurbine(mxHelper, currentWindTurbineId);
            exportCSVHelper = new ExportCSVHelper(this, databaseHelper.getMatrixCursor(currentWindTurbineId));
            exportCSVHelper.setCSVDefaultNameName("export_matrix_"+currentWindTurbineName);
            exportCSVHelper.createFile();
        } else if (id == R.id.action_test_matrix) {
            databaseHelper.getAffineTransformForWindTurbine(mxHelper, currentWindTurbineId);
            testMatrix_fromDB();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles the result of activities started for a result.
     *
     * @param requestCode The request code.
     * @param resultCode The result code.
     * @param data The result data.
     */    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        exportCSVHelper.handleActivityResult(requestCode, resultCode, data);
    }
}
