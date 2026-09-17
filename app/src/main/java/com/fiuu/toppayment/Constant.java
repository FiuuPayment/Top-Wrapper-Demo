package com.fiuu.toppayment;

public class Constant {

    // Success return
    public final static String INITIALISED_SUCCESS_CODE = "200";
    public final static  String INITIALISED_SUCCESS_DESC = "Initialised success";

    public final static String NO_PERMISSION_REQUIRE_CODE = "201";
    public final static String NO_PERMISSION_REQUIRE_DESC = "No permission require";

    public final static String PERMISSION_GRANTED_CODE = "202";
    public final static String PERMISSION_GRANTED_DESC = "Permission granted";

    public final static String SUCCESSFULLY_SCAN_CODE = "203";
    public final static String SUCCESSFULLY_SCAN_DESC = "Successfully scanned";

    public final static String SUCCESSFULLY_LOGGED_CODE = "204";
    public final static String SUCCESSFULLY_LOGGED_DESC = "Login successful";

    public final static String SUCCESSFULLY_ACTIVATED_CODE = "205";
    public final static String SUCCESSFULLY_ACTIVATED_DESC = "Activate successful";

    public final static String VOID_APPROVED_CODE = "206";
    public final static String VOID_APPROVED_DESC = "Void approved";

    public final static String QUERY_STATUS_CODE = "207";
    public final static String QUERY_STATUS_DESC = "Get transaction status success";

    public final static String GET_SDK_DETAILS_CODE = "208";
    public final static String GET_SDK_DETAILS_DESC = "Get SDK details success";

    // Failure return
    public final static String INITIALISED_FAILED_CODE = "300";
    public final static String INITIALISED_FAILED_DESC = "Initialise failed";

    public final static String REQUIRE_PERMISSION_CODE = "301";
    public final static  String REQUIRE_PERMISSION_DESC = "Require permission before proceed";

    public final static String PERMISSION_NOT_GRANTED_CODE = "302";
    public final static String PERMISSION_NOT_GRANTED_DESC = "Permission not granted";

    public final static String REQUIRE_ACTIVATION_CODE = "303";
    public final static String REQUIRE_ACTIVATION_DESC = "Require activation before proceed";

    public final static String REQUIRE_LOGIN_CODE = "304";
    public final static String REQUIRE_LOGIN_DESC = "Require login before proceed";

    public final static String SCAN_CANCELLED_CODE = "305";
    public final static String SCAN_CANCELLED_DESC = "Scan card has been cancelled";

    public final static String SCAN_FAILURE_CODE = "306";
    public final static String SCAN_FAILURE_DESC = "Card fails to be scanned";

    public final static String FATAL_EXCEPTION_CODE = "307";
    public final static String FATAL_EXCEPTION_DESC = "Fatal exception";

    public final static String MANUAL_INPUT_CODE = "308";
    public final static String MANUAL_INPUT_DESC = "Decided to proceed by manual input";

    public final static String UNHANDLED_EVENT_CODE = "309";
    public final static String UNHANDLED_EVENT_DESC = "Unhandled transaction UI event";

    public final static String TIME_OUT_CODE = "310";
    public final static String TIME_OUT_DESC = "Time out";

    public final static String NFC_NOT_SUPPORTED_CODE = "311";
    public final static String NFC_NOT_SUPPORTED_DESC = "NFC is not supported";

    public final static String NFC_NOT_ENABLED_CODE = "312";
    public final static String NFC_NOT_ENABLED_DESC = "NFC is not enabled";

    public final static String INTERNAL_ERROR_CODE = "999";
    public final static String INTERNAL_ERROR_DESC = "Internal error";

    // Event return
    public final static String INITIALISING_CODE = "E01";
    public final static String INITIALISING_DESC = "Initialising";

    public final static String PRESENT_CARD_CODE = "E02";
    public final static String PRESENT_CARD_DESC = "Present Card";

    public final static String CARD_PRESENTED_CODE = "E03";
    public final static String CARD_PRESENTED_DESC = "Card Presented";

    public final static String AUTHORISING_CODE = "E04";
    public final static String AUTHORISING_DESC = "Authorising";

    public final static String READ_CARD_OK_CODE = "E05";
    public final static String READ_CARD_OK_DESC = "Read Card OK";

    public final static String REQUEST_SIGNATURE_CODE = "E06";
    public final static String REQUEST_SIGNATURE_DESC = "Request Signature";

    public final static String UPLOAD_SIGNATURE_STARTED_CODE = "E07";
    public final static String UPLOAD_SIGNATURE_STARTED_DESC = "Upload Signature Started";

    public final static String CARD_TIME_OUT = "E08";
    public final static String CARD_TIME_OUT_DESC = "Read Card Time Out";

    public final static String CARD_FAILED = "E09";
    public final static String CARD_FAILED_DESC = "Card failed. Please try again";

    public final static String CARD_READ_ERROR_CODE = "E10";
    public final static String CARD_READ_ERROR_DESC = "Card Read Error";

    public final static String CARD_READ_DUPLICATED_CODE = "E11";
    public final static String CARD_READ_DUPLICATED_DESC = "Card Read Duplicate";

    public final static String CARD_TAG_NOT_SUPPORTED_CODE = "E12";
    public final static String CARD_TAG_NOT_SUPPORTED_DESC = "Card Tag Not Supported";

    public final static String CARD_READ_UNKNOWN_CODE = "E13";
    public final static String CARD_READ_UNKNOWN_DESC = "CardEvent: Unknown Event";

    public final static String PIN_REQUIRED_CODE = "E14";
    public final static String PIN_REQUIRED_DESC = "Pin required";

    public final static int REQUEST_CODE_START_INITIALISE = 10090;
    public final static int REQUEST_CODE_START_SCAN = 10092;
    public final static int REQUEST_CODE_STOP_SCAN = 10093;
    public final static int REQUEST_CODE_FINISH_SCAN = 10094;

    public final static String COTS_ID = "cotsId";
    public final static String SDK_VERSION = "sdkVersion";

    public final static String STATUS_CODE = "statusCode";
    public final static String STATUS_MESSAGE = "statusMessage";
    public final static String AMOUNT_AUTHORIZED = "amountAuthorized";
    public final static String APPROVAL_CODE = "approvalCode";
    public final static String TRANSACTION_ID = "transactionID";
    public final static String CARD_NO = "cardNo";
    public final static String CARD_TYPE = "cardType";
    public final static String CARDHOLDER_NAME = "cardholderName";
    public final static String REFERENCE_NO = "referenceNo";
    public final static String INVOICE_NO = "invoiceNo";
    public final static String ACQUIRER_ID = "acquirerID";
    public final static String AID = "aid";
    public final static String APPLICATION_CRYPTOGRAM = "applicationCryptogram";
    public final static String TERMINAL_VERIFICATION_RESULTS = "terminalVerificationResults";
    public final static String TRANSACTION_STATUS_INFO = "transactionStatusInfo";
    public final static String TRANSACTION_CERT = "transactionCert";
    public final static String MERCHANT_IDENTIFIER = "merchantIdentifier";
    public final static String TERMINAL_IDENTIFIER = "terminalIdentifier";
    public final static String CONTACTLESS_CVM_TYPE = "contactlessCVMType";
    public final static String RREF_NO = "rrefNo";
    public final static String TRACE_NO = "traceNo";
    public final static String TRANSACTION_DATE = "transactionDate";
    public final static String TRANSACTION_TIME = "transactionTime";
    public final static String POS_ENTRY_TYPE = "posEntryType";
    public final static String BATCH_NO = "batchNo";
    public final static String APPLICATION_LABEL = "applicationLabel";
    public final static String MERCHANT_CATEGORY_CODE = "merchantCategoryCode";
    public final static String CARD_EXPIRY = "cardExpiry";

    public final static String TAG = "TOPNEWROUTE";

    public final static int PERMISSIONS_REQUEST_LOCATION = 99;
    public final static int PERMISSION_REQUEST_PHONE = 1000;
    public final static String ERROR_CODE_ACTIVATION_REQUIRED = "14014";
    public final static String ERROR_CODE_LOGIN_REQUIRED = "14020";

    public final static String CARD_TYPE_VISA = "0";
    public final static String CARD_TYPE_MASTER = "1";
    public final static String CARD_TYPE_AMEX = "2";
    public final static String CARD_TYPE_JCB = "3";
    public final static String CARD_TYPE_UNIONPAY = "7";
    public final static String CARD_TYPE_DEBIT = "8";
    public final static String CARD_TYPE_TPN = "11";

    public final static String NO_CVM = "00";
    public final static String SIGNATURE = "01";
    public final static String ONLINE_PIN = "02";

    public final static String OPERATION_CODE = "operationCode";
    public final static String OPERATION_MSG = "operationMsg";
    public final static String ABORT_STATUS = "abortStatus";
}