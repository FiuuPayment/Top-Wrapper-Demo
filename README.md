# Fiuu Tap-on-Phone tester

Sample source for Fiuu Tap-on-Phone (ToP).

This public repo is a **reference app**: it shows how to call the ToP wrapper (amount entry, tap sale, PIN, signature, void). The `.aar` and `.jar` libraries are provided by Fiuu after you register; they are not in this repo. Copy them into `app/libs/` before you build.

Build this tester on a real NFC device with **your** App ID and credentials. When a tap sale works here, you can apply the same flow in your own merchant app.

## Libraries

The `.aar` and `.jar` files are provided by Fiuu. They are not in this public repo.

The Gradle build reads these files from `app/libs/`:

```
app/libs/
  topnewroute-debug.aar      # debug builds
  topnewroute-release.aar    # release builds
  mobile-pogengine.aar
  filter-ssmobile.jar
```

Fiuu sends these as a zip after registration. Extract the zip, then copy the `.aar` and `.jar` files into `app/libs/`:

```bash
unzip fiuu-top-libs.zip -d /tmp/top-libs
cp /tmp/top-libs/*.aar /tmp/top-libs/*.jar app/libs/
```

Use the zip Fiuu sent you; the filename may differ. `app/libs/` must contain the four files above before you sync or build.

## What you can try

| Flow | How |
| --- | --- |
| Sale | Enter an amount on the keypad, tap **Charge**, present a contactless card |
| PIN | Cards / amounts that require online PIN (native PIN pad from the SDK) |
| Signature | Cards / amounts that require signature (in-app pad, then submit to the SDK) |
| Abort | Cancel on the tap-card dialog |
| Void | **Void** on the home screen; fields are prefilled from the last approved sale |
| SDK logs | Expand **Developer log** on the home screen |

Amounts are Malaysian Ringgit. The keypad stores cents (max `99999.99`). The SDK receives the amount in cents.

## Requirements

- Android Studio with AGP 8.6 and Gradle 8.9 (JDK 17)
- Physical Android device, API 29+
- NFC enabled (emulator is not useful for tap)
- The `.aar` and `.jar` libraries provided by Fiuu (after registration)
- Your ToP credentials
- A keystore whose cert matches the App ID you are testing

App ID, signing cert, and credentials must belong to the same app.

## Setup

```bash
git clone https://github.com/FiuuPayment/Tap-on-Phone.git
cd Tap-on-Phone
```

### 1. Libraries

Copy the `.aar` and `.jar` files provided by Fiuu into `app/libs/` (see [Libraries](#libraries)).

### 2. Credentials

```bash
cp secrets.properties.example secrets.properties
```

Fill in the values from your ToP provisioning sheet:

| Key | Used for |
| --- | --- |
| `uniqueId` | Merchant / terminal unique ID |
| `vKey` | SDK init |
| `attestationHost` / `hostCertPinning` | Attestation |
| `keyloadingHost` / `keyloadingCertPinning` / `cert` | Key loading |
| `libAccessKey` / `libSecretKey` | Library auth |

Gradle fails the build if `secrets.properties` is missing.

### 3. App ID

In `app/build.gradle`, set `applicationId` to **your** merchant App ID. The sample value `com.fiuu.toppayment` is only a placeholder.

The Gradle `namespace` stays `com.fiuu.toppayment.app`. That avoids a `BuildConfig` clash with the ToP AAR. It is not your App ID.

### 4. Signing

```bash
cp key.properties.example key.properties
```

Point `storeFile` at the keystore for that App ID and set `storePassword`, `keyPassword`, and `keyAlias`. Debug and release both use this signing config.

### 5. Run

1. Open the project in Android Studio.
2. Let Gradle sync.
3. Connect a device with NFC on.
4. Run the `app` configuration (debug).

```bash
./gradlew :app:installDebug
```

On first launch the app asks for location (required by the reader SDK), initialises ToP, and logs in with `uniqueId`. Watch the developer log if init or login fails.

## Sample structure

```
MainActivity  →  FasstapManager  →  TapSDK (from app/libs)
```

```
app/src/main/java/com/fiuu/toppayment/
  MainActivity.java          Amount keypad, tap dialog, void dialog
  FasstapManager.java        SDK init and transaction calls
  SignatureActivity.java     Signature capture
  SuccessPayment.java        Approved sale / void
  FailPayment.java           Declined / error
  DevLog*.java               On-screen SDK log panel
```

Debug builds run the SDK in non-production mode; release builds set production mode.
