package htwd.s224.gruppe1.mnbirdsaver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;


public class ActivityRedPixelDetector extends AppCompatActivity {
    Button btnPickImage;
    ImageView imageView;

    ActivityResultLauncher<Intent> resultLauncher;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.red_pixel_detector);

        btnPickImage = findViewById(R.id.btnPickImage);
        imageView = findViewById(R.id.imageView);
        registerResult();

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
                PixelDetector.Coordinate center = redPixels.get(0);
                Log.d("RedPixelCenter", "Center of red pixels: " + center.toString());
                Toast.makeText(this, "Center of red pixels: X: " + center.x + ", Y: " + center.y, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "No red pixels found.", Toast.LENGTH_SHORT).show();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show();
        }
    }


}