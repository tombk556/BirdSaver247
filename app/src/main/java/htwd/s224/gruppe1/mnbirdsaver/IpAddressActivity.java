package htwd.s224.gruppe1.mnbirdsaver;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.InputStream;


public class IpAddressActivity extends AppCompatActivity {
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ip_address_view);
        editText = findViewById(R.id.editText);
    }

    public void startCalibration(View view){
        //navigate to ip address view
        Intent intent = new Intent(this, Home.class);
        intent.putExtra("IPADDRESS", editText.getText().toString());
        startActivity(intent);
    }
}