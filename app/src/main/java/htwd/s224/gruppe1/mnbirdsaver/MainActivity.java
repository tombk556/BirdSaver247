package htwd.s224.gruppe1.mnbirdsaver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
}