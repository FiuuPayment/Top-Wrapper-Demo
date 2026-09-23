package com.fiuu.toppayment;

import static com.fiuu.toppayment.TransactionType.ABORT;
import static com.fiuu.toppayment.TransactionType.CANCEL;
import static com.fiuu.toppayment.TransactionType.SALE;

import com.fiuu.toppayment.app.BuildConfig;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.fiuu.toppayment.TapSDK;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import my.com.softspace.reader.TransactionCodes;
import my.com.softspace.ssfasstapsdk.FasstapSDKConfiguration;
import my.com.softspace.ssfasstapsdk.pog.RecoverableAction;
import my.com.softspace.reader.TransactionCodes.TransactionUIEvent;
import my.com.softspace.ssfasstapsdk.transaction.Transaction.TransactionEvents.CardEvent;

public class FasstapManager {

    private TransactionType transactionType = TransactionType.INIT;

    private static FasstapManager instance;
    TapSDK tapSDK = new TapSDK();
    private String logger;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private FasstapManager(){}

    public static FasstapManager getInstance(){
        if(instance == null){
            instance = new FasstapManager();
        }
        return instance;
    }

    public interface TapEventListener{
        void onTransactionResult(JSONObject json);
        void onCardTapped();
        void onStatusUpdate(String message);
        void onCardTimeout(String operationCode, String operationMessage);
        void onPinRequired(boolean requiresPin);
        void onSignatureRequired();
    }

    private TapEventListener listener;
    public void setListener(TapEventListener listener){
        this.listener = listener;
    }

    public void initialiseSdk(Activity context){
        boolean isProduction = !BuildConfig.DEBUG;
        try{
            String vKey = BuildConfig.FASSTAP_VKEY;
            String country = "MY";

            FasstapSDKConfiguration configuration = FasstapSDKConfiguration.Builder
                    .create()
                    .setAttestationHost(BuildConfig.FASSTAP_HOST)
                    .setAttestationHostCertPinning(BuildConfig.FASSTAP_HOST_PINNING)
                    .setAttestationHostReadTimeout(10000L)
                    .setAttestationRefreshInterval(300000L)
                    .setAttestationStrictHttp(true)
                    .setAttestationConnectionTimeout(30000L)
                    .setKeyloadingHost(BuildConfig.FASSTAP_KEYLOADING_HOST)
                    .setKeyLoadingHostCertPinning(BuildConfig.FASSTAP_KEYLOADING_PINNING)
                    .setKeyLoadingCACert(BuildConfig.FASSTAP_CA_CERT)
                    .setLibGooglePlayProjNum("757874674469")
                    .setLibAccessKey(BuildConfig.FASSTAP_ACCESS_KEY)
                    .setLibSecretKey(BuildConfig.FASSTAP_SECRET_KEY)
                    .setIsProductionMode(isProduction)
                    .build();

            tapSDK.init(context, vKey, "SDK", country,true,true, configuration, new TapSDK.TapCallback() {
                @Override
                public void onLog(String s) {
                    DevLog.d("INIT", s);
                    // EVENT_ENTER_PIN doesn't reliably reach onTransactionUIEvent on some
                    // devices/SDK builds, but this raw SDK log line always precedes the native
                    // PIN pad being drawn - use it as the (only) reliable signal to get our
                    // dialog out of the way before the PIN pad renders underneath it.
                    if (s != null && s.toLowerCase(Locale.US).contains("pin pad")
                            && listener != null) {
                        listener.onPinRequired(true);
                    }
                }

                @Override
                public void onError(String s) {
                    logger = "onError" + s;
                    DevLog.e("INIT", logger);
                }

                @Override
                public void onRequireUserAction(RecoverableAction recoverableAction) {
                    DevLog.d("INIT", String.valueOf(recoverableAction));
                }

                @Override
                public void onLoginSuccess() {
                    if (listener != null) {
                        listener.onStatusUpdate("Please Tap Your Card");
                    }

                    try {
                        String cotsId = tapSDK.getCotsId(context.getApplicationContext());
                        String sdkVersion = tapSDK
                                .getSDKVersionInfo(context.getApplicationContext());
                        JSONObject json = new JSONObject();
                        json.put("cotsId", cotsId);
                        json.put("sdkVersion", sdkVersion);
                        DevLog.d("Login", "Success: " + json);
                    } catch (JSONException e) {
                        logger = "Error: " + e.getMessage();
                        DevLog.d("Login", logger);
                    }
                }

                @Override
                public void onCardStatus(int i) {
                    try {
                        JSONObject json = new JSONObject();
                        switch (i) {
                            case CardEvent.CardTapped:
                                writeLog("CardEvent: Card Tapped (" + i + ")");
                                json.put(Constant.OPERATION_CODE,
                                        Constant.CARD_PRESENTED_CODE);
                                json.put(Constant.OPERATION_MSG,
                                        Constant.CARD_PRESENTED_DESC);
                                if (listener != null) {
                                    listener.onCardTapped();
                                }
                                break;
                            case CardEvent.CardReadError:
                                writeLog("CardEvent: Card Read Error (" + i
                                        + ")");
                                json.put(Constant.OPERATION_CODE,
                                        Constant.CARD_READ_ERROR_CODE);
                                json.put(Constant.OPERATION_MSG,
                                        Constant.CARD_READ_ERROR_DESC);
                                break;
                            case CardEvent.CardReadTimeout:
                                writeLog("CardEvent: Card Read Timeout (" + i
                                        + ")");
                                json.put(Constant.OPERATION_CODE,
                                        Constant.CARD_TIME_OUT);
                                json.put(Constant.OPERATION_MSG,
                                        Constant.CARD_TIME_OUT_DESC);
                                if (listener != null) {
                                    listener.onCardTimeout(Constant.CARD_TIME_OUT,
                                            Constant.CARD_TIME_OUT_DESC);
                                }
                                return;
                            case CardEvent.CardReadDuplicate:
                                writeLog("CardEvent: Card Read Duplicate (" + i
                                        + ")");
                                json.put(Constant.OPERATION_CODE,
                                        Constant.CARD_READ_DUPLICATED_CODE);
                                json.put(Constant.OPERATION_MSG,
                                        Constant.CARD_READ_DUPLICATED_DESC);
                                break;
                            case CardEvent.CardTagNotSupported:
                                writeLog("CardEvent: Card Tag Not Supported ("
                                        + i
                                        + ")");
                                json.put(Constant.OPERATION_CODE,
                                        Constant.CARD_TAG_NOT_SUPPORTED_CODE);
                                json.put(Constant.OPERATION_MSG,
                                        Constant.CARD_TAG_NOT_SUPPORTED_DESC);
                                break;
                            case CardEvent.NfcUnexpectedError:
                                writeLog("CardEvent: NFC Unexpected Error (" + i
                                        + ")");
                                json.put(Constant.OPERATION_CODE,
                                        Constant.NFC_NOT_SUPPORTED_CODE);
                                json.put(Constant.OPERATION_MSG,
                                        Constant.NFC_NOT_SUPPORTED_DESC);
                                break;
                            default:
                                writeLog(
                                        "CardEvent: Unknown Event (" + i + ")");
                                json.put(Constant.OPERATION_CODE,
                                        Constant.CARD_READ_UNKNOWN_CODE);
                                json.put(Constant.OPERATION_MSG,
                                        "CardEvent: Unknown Event (" + i + ")");
                                break;
                        }

                        if (listener != null) {
                            listener.onStatusUpdate(json.getString(Constant.OPERATION_MSG));
                        }
                    } catch (Exception e) {
                        writeLog("onCardStatus " + e.getMessage());
                    }
                }

                @Override
                public void onTransactionUIEvent(int i) {
                    try {
                        switch (i) {
                            case TransactionUIEvent.EVENT_CARD_READ_OK_REMOVE_CARD:
                                writeLog(
                                        "onTransactionUIEvent: Card Read OK, Remove Card ("
                                                + i + ")");
                                if (listener != null) {
                                    listener.onStatusUpdate("Card Read OK, Remove Card");
                                }
                                break;
                            case TransactionUIEvent.EVENT_ENTER_PIN:
                                writeLog("onTransactionUIEvent: ENTER PIN (" + i + ")");
                                if (listener != null) {
                                    listener.onPinRequired(true);
                                }
                                break;
                            case TransactionUIEvent.EVENT_PIN_TIMEOUT:
                                writeLog("onTransactionUIEvent: ENTER PIN TIMEOUT (" + i
                                        + ")");
                                if (listener != null) {
                                    listener.onPinRequired(false);
                                }
                                break;
                            case TransactionUIEvent.EVENT_CANCEL_PIN:
                                writeLog("onTransactionUIEvent: ENTER CANCEL PIN (" + i
                                        + ")");
                                if (listener != null) {
                                    listener.onPinRequired(false);
                                }
                                break;
                            case TransactionUIEvent.EVENT_PIN_ENTERED:
                                writeLog("onTransactionUIEvent: PIN ENTERED (" + i
                                        + ")");
                                if (listener != null) {
                                    listener.onPinRequired(false);
                                }
                                break;
                            case TransactionUIEvent.EVENT_PIN_BYPASS:
                                writeLog("onTransactionUIEvent: EVENT PIN BYPASS (" + i
                                        + ")");
                                break;
                            case TransactionUIEvent.UNKNOWN_EVENT:
                                writeLog("onTransactionUIEvent: UNKNOWN EVENT (" + i
                                        + ")");
                                break;
                            case TransactionAuthorization.SIGNATURE:
                                writeLog("onTransactionUIEvent: TransactionAuthorization.SIGNATURE (" + i
                                        + ")");
                                if (listener != null) {
                                    listener.onSignatureRequired();
                                }
                                break;
                            case TransactionAuthorization.PIN:
                                writeLog(
                                        "onTransactionUIEvent: TransactionAuthorization PIN Authorization ("
                                                + i + ")");
                                break;
                            default:
                                writeLog("onTransactionUIEvent: Unhandled event (" + i
                                        + ")");
                                break;
                        }
                    } catch (Exception e) {
                        writeLog(e.getMessage());
                    }
                }

                @Override
                public void onTransactionResult(int i, String s) {
                    switch (i) {
                        case 0:
                        case 200:
                            logger = "Transaction Result: SUCCESS (" + i + ") " + s;
                            writeLog(logger);

                            try {
                                JSONObject json = new JSONObject(s);

                                String statusCode = json.getString("statusCode");
                                String statusMessage = json.getString("statusMessage");

                                if (statusCode.equals("00") || statusCode.equals("0")) {
                                    switch (transactionType) {
                                        case SALE:
                                            json.put(Constant.OPERATION_CODE, Constant.SUCCESSFULLY_SCAN_CODE);
                                            json.put(Constant.OPERATION_MSG, Constant.SUCCESSFULLY_SCAN_DESC);
                                            break;
                                        case CANCEL:
                                            json.put(Constant.OPERATION_CODE, Constant.VOID_APPROVED_CODE);
                                            json.put(Constant.OPERATION_MSG, Constant.VOID_APPROVED_DESC);
                                            break;
                                    }
                                } else {
                                    json.put(Constant.OPERATION_CODE, statusCode);
                                    json.put(Constant.OPERATION_MSG, statusMessage);
                                }

                                //    SEND RESULT BACK TO UI
                                if (listener != null) {
                                    listener.onTransactionResult(json);
                                }

                            } catch (JSONException e) {
                                writeLog(e.getMessage());
                            }

                            break;
                        case 7004:
                            displayErrorMessage(i, "CARD DECLINED");
                            break;
                        case 7005:
                            displayErrorMessage(i, "CARD FAILED");
                            break;
                        case 7006:
                            displayErrorMessage(i, "NO APP ERROR");
                            break;
                        case 7007:
                            displayErrorMessage(i, "FAILED - ALLOW FALLBACK");
                            break;
                        case 7008:
                            displayErrorMessage(i, "CARD EXPIRED");
                            break;
                        case 7020:
                            displayErrorMessage(i, "ONLINE FAIL");
                            break;
                        case 7024:
                            displayErrorMessage(i, "CARD CANCELLED");
                            break;
                        case 7028:
                            displayErrorMessage(i, "CARD TIMEOUT");
                            break;
                        case 7030:
                            displayErrorMessage(i, "CARD ERROR");
                            break;
                        case 7052:
                            displayErrorMessage(i, "READER CONFIG ERROR");
                            break;
                        case 7053:
                            displayErrorMessage(i, "SELECT NEXT ERROR");
                            break;
                        case 7054:
                            displayErrorMessage(i, "REQUIRE CDCVM");
                            break;
                        case 7055:
                            displayErrorMessage(i, "END APPLICATION ERROR");
                            break;
                        case 7056:
                        default:
                            displayErrorMessage(i, "UNKNOWN (" + s + ")");
                            break;
                    }
                }
            });
            tapSDK.loginWithResetProvision(context, BuildConfig.FASSTAP_UNIQUE_ID);
        }catch (Exception e){
            DevLog.e("INIT FAILED", e.toString());
        }
    }

    public void startTransaction(Context context, String uniqueId, String orderId, String amount){
        transactionType = SALE;

        try{
            mainHandler.post(
                    ()-> tapSDK.startTransactionFlow(context, uniqueId, orderId, amount));
        } catch (Exception e){
            writeLog("Error startTransaction " + e.getMessage());
        }
    }

    public void abortTransaction(){
        transactionType = ABORT;

        try{
            mainHandler.post(
                    ()-> tapSDK.abortTransaction());
        } catch (Exception e){
            writeLog("Error abortTransaction " + e.getMessage());
        }
    }

    public void submitSignature(String signaturePath, String signatureBase64){
        try{
            mainHandler.post(
                    ()-> tapSDK.setAuthorizationCaptured(signaturePath, signatureBase64));
        } catch (Exception e){
            writeLog("Error submitSignature " + e.getMessage());
        }
    }

    public void voidTransaction(String uniqueId, String orderId, String amount, String transId, String appId){
        transactionType = CANCEL;

        try{
            mainHandler.post(
                    ()-> tapSDK.cancelPurchase(amount, appId, transId, uniqueId, orderId));
        } catch (Exception e){
            writeLog("Error voidTransaction " + e.getMessage());
        }
    }

    private void writeLog(String message){
        DevLog.d(Constant.TAG, message);
    }
    void displayErrorMessage(int errorCode, String errorMessage) {
        logger = "Transaction Result: " + errorMessage + " (" + errorCode + ")";
        writeLog(logger);

        if (listener != null) {
            try {
                JSONObject json = new JSONObject();
                json.put(Constant.STATUS_CODE, String.valueOf(errorCode));
                json.put(Constant.STATUS_MESSAGE, errorMessage);
                listener.onTransactionResult(json);
            } catch (JSONException e) {
                writeLog(e.getMessage());
            }
        }
    }
}

