package htwd.s224.gruppe1.mnbirdsaver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

// Importiere den DatabaseHelper aus dem Unterpaket
import java.util.ArrayList;
import java.util.List;


import htwd.s224.gruppe1.mnbirdsaver.util.DatabaseHelper;
import htwd.s224.gruppe1.mnbirdsaver.util.WindTurbine;

public class IpAddressActivity extends AppCompatActivity {
    EditText editIp, editName;
    DatabaseHelper databaseHelper;

    ImageButton imagebuttonFooter;

    int selected_windTurbineId = 0;

    int current_windTurbineId = 0;

    private static final String PREFS_NAME = "WindTurbinePrefs";

    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ip_address_view);
        editIp = findViewById(R.id.editText);
        editName = findViewById(R.id.editName);
        imagebuttonFooter = findViewById(R.id.footer_image_button);

        // DatabaseHelper initialisieren
        databaseHelper = new DatabaseHelper(this);

        current_windTurbineId = (int) databaseHelper.getCurrentWindTurbineId();

        // Initialisiere SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);


        // Spinner und Windrad Liste
        Spinner spinnerWindTurbines = findViewById(R.id.spinnerWindTurbines);
        List<WindTurbine> windTurbines = new ArrayList<>();
        windTurbines.add(new WindTurbine(0, "---", ""));           // Platzhalter

        List<WindTurbine> allWindTurbines = databaseHelper.getAllWindTurbines();
        windTurbines.addAll(allWindTurbines);

        if (!allWindTurbines.isEmpty()) {
            ArrayAdapter<WindTurbine> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, windTurbines);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerWindTurbines.setAdapter(adapter);

            spinnerWindTurbines.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    WindTurbine selectedTurbine = (WindTurbine) parent.getItemAtPosition(position);
                    if (selectedTurbine.getId() == 0) {
                        editIp.setEnabled(true);
                        //editIp.setText("");

                        editName.setEnabled(true);
                        editName.setText("");
                        selected_windTurbineId = 0;

                    } else {
                        editIp.setText(selectedTurbine.getIpAddress());
                        editName.setText(selectedTurbine.getName());
                        selected_windTurbineId = selectedTurbine.getId();

                        editIp.setEnabled(false);
                        editName.setEnabled(false);

                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    editIp.setEnabled(true);
                    //editIp.setText("");

                    editName.setEnabled(true);
                    editName.setText("");
                    selected_windTurbineId = 0;
                }

            });

        } else {
            spinnerWindTurbines.setVisibility(View.GONE);
        }

        if (current_windTurbineId == 0) {
            imagebuttonFooter.setEnabled(false);
            imagebuttonFooter.setVisibility(View.GONE);
        }

    }

    public void startCalibration(View view){
        // navigate to ip address view
        Intent intent = new Intent(this, Home.class);

        String ip = editIp.getText().toString();
        String name = editName.getText().toString();

        if (ip.isEmpty() || name.isEmpty()) {
            Toast.makeText(IpAddressActivity.this, "IP Adresse und Name\ndürfen nicht leer sein!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selected_windTurbineId == 0) {
            databaseHelper.addWindTurbine(name, ip);
        } else {
            saveCurrentWindTurbineId(selected_windTurbineId);
        }

        startActivity(intent);
    }

    // use home button to navigate to camera view
    // TODO replace hardcoded ip address
    public void navigateToHome(View view){
        // navigate to ip home view
        Intent intent = new Intent(this, Home.class);
        startActivity(intent);
    }

    private void saveCurrentWindTurbineId(long windTurbineId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("CurrentWindTurbineId", windTurbineId);
        editor.apply();
    }

}

