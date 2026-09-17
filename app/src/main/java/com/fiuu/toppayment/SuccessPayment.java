package com.fiuu.toppayment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.fiuu.toppayment.app.R;

public class SuccessPayment extends AppCompatActivity {

    private TextView logText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        int actionBarColor = ContextCompat.getColor(this, R.color.colorSurfaceBase);
        EdgeToEdge.enable(this,
                SystemBarStyle.light(actionBarColor, actionBarColor),
                SystemBarStyle.light(actionBarColor, actionBarColor));
        setContentView(R.layout.success_payment);

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
        String logger = intent.getStringExtra("logger");
        boolean isVoid = intent.getBooleanExtra("isVoid", false);

        DevLog.d("Transaction", "payment completed");

        TextView trxTitle = findViewById(R.id.trxTitle);
        Button btnExit = findViewById(R.id.btnExit);
        logText = findViewById(R.id.logText);

        trxTitle.setText(isVoid ? R.string.void_success_title : R.string.payment_success_title);

        btnExit.setOnClickListener(view -> btnExitOnClick());
        logText.setText(logger);

        successGif();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToMain();
            }
        });
    }

    private void navigateToMain() {
        // MainActivity is already the activity directly below this one on the back stack
        // (it started us with a plain startActivity, no flags), so just finish back to it
        // rather than relaunching it - relaunching with CLEAR_TOP/NEW_TASK would destroy and
        // recreate that standard-launch-mode instance, wiping its last-transaction state.
        finish();
    }

    public void successGif(){
        ImageView gifImageView = findViewById(R.id.successGif);

        Glide.with(this)
                .asGif()
                .load(R.drawable.success)
                .into(gifImageView);
    }

    public void btnStatusOnClick() {
        logText.setVisibility(View.VISIBLE);
    }

    public void btnExitOnClick() {
        navigateToMain();
    }
}
