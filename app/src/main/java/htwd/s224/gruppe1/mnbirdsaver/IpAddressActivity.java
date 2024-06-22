package htwd.s224.gruppe1.mnbirdsaver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// Importiere den DatabaseHelper aus dem Unterpaket
import java.util.ArrayList;
import java.util.List;


import htwd.s224.gruppe1.mnbirdsaver.util.DatabaseHelper;
import htwd.s224.gruppe1.mnbirdsaver.util.WindTurbine;

/**
 * The {@code IpAddressActivity} class provides the user interface for setting the IP address
 * and name of the wind turbine. It allows users to select existing wind turbines from a spinner
 * or add a new one.
 */
public class IpAddressActivity extends AppCompatActivity {
    private EditText editIp, editName;
    private DatabaseHelper databaseHelper;
    private ImageButton imagebuttonFooter;
    private int selected_windTurbineId = 0;
    private int current_windTurbineId = 0;
    private static final String PREFS_NAME = "WindTurbinePrefs";
    private SharedPreferences sharedPreferences;

    /**
     * Called when the activity is first created. Initializes the UI components,
     * sets up the spinner for wind turbines, and handles user interactions.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *                           shut down then this Bundle contains the data it most recently
     *                           supplied in {@link #onSaveInstanceState}. Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ip_address_view);
        editIp = findViewById(R.id.editText);
        editName = findViewById(R.id.editName);
        imagebuttonFooter = findViewById(R.id.footer_image_button);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);

        current_windTurbineId = (int) databaseHelper.getCurrentWindTurbineId();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);


        // Setup Spinner for wind turbines
        Spinner spinnerWindTurbines = findViewById(R.id.spinnerWindTurbines);
        TextView label = findViewById(R.id.description2);

        List<WindTurbine> windTurbines = new ArrayList<>();
        windTurbines.add(new WindTurbine(0, "---", ""));    // Placeholder

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
                    editName.setEnabled(true);
                    editName.setText("");
                    selected_windTurbineId = 0;
                }

            });

        } else {
            spinnerWindTurbines.setVisibility(View.GONE);
            label.setVisibility(View.GONE);
        }

        if (current_windTurbineId == 0) {
            imagebuttonFooter.setEnabled(false);
            imagebuttonFooter.setVisibility(View.GONE);
        }

    }

    /**
     * Starts the calibration process by navigating to the Home activity.
     * If the IP address or name is empty, shows a Toast message.
     *
     * @param view The view that was clicked.
     */
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

    /**
     * Navigates to the Home activity.
     *
     * @param view The view that was clicked. It is called when "Home" button is clicked.
     */
    public void navigateToHome(View view){
        // navigate to ip home view
        Intent intent = new Intent(this, Home.class);
        startActivity(intent);
    }

    /**
     * Saves the current wind turbine ID to SharedPreferences.
     *
     * @param windTurbineId The ID of the wind turbine to save.
     */
    private void saveCurrentWindTurbineId(long windTurbineId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("CurrentWindTurbineId", windTurbineId);
        editor.apply();
    }

}

