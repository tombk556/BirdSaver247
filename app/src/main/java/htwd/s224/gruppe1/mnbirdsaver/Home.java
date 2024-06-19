package htwd.s224.gruppe1.mnbirdsaver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Matrix;
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
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;

// Importiere den DatabaseHelper aus dem Unterpaket
import htwd.s224.gruppe1.mnbirdsaver.util.DatabaseHelper;
import htwd.s224.gruppe1.mnbirdsaver.util.ImageFetcher;
import htwd.s224.gruppe1.mnbirdsaver.util.MatrixHelper;
import htwd.s224.gruppe1.mnbirdsaver.util.ExportCSVHelper;

/**
 * The {@code Home} class represents the main activity of the Bird Saver app,
 * handling the start of the application, user interactions, image fetching, GPS permissions, and database operations.
 * This class extends {@link AppCompatActivity} and implements {@link ImageFetcher.RedPixelCoordinatesListener}.
 */
public class Home extends AppCompatActivity implements ImageFetcher.RedPixelCoordinatesListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private ImageView imageView;
    private String ip_address;
    private Button simulationToggle;
    private Handler handler = new Handler();
    private TextView tv_name;
    private LocationRequest locationRequest;
    private DatabaseHelper databaseHelper;
    private MatrixHelper matrixHelper;
    private int currentWindTurbineId;
    private String currentWindTurbineName;
    private ImageFetcher imageFetcher;
    private boolean isDownloading = false;
    private Runnable imageDownloader;   // runnable task for image downloading
    private ExportCSVHelper exportCSVHelper;
    private int redPixelX = -1;     // saves detected pixel value (x)
    private int redPixelY = -1;     // saves detected pixel value (y)
    private boolean includeArcDot;
    private SwitchCompat simOnOffSwitch;

    /**
     * Called when the activity is first created. Initializes views, requests location permission,
     * and sets up the initial state of various components.
     * If there is no Ip Address set, the Home view will automatically navigate to the IP Address
     * Activity to set a new Ip Address.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *                           shut down then this Bundle contains the data it most recently
     *                           supplied in {@link #onSaveInstanceState}. Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        requestLocationPermission();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageView = findViewById(R.id.view);
        tv_name = findViewById(R.id.name);
        simulationToggle = findViewById(R.id.submitButton);
        simulationToggle.setText("Start");
        simOnOffSwitch = findViewById(R.id.SimOnOff);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(10000)
                .build();

        // initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);

        // Retrieve the last WindTurbine_ID, default is 0
        currentWindTurbineId = (int) databaseHelper.getCurrentWindTurbineId();

        if (currentWindTurbineId == 0) {
            Intent intent = new Intent(this, IpAddressActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        currentWindTurbineName =  databaseHelper.getWindTurbineName(currentWindTurbineId);
        tv_name.setText(currentWindTurbineName);

        ip_address =  databaseHelper.getWindTurbineIpAddress(currentWindTurbineId);


        try {
            simulationToggle = findViewById(R.id.submitButton);
            simulationToggle.setText("Start");

            // Set initial state of includeArcDot
            includeArcDot = false;

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

        matrixHelper = new MatrixHelper();
        databaseHelper.reset_matrix();

    }


    // Image and Red Pixel -------------------------------------------------------------------------
    /**
     * Initializes the {@link ImageFetcher} with the current IP address and other settings.
     */
    private void initializeImageFetcher() {
        imageFetcher = new ImageFetcher(ip_address, imageView, this, includeArcDot);
    }

    /**
     * Callback method that is invoked when red pixel coordinates are detected in the image.
     *
     * @param x The X coordinate of the red pixel.
     * @param y The Y coordinate of the red pixel.
     */
    @Override
    public void onRedPixelCoordinatesDetected(int x, int y) {
        redPixelX = x;
        redPixelY = y;

        if(redPixelX >= 0 && redPixelY >= 0){ // checks if red pixel was detected to enable "Starte Kalibrierung" button
            ImageView checkImageView = findViewById(R.id.check);
            checkImageView.setColorFilter(Color.parseColor("#2e6b12"));

            Button calibrateButton = findViewById(R.id.button);
            calibrateButton.setBackgroundColor(ContextCompat.getColor(this, R.color.light_green));
            calibrateButton.setTextColor(ContextCompat.getColor(this, R.color.dark_grey));
            calibrateButton.setEnabled(true);
        }
    }

    // GPS Permissions -----------------------------------------------------------------------------
    /**
     * Requests location permission from the user if not already granted.
     */
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Standortberechtigung bereits erteilt.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Callback method for handling the result of permission requests.
     *
     * @param requestCode The request code passed in {@link #requestPermissions}.
     * @param permissions The requested permissions.
     * @param grantResults The results of the permission requests.
     */
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


    // Buttons -------------------------------------------------------------------------------------
    /**
     * Called when the start button is clicked. Starts or stops the image downloading process.
     *
     * @param view The view that was clicked.
     */
    public void startButtonClicked(View view) {
        if (!isDownloading) {
            isDownloading = true;
            simulationToggle.setText("Stop");
            handler.post(imageDownloader);

            TextView instructionTextView = findViewById(R.id.instruction);
            instructionTextView.setText("Laufen Sie ins Bild sodass Sie sich selbst sehen.");

        } else {
            isDownloading = false;
            simulationToggle.setText("Start");
            handler.removeCallbacks(imageDownloader);
        }
    }

    /**
     * Called when the continue button is clicked. Starts the {@link CameraViewActivity}.
     *
     * @param view The view that was clicked.
     */
    public void continueButtonClicked(View view) {
        Intent intent = new Intent(this, CameraViewActivity.class);
        startActivity(intent);
    }

    // Matrix --------------------------------------------------------------------------------------
    /**
     * Updates the matrix in the database for the current wind turbine.
     */
    private void updateMatrix_inDB() {
        MatrixHelper mxHelper =  new MatrixHelper();
        databaseHelper.getAffineTransformForWindTurbine(mxHelper, currentWindTurbineId);

    }

    /**
     * Tests the matrix by converting pixel coordinates to GPS coordinates and displaying them.
     */
    private void testMatrix_fromDB() {
        Matrix transformMatrix =  databaseHelper.getMatrixByWindTurbineId(matrixHelper, currentWindTurbineId);
        if (transformMatrix != null) {
            float[] gpsCoords = matrixHelper.pixelToGps(transformMatrix, 284, 296);
            String my_toast = "Input: " + 284 + "x "+ 296 + "y\n"
                    +"Longitude: " + gpsCoords[0] + " Latitude: " + gpsCoords[1];
            Toast.makeText(this, my_toast, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Matrix ist empty", Toast.LENGTH_SHORT).show();
        }
    }

    // Navigation ----------------------------------------------------------------------------------
    /**
     * Navigates to the home activity. It is called when the user clicks the "Home" button.
     *
     * @param view The view that was clicked.
     */
    public void navigateToHome(View view) {
        Intent intent = new Intent(this, Home.class);
        startActivity(intent);
        finish();
    }

    // Menu ----------------------------------------------------------------------------------------
    /**
     * Inflates the menu for the activity.
     *
     * @param menu The options menu in which you place your items.
     * @return true for the menu to be displayed; false otherwise.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Handles item selections in the options menu.
     *
     * @param item The selected menu item.
     * @return false to allow normal menu processing to proceed, true to consume it here.
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
            databaseHelper.getAffineTransformForWindTurbine(matrixHelper, currentWindTurbineId);
            exportCSVHelper = new ExportCSVHelper(this, databaseHelper.getMatrixCursor(currentWindTurbineId));
            exportCSVHelper.setCSVDefaultNameName("export_matrix_"+currentWindTurbineName);
            exportCSVHelper.createFile();
        } else if (id == R.id.action_test_matrix) {
            Cursor cursor = databaseHelper.getFilteredAverageCoordsCursor(currentWindTurbineId);
            Toast.makeText(this, "Count: "+ cursor.getCount(), Toast.LENGTH_LONG).show();
            databaseHelper.getAffineTransformForWindTurbine(matrixHelper, currentWindTurbineId);
            testMatrix_fromDB();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles the result from an activity started with startActivityForResult.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult().
     * @param resultCode The integer result code returned by the child activity through its setResult().
     * @param data An Intent, which can return result data to the caller.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        exportCSVHelper.handleActivityResult(requestCode, resultCode, data);
    }

}
