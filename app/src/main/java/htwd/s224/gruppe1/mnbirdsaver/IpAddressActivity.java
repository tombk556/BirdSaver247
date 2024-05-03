package htwd.s224.gruppe1.mnbirdsaver;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.EditText;

public class IpAddressActivity extends AppCompatActivity {
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ip_address_view);
        editText = findViewById(R.id.editText);
    }

    public void startCalibration(View view){
        // navigate to ip address view
        Intent intent = new Intent(this, Home.class);
        intent.putExtra("IPADDRESS", editText.getText().toString()); // make sure value for ip address can be used in Home / camera_view
        startActivity(intent);
    }

    // use home button to navigate to camera view
    // TODO replace hardcoded ip address
    public void navigateToHome(View view){
        Log.d("CREATION", "nav");

        // navigate to ip home view
        Intent intent = new Intent(this, Home.class);
        intent.putExtra("IPADDRESS", "141.56.131.15"); // make sure value for ip address can be used in Home / camera_view
        startActivity(intent);
    }
}

