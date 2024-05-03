package htwd.s224.gruppe1.mnbirdsaver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class FragmentFooter extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_footer);
    }

    public void navigateToHome(View view){
        // navigate to ip home view
        Intent intent = new Intent(this, IpAddressActivity.class);
        startActivity(intent);
    }
}
