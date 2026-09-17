package com.fiuu.toppayment;

import com.fiuu.toppayment.app.R;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

public class FailPayment extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        int actionBarColor = ContextCompat.getColor(this, R.color.colorSurfaceBase);
        EdgeToEdge.enable(this,
                SystemBarStyle.light(actionBarColor, actionBarColor),
                SystemBarStyle.light(actionBarColor, actionBarColor));
        setContentView(R.layout.fail_payment);

        View root = findViewById(R.id.main);
        View navBarBg = findViewById(R.id.navBarBg);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            // Sized to the nav bar's height and pulled down by an equal negative margin so it
            // pokes out of root's own inset padding to sit exactly behind the nav bar - needed
            // because setNavigationBarColor/SystemBarStyle no longer tint it on API 35+.
            ViewGroup.MarginLayoutParams navBarBgParams = (ViewGroup.MarginLayoutParams) navBarBg.getLayoutParams();
            navBarBgParams.height = bars.bottom;
            navBarBgParams.bottomMargin = -bars.bottom;
            navBarBg.setLayoutParams(navBarBgParams);
            return WindowInsetsCompat.CONSUMED;
        });

        Intent intent = getIntent();
        boolean isVoid = intent.getBooleanExtra("isVoid", false);

        DevLog.d("Transaction", "payment completed");

        TextView trxTitle = findViewById(R.id.trxID);
        trxTitle.setText(isVoid ? R.string.void_failed_title : R.string.payment_failed_title);

        Button btnBackToMenu = findViewById(R.id.btnMenu);
        btnBackToMenu.setOnClickListener(v -> btnStatusOnClick());

        failGif();
    }

    public void failGif(){
        ImageView gifImageView = findViewById(R.id.successGif);

        Glide.with(this)
                .asGif()
                .load(R.drawable.error)
                .into(gifImageView);
    }

    public void btnStatusOnClick() { finish();}

}
