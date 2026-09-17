package com.fiuu.toppayment;

import com.fiuu.toppayment.app.BuildConfig;
import com.fiuu.toppayment.app.R;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity
    implements FasstapManager.TapEventListener
{

    /** Raw digits are accumulated as cents; 7 digits supports up to 99,999.99. */
    private static final int MAX_DIGITS = 7;

    private final String uniqueId = BuildConfig.FASSTAP_UNIQUE_ID;

    private TextView amountText;
    private Button chargeButton;
    private Button voidButton;
    private String current = "";

    private Dialog tapCardDialog;
    private TextView dialogTapSubtitle;
    private ObjectAnimator glowPulseAnimator;
    private String orderId;

    private Dialog voidTransactionDialog;
    private TextView dialogVoidSubtitle;
    private String lastUniqueId = "";
    private String lastOrderId = "";
    private String lastAmount = "";
    private String lastTransId = "";
    private String lastAid = "";
    private boolean pendingIsVoid = false;

    /**
     * True from the moment the signature pad is launched until its result comes back. The SDK's
     * transaction result can arrive while the user is still signing, so it must be held until
     * the signature is submitted instead of navigating to the success/fail page immediately.
     */
    private boolean awaitingSignature = false;
    private JSONObject pendingTransactionResult;

    private final ActivityResultLauncher<Intent> signatureResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                awaitingSignature = false;
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    String path = data != null ? data.getStringExtra("signature_path") : null;
                    String base64Signature = data != null ? data.getStringExtra("signature_base64") : null;
                    FasstapManager.getInstance().submitSignature(path, base64Signature);
                }
                if (pendingTransactionResult != null) {
                    JSONObject result2 = pendingTransactionResult;
                    pendingTransactionResult = null;
                    handleTransactionResult(result2);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        getPermissionFasstap();
        FasstapManager.getInstance().initialiseSdk(MainActivity.this);

        super.onCreate(savedInstanceState);
        // Status bar matches the action bar's background (colorPrimary, which equals
        // colorSurfaceBase's white) so the two form one continuous band instead of a
        // transparent strip that shows the screen's own background. colorPrimary itself isn't
        // referenced here because a vendored AAR (libs/topnewroute-debug.aar) shares this app's
        // namespace and also declares colorPrimary, which excludes it from this module's
        // compile-time R class even though it still resolves correctly at runtime.
        int actionBarColor = ContextCompat.getColor(this, R.color.colorSurfaceBase);
        EdgeToEdge.enable(this,
                SystemBarStyle.light(actionBarColor, actionBarColor),
                SystemBarStyle.light(actionBarColor, actionBarColor));
        setContentView(R.layout.activity_main);

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

        amountText = findViewById(R.id.amountText);
        chargeButton = findViewById(R.id.btnTrx);
        voidButton = findViewById(R.id.btnVoidTrx);

        chargeButton.setOnClickListener(view -> btnStartOnClick());
        voidButton.setOnClickListener(view -> btnVoidOnClick());
        findViewById(R.id.btnInfo).setOnClickListener(v -> showInfoDialog());

        View.OnClickListener digitListener = v -> onDigitPressed(((TextView) v).getText().toString());
        int[] digitKeyIds = {R.id.key0, R.id.key1, R.id.key2, R.id.key3, R.id.key4,
                R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9};
        for (int id : digitKeyIds) {
            findViewById(id).setOnClickListener(digitListener);
        }

        findViewById(R.id.keyClear).setOnClickListener(v -> onClearPressed());
        findViewById(R.id.keyBackspace).setOnClickListener(v -> onBackspacePressed());

        refreshAmount();
    }

    private void onDigitPressed(String digit) {
        if (current.length() >= MAX_DIGITS) {
            return;
        }
        String next = current.equals("0") ? digit : current + digit;
        next = next.replaceFirst("^0+(?=\\d)", "");
        current = next;
        refreshAmount();
    }

    private void onClearPressed() {
        current = "";
        refreshAmount();
    }

    private void onBackspacePressed() {
        if (!current.isEmpty()) {
            current = current.substring(0, current.length() - 1);
        }
        refreshAmount();
    }

    private void refreshAmount() {
        String display = formatAsDecimal(current);
        amountText.setText(display);
        chargeButton.setText(getString(R.string.charge_button_label, display));
        chargeButton.setEnabled(isAmountValid());
    }

    private boolean isAmountValid() {
        return !current.isEmpty() && !current.equals("0");
    }

    /** Formats raw accumulated digits (cents) as a "whole.cents" decimal string, e.g. "2500" -> "25.00". */
    private String formatAsDecimal(String rawDigits) {
        String digits = rawDigits.isEmpty() ? "0" : rawDigits;
        while (digits.length() < 3) {
            digits = "0" + digits;
        }
        String cents = digits.substring(digits.length() - 2);
        String whole = digits.substring(0, digits.length() - 2).replaceFirst("^0+(?=\\d)", "");
        return whole + "." + cents;
    }

    private void getPermissionFasstap(){
        try {

             DevLog.d(Constant.TAG, "Get location permission");
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        Constant.PERMISSIONS_REQUEST_LOCATION);
            } else {
                try {
                    JSONObject json = new JSONObject();
                    json.put(Constant.OPERATION_CODE, Constant.PERMISSION_GRANTED_CODE);
                    json.put(Constant.OPERATION_MSG, Constant.PERMISSION_GRANTED_DESC);
                } catch (JSONException | RuntimeException e) {
                    e.printStackTrace();
                }
            }
            DevLog.d(Constant.TAG, "Permission granted");

        } catch (Exception | Error e) {
            DevLog.d(Constant.TAG, "Error granting permission");

            e.printStackTrace();
            try {
                JSONObject json = new JSONObject();
                json.put(Constant.OPERATION_CODE, Constant.INTERNAL_ERROR_CODE);
                json.put(Constant.OPERATION_MSG, e.getMessage());
            } catch (JSONException | RuntimeException exception) {
                exception.printStackTrace();
            }
        }
    }

    private void showInfoDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog, null);

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        TextView message = dialogView.findViewById(R.id.dialogMessage);
        Button exit = dialogView.findViewById(R.id.btnOK);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        exit.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    public void btnStartOnClick() {

        if (!isAmountValid()) {
            Toast.makeText(this, "Invalid input. Please enter a valid amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isNfcEnabled()) {
            return;
        }

        String amount = formatAsDecimal(current);
        DevLog.d("Main Activity", "amount: " + amount);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddkkmmss");
        orderId = dateFormat.format(new Date());

        pendingIsVoid = false;
        showTapCardDialog(amount);
        FasstapManager.getInstance().setListener(this);
        FasstapManager.getInstance().startTransaction(MainActivity.this, uniqueId, orderId, toCentsString(amount));
    }

    public void btnVoidOnClick(){
        showVoidTransactionDialog();
    }

    /** Converts a "whole.cents" decimal amount (e.g. "25.00") into a whole-cents string (e.g. "2500"). */
    private String toCentsString(String decimalAmount) {
        if (decimalAmount == null || decimalAmount.isEmpty()) {
            return "0";
        }
        try {
            BigDecimal cents = new BigDecimal(decimalAmount)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP);
            return cents.toPlainString();
        } catch (NumberFormatException e) {
            DevLog.d(Constant.TAG, "Invalid amount format: " + decimalAmount);
            return "0";
        }
    }

    private boolean isNfcEnabled() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter == null) {
            Toast.makeText(this, R.string.ALERT_NOT_SUPPORTED_MSG, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (adapter.isEnabled()) {
            return true;
        }

        new AlertDialog.Builder(this)
                .setMessage(R.string.ALERT_NFC_NOT_ENABLE)
                .setPositiveButton(R.string.ALERT_BTN_OK, (dialog, which) -> dialog.dismiss())
                .setNegativeButton(R.string.BTN_SETTINGS, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                        return;
                    }
                    intent = new Intent(Settings.ACTION_SETTINGS);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.ALERT_NOT_SUPPORTED_MSG)
                                .setCancelable(true)
                                .setPositiveButton(R.string.ALERT_BTN_OK, (d2, w2) -> d2.dismiss())
                                .show();
                    }
                })
                .setCancelable(false)
                .show();
        return false;
    }

    private void showTapCardDialog(String amount) {
        tapCardDialog = new Dialog(this, R.style.TapCardDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_tap_card, null);

        ImageView blurBackground = view.findViewById(R.id.dialogBlurBackground);
        blurBackground.setImageBitmap(createBlurredSnapshot(findViewById(R.id.main)));

        dialogTapSubtitle = view.findViewById(R.id.dialogTapSubtitle);
        TextView chargeAmountView = view.findViewById(R.id.dialogChargeAmount);
        chargeAmountView.setText(getString(R.string.dialog_charge_amount, amount));

        View glowRing = view.findViewById(R.id.nfcGlowRing);
        view.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> abortTransaction());

        tapCardDialog.setContentView(view);
        tapCardDialog.setCancelable(true);
        tapCardDialog.setOnDismissListener(d -> {
            stopGlowPulse();
            dialogTapSubtitle = null;
            tapCardDialog = null;
        });
        tapCardDialog.show();

        // The dialog's own layout now spans the full window and centers its card with margins,
        // so the window itself must be full-screen rather than sized to a fraction of the width.
        Window dialogWindow = tapCardDialog.getWindow();
        if (dialogWindow != null) {
            dialogWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            WindowCompat.setDecorFitsSystemWindows(dialogWindow, false);

            // This dialog is its own Window, so it doesn't inherit MainActivity's EdgeToEdge
            // system bar styling - without this it falls back to a transparent nav bar.
            int barColor = ContextCompat.getColor(this, R.color.colorSurfaceBase);
            dialogWindow.setStatusBarColor(barColor);
            dialogWindow.setNavigationBarColor(barColor);
            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(dialogWindow, dialogWindow.getDecorView());
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        startGlowPulse(glowRing);
    }

    private void showVoidTransactionDialog() {
        voidTransactionDialog = new Dialog(this, R.style.TapCardDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_void_transaction, null);

        ImageView blurBackground = view.findViewById(R.id.dialogVoidBlurBackground);
        blurBackground.setImageBitmap(createBlurredSnapshot(findViewById(R.id.main)));

        dialogVoidSubtitle = view.findViewById(R.id.dialogVoidSubtitle);

        EditText edtUniqueId = view.findViewById(R.id.edtVoidUniqueId);
        EditText edtOrderId = view.findViewById(R.id.edtVoidOrderId);
        EditText edtAmount = view.findViewById(R.id.edtVoidAmount);
        EditText edtTransId = view.findViewById(R.id.edtVoidTransId);
        EditText edtAid = view.findViewById(R.id.edtVoidAid);

        edtUniqueId.setText(lastUniqueId);
        edtOrderId.setText(lastOrderId);
        edtAmount.setText(lastAmount);
        edtTransId.setText(lastTransId);
        edtAid.setText(lastAid);

        Button btnProceed = view.findViewById(R.id.btnDialogVoidProceed);
        view.findViewById(R.id.btnDialogVoidCancel).setOnClickListener(v -> voidTransactionDialog.dismiss());
        btnProceed.setOnClickListener(v -> {
            String voidUniqueId = edtUniqueId.getText().toString().trim();
            String voidOrderId = edtOrderId.getText().toString().trim();
            String voidAmount = edtAmount.getText().toString().trim();
            String voidTransId = edtTransId.getText().toString().trim();
            String voidAppId = edtAid.getText().toString().trim();

            if (voidUniqueId.isEmpty() || voidOrderId.isEmpty() || voidAmount.isEmpty()) {
                Toast.makeText(this, "Unique ID, Order ID and Amount are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnProceed.setEnabled(false);
            pendingIsVoid = true;
            FasstapManager.getInstance().setListener(this);
            FasstapManager.getInstance().voidTransaction(voidUniqueId, voidOrderId, voidAmount, voidTransId, voidAppId);
        });

        voidTransactionDialog.setContentView(view);
        voidTransactionDialog.setCancelable(true);
        voidTransactionDialog.setOnDismissListener(d -> {
            dialogVoidSubtitle = null;
            voidTransactionDialog = null;
        });
        voidTransactionDialog.show();

        Window dialogWindow = voidTransactionDialog.getWindow();
        if (dialogWindow != null) {
            dialogWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            WindowCompat.setDecorFitsSystemWindows(dialogWindow, false);

            int barColor = ContextCompat.getColor(this, R.color.colorSurfaceBase);
            dialogWindow.setStatusBarColor(barColor);
            dialogWindow.setNavigationBarColor(barColor);
            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(dialogWindow, dialogWindow.getDecorView());
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }
    }

    /**
     * Renders a blurred still of the given view into a bitmap sized to it. Downscaling before
     * a bilinear upscale is a cheap, dependency-free blur that works on every API level and
     * device, unlike the platform's cross-window blur which many emulators and devices don't
     * support at all.
     */
    private Bitmap createBlurredSnapshot(View source) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        Bitmap fullSize = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        source.draw(new Canvas(fullSize));

        float downscale = 0.005f;
        int smallWidth = Math.max(1, (int) (width * downscale));
        int smallHeight = Math.max(1, (int) (height * downscale));
        Bitmap shrunk = Bitmap.createScaledBitmap(fullSize, smallWidth, smallHeight, true);
        Bitmap blurred = Bitmap.createScaledBitmap(shrunk, width, height, true);

        fullSize.recycle();
        shrunk.recycle();
        return blurred;
    }

    private void abortTransaction(){
        FasstapManager.getInstance().abortTransaction();
        dismissTapCardDialog();
    }

    private void dismissTapCardDialog() {
        if (tapCardDialog != null) {
            tapCardDialog.dismiss();
        }
    }

    private void dismissVoidTransactionDialog() {
        if (voidTransactionDialog != null) {
            voidTransactionDialog.dismiss();
        }
    }

    private void startGlowPulse(View glowView) {
        if (glowView == null) {
            return;
        }
        glowPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(glowView,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.15f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.15f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.5f, 1f));
        glowPulseAnimator.setDuration(1000);
        glowPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        glowPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        glowPulseAnimator.start();
    }

    private void stopGlowPulse() {
        if (glowPulseAnimator != null) {
            glowPulseAnimator.cancel();
            glowPulseAnimator = null;
        }
    }

    private void intentSuccess(String transId, String jsonData) {
        boolean isVoid = pendingIsVoid;
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, SuccessPayment.class);
            intent.putExtra("logger", "Trans ID: " + transId + "\n" + jsonData);
            intent.putExtra("isVoid", isVoid);
            startActivity(intent);
        }, 2000);
    }

    private void intentFail(String logger) {
        Intent intent = new Intent(MainActivity.this, FailPayment.class);
        intent.putExtra("logger", logger);
        intent.putExtra("isVoid", pendingIsVoid);
        startActivity(intent);
    }

    @Override
    public void onTransactionResult(JSONObject json) {
        runOnUiThread(() -> {
            if (awaitingSignature) {
                // Hold the result until the signature pad returns instead of navigating away
                // while the user is still signing.
                pendingTransactionResult = json;
                return;
            }
            handleTransactionResult(json);
        });
    }

    private void handleTransactionResult(JSONObject json) {
        dismissTapCardDialog();
        dismissVoidTransactionDialog();
        try {
            String status = json.getString("statusCode");
            String operationCode = json.optString(Constant.OPERATION_CODE, "");
            if (status.equals("00") || status.equals("0")) {
                if (operationCode.equals(Constant.SUCCESSFULLY_SCAN_CODE)) {
                    // A completed sale becomes the new "last transaction" available to void.
                    lastUniqueId = uniqueId;
                    lastOrderId = orderId;
                    lastAmount = json.optString("transAmount", "");
                    lastTransId = json.optString("transID", "");
                    lastAid = json.optString("appId", "");
                    voidButton.setEnabled(true);
                } else if (operationCode.equals(Constant.VOID_APPROVED_CODE)) {
                    // Already voided - nothing left to void until the next sale.
                    voidButton.setEnabled(false);
                }
                intentSuccess(json.optString("transID", ""), json.toString());
            } else {
                intentFail(json.optString("statusMessage", "Transaction failed"));
            }
        } catch (JSONException e) {
            intentFail("Error: " + e.getMessage());
        }
    }

    @Override
    public void onCardTapped() {
        // status text is already driven by onStatusUpdate()
    }

    @Override
    public void onStatusUpdate(String message) {
        runOnUiThread(() -> {
            if (dialogTapSubtitle != null) {
                dialogTapSubtitle.setText(message);
            }
            if (dialogVoidSubtitle != null) {
                dialogVoidSubtitle.setText(message);
            }
        });
    }

    @Override
    public void onCardTimeout(String operationCode, String operationMessage) {
        runOnUiThread(() -> {
            dismissTapCardDialog();
            dismissVoidTransactionDialog();
            intentFail(operationMessage + " (" + operationCode + ")");
        });
    }

    @Override
    public void onSignatureRequired() {
        runOnUiThread(() -> {
            awaitingSignature = true;
            signatureResultLauncher.launch(new Intent(MainActivity.this, SignatureActivity.class));
        });
    }

    @Override
    public void onPinRequired(boolean requiresPin) {
        // The SDK's native PIN entry screen is drawn behind our dialog's window, so it must be
        // hidden (not dismissed) while PIN entry is in progress, then restored once it's done.
        runOnUiThread(() -> {
            if (requiresPin) {
                if (tapCardDialog != null) {
                    tapCardDialog.hide();
                }
                if (voidTransactionDialog != null) {
                    voidTransactionDialog.hide();
                }
            } else {
                if (tapCardDialog != null) {
                    tapCardDialog.show();
                }
                if (voidTransactionDialog != null) {
                    voidTransactionDialog.show();
                }
            }
        });
    }
}