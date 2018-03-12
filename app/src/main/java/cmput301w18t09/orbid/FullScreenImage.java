package cmput301w18t09.orbid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class FullScreenImage  extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_full_screen_image);
        Bitmap bitmap = (Bitmap) getIntent().getParcelableExtra("BitmapImage");
        ImageView imgDisplay;
        imgDisplay = (ImageView) findViewById(R.id.BigImage);

        imgDisplay.setImageBitmap(bitmap);

    }


}