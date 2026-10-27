package com.apkfactory.starter;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.rgb(22, 27, 34));
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("APK Factory");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("Tu taller de aplicaciones Android está listo para empezar.");
        subtitle.setTextColor(Color.rgb(190, 200, 215));
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 20, 0, 0);

        layout.addView(title);
        layout.addView(subtitle);
        setContentView(layout);
    }
}
