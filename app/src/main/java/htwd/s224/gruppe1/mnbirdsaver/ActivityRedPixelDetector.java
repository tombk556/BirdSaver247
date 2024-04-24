package htwd.s224.gruppe1.mnbirdsaver;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class ActivityRedPixelDetector extends AppCompatActivity {    Button btnPickImage;
    ImageView imageView;
    ListView listView;
    ActivityResultLauncher<Intent> resultLauncher;
    DatabaseHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.red_pixel_detector);

        btnPickImage = findViewById(R.id.btnPickImage);
        imageView = findViewById(R.id.imageView);
        listView = findViewById(R.id.listViewMeasurements);
        dbHelper = new DatabaseHelper(this); // Initialize the helper
        registerResult();
        loadMeasurementData(); // Load and display the database data

        btnPickImage.setOnClickListener(view -> pickImage());
    }


    private void pickImage(){
        Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
        resultLauncher.launch(intent);
    }

    private void registerResult(){
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result){
                        try{
                            Uri imageUri = result.getData().getData();
                            imageView.setImageURI(imageUri);
                            printImageDetails(imageUri);
                            loadMeasurementData();
                        } catch (Exception e){
                            Toast.makeText(ActivityRedPixelDetector.this, "No image selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }


    private void printImageDetails(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            List<PixelDetector.Coordinate> redPixels = PixelDetector.isPixelRed(bitmap);

            if (!redPixels.isEmpty()) {
                for (PixelDetector.Coordinate center : redPixels) {
                    Log.d("RedPixelCenter", "Center of red pixels: " + center.toString());
                    dbHelper.addRedPixel(center.x, center.y, 12.23f, 12.2f); // Store each red pixel in the database
                    Toast.makeText(this, "Red pixel saved: X: " + center.x + ", Y: " + center.y, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "No red pixels found.", Toast.LENGTH_SHORT).show();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadMeasurementData() {
        ArrayList<String> listItems = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        listView.setAdapter(adapter);

        Cursor cursor = dbHelper.getReadableDatabase().query(
                DatabaseHelper.TABLE_NAME,
                new String[] { "measurement_id", "pixel_x_coord", "pixel_y_coord", "gps_x_coord", "gps_y_coord", "created_at" },
                null, null, null, null, null);

        while (cursor.moveToNext()) {
            @SuppressLint("Range") String id = cursor.getString(cursor.getColumnIndex("measurement_id"));
            @SuppressLint("Range") int pixelX = cursor.getInt(cursor.getColumnIndex("pixel_x_coord"));
            @SuppressLint("Range") int pixelY = cursor.getInt(cursor.getColumnIndex("pixel_y_coord"));
            @SuppressLint("Range") float gpsX = cursor.getFloat(cursor.getColumnIndex("gps_x_coord"));
            @SuppressLint("Range") float gpsY = cursor.getFloat(cursor.getColumnIndex("gps_y_coord"));
            @SuppressLint("Range") String createdAt = cursor.getString(cursor.getColumnIndex("created_at"));

            String displayText = "ID: " + id + ", Pixel X: " + pixelX + ", Pixel Y: " + pixelY +
                    ", GPS X: " + gpsX + ", GPS Y: " + gpsY + ", Created At: " + createdAt;
            listItems.add(displayText);
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }



}