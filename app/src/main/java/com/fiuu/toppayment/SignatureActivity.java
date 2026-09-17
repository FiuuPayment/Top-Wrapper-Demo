package com.fiuu.toppayment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.Toast;

import com.fiuu.toppayment.app.R;

import java.io.ByteArrayOutputStream;

import se.warting.signatureview.views.SignaturePad;

public class SignatureActivity extends Activity {

    private SignaturePad signaturePad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signature_pad_activity);

        signaturePad = findViewById(R.id.signature_pad);

        Button clearButton = findViewById(R.id.clear_button);
        clearButton.setOnClickListener(v -> signaturePad.clear());

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            Bitmap signatureBitmap = signaturePad.getSignatureBitmap();
            if (signatureBitmap != null) {
                String signatureBase64 = saveSignatureImage(signatureBitmap);
                getIntent().putExtra("signature_base64", signatureBase64);
                setResult(RESULT_OK, getIntent());
                finish();
            } else {
                Toast.makeText(SignatureActivity.this, "Please provide a signature first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String saveSignatureImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
}
