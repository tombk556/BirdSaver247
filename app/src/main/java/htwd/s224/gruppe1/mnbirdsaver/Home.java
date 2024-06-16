package htwd.s224.gruppe1.mnbirdsaver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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

public class Home extends AppCompatActivity implements ImageFetcher.RedPixelCoordinatesListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;


    private ImageView imageView;
    private String ip_address;
    private Button toggleButton;
    private Handler handler = new Handler();
    TextView tv_name;
    LocationRequest locationRequest;

    DatabaseHelper databaseHelper;

    private int currentWindTurbineId;
    private String currentWindTurbineName;

    ImageFetcher imageFetcher;
    private boolean isDownloading = false;
    private Runnable imageDownloader;
    ExportCSVHelper exportCSVHelper;
    private int redPixelX = -1;
    private int redPixelY = -1;

    private boolean includeArcDot;
    private SwitchCompat simOnOffSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        requestLocationPermission();


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageView = findViewById(R.id.view);
        tv_name = findViewById(R.id.name);
        toggleButton = findViewById(R.id.submitButton);
        toggleButton.setText("Start");
        simOnOffSwitch = findViewById(R.id.SimOnOff);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(10000)
                .build();

        // DatabaseHelper initialisieren
        databaseHelper = new DatabaseHelper(this);

        //databaseHelper.resetViews();

        // Letzte WindTurbine_ID abrufen, Standardwert ist 0
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
            toggleButton = findViewById(R.id.submitButton);
            toggleButton.setText("Start");

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

    }


    // Image and Red Pixel -------------------------------------------------------------------------
    private void initializeImageFetcher() {
        imageFetcher = new ImageFetcher(ip_address, imageView, this, includeArcDot);
    }


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


    // Buttons -------------------------------------------------------------------------------------
    public void startButtonClicked(View view) {
        if (!isDownloading) {
            isDownloading = true;
            toggleButton.setText("Stop");
            handler.post(imageDownloader);

            TextView instructionTextView = findViewById(R.id.instruction);
            instructionTextView.setText("Laufen Sie ins Bild sodass Sie sich selbst sehen.");

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

    // Matrix --------------------------------------------------------------------------------------

    private void updateMatrix_inDB() {
        MatrixHelper mxHelper =  new MatrixHelper();
        databaseHelper.getAffineTransformForWindTurbine(mxHelper, currentWindTurbineId);

    }

    private Matrix getMatrix_fromDB() {
        MatrixHelper mxHelper =  new MatrixHelper();
        return databaseHelper.getMatrixByWindTurbineId(mxHelper, currentWindTurbineId);
    }

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

    // Navigation ----------------------------------------------------------------------------------
    public void navigateToHome(View view) {
        Intent intent = new Intent(this, Home.class);
        intent.putExtra("IPADDRESS", "141.56.131.15");
        startActivity(intent);
    }


    // Menu ----------------------------------------------------------------------------------------
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
        } else if (id == R.id.action_export_measurement) {
            exportCSVHelper = new ExportCSVHelper(this, databaseHelper.getAverageCoordsCursor(currentWindTurbineId));
            exportCSVHelper.setCSVDefaultNameName("export_"+currentWindTurbineName);
            exportCSVHelper.createFile();
            return true;
        } else if (id == R.id.action_export_matrix) {
            exportCSVHelper = new ExportCSVHelper(this, databaseHelper.getMatrixCursor(currentWindTurbineId));
            exportCSVHelper.setCSVDefaultNameName("export_matrix_"+currentWindTurbineName);
            exportCSVHelper.createFile();
        } else if (id == R.id.action_test_matrix) {
            testMatrix_fromDB();
        }

        return super.onOptionsItemSelected(item);
    }

    // Result of ExportCSVHelper
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        exportCSVHelper.handleActivityResult(requestCode, resultCode, data);
    }

}
