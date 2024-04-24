package htwd.s224.gruppe1.mnbirdsaver;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestLocationPermission();  // wichtig, sonst funktioniert GPS später nicht!
    }


    public void navigateToIpAddressView(View view){
        // navigate to ip address view
        Intent intent = new Intent(this, IpAddressView.class);
        startActivity(intent);
    }

    public void navigateToDeleteView(View view){
        // navigate to ip address view
        Intent intent = new Intent(this, DeleteView.class);
        startActivity(intent);
    }

    public void navigateToGpsView(View view){
        // navigate to ip address view
        Intent intent = new Intent(this, GPSActivity.class);
        startActivity(intent);
    }

    public void navigateToRedPixelDetectorView(View view){
        // navigate to ip address view
        Intent intent = new Intent(this, ActivityRedPixelDetector.class);
        startActivity(intent);
    }

    public void navigateToCameraView(View view){
        // navigate to camera view
        Intent intent = new Intent(this, CameraView.class);
        startActivity(intent);
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

}