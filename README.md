[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Siren-green.svg?style=true)](https://android-arsenal.com/details/1/3379)
[![Circle CI](https://circleci.com/gh/eggheadgames/Siren.svg?style=svg)](https://circleci.com/gh/eggheadgames/Siren)
[![Release](https://jitpack.io/v/eggheadgames/Siren.svg)](https://jitpack.io/#eggheadgames/Siren)

# Siren for Android

### Notify users when a new version of your Android app is available, and prompt them with the Play Store link.

This is a port of the iOS library of the same name: https://github.com/ArtSabintsev/Siren

## About

Siren checks a user's currently installed version of your Android app against the latest version information stored in a JSON file on a server URL you provide. (_Unfortunately, the Google public API for version checking requires a token, and due to logistics and rate limiting, it's not feasible to use the API from an Android app_).

If a new version is available, an alert can be presented to the user informing them of the newer version, and giving them the option to update the application. Alternatively, Siren can notify your app programmatically, enabling you to inform the user through alternative means, such as a custom interface.

* Siren is built to work with semantic version numbering to provide a range of update suggestions for your customers
* Siren is a Java language port of a [Siren](https://github.com/ArtSabintsev/Siren), an iOS Swift library that achieves the same functionality
* Siren is actively maintained by [Egghead Games](http://eggheadgames.com) for their cross-platform mobile/tablet apps ([great mind puzzles with no ads](https://play.google.com/store/apps/dev?id=8905223606155014113)!)

## Features
- [x] Gradle support (using [JitPack](https://jitpack.io/))
- [x] Localized for 20+ languages (See **Localization** Section)
- [x] Three types of alerts (see **Screenshots & Alert Types**)
- [x] Optional override methods (see **Optional Override** section)
- [x] Accompanying [sample Android app](https://github.com/eggheadgames/SirenSample/)

## Similar Android libraries

Choose what works best for your scenario. We chose _not_ to "screenscrape" the Google Play Listing. We've kept the prompt & update scenarios simple. We kept close to the iOS Siren library to keep our iOS & Android apps similar. Other Android solutions: [AppUpdater](https://github.com/javiersantos/AppUpdater) (the most comprehensive and feature rich library, including support for checks at Amazon and FDroid), [Gandalf](https://github.com/btkelly/gandalf) (also has a "companion" iOS solution), [Update Checker](https://github.com/rampo/UpdateChecker), [Fit](https://github.com/KeithYokoma/Fit) (callback framework, no UI),

## Screenshots & Alert Types

- The **left picture** forces the user to update the app.
- The **center picture** gives the user the option to update the app.
- The **right picture** gives the user the option to skip the current update.
- These options are controlled by the `SirenAlertType` enum.

![Force update](/extras/force.png)
![Optional update](/extras/option.png)
![Skip update](/extras/skip.png)


## Setup

A minimal usage is to add the following to the `onCreate` of your main activity.
This will check at most once a day for a new version and give the user the option to choose "Update" or "Next time".

```java
private static final String SIREN_JSON_URL = "https://example.com/com.mycompany.myapp/version.json";

Siren siren = Siren.getInstance(getApplicationContext());
siren.checkVersion(this, SirenVersionCheckType.DAILY, SIREN_JSON_URL);
```

## Installation Instructions
Add the JitPack.io repository to your root `build.gradle`:

```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

Add a dependency to your application related `build.gradle`

```gradle
dependencies {
    compile 'com.github.eggheadgames:Siren:1.4.+'
}
```

Host a Json document with a public access that will describe your application package name and current application version.

```json
{ "com.example.app": { "minVersionName": "1.12.2" } }
```
OR
```json
{ "com.example.app": { "minVersionCode": 7 } }
```

Parameters supported on the JSON document:

- **minVersionName**: The minimum version name required.
- **minVersionCode**: The minimum version code required, minVersionName will take precendence if both specified.
- **enable**: A boolean flag to remotely toggle the version check feature.
- **force**: A boolean flag to remotely set alertType as FORCE on every type of update.

Example:
```json
{ "com.example.app": { "minVersionCode": 7, "enable": true, "force": false } }
```

## Options

The **SirenVersionCheckType** controls how often the server is checked for a new version, and hence how often the user will be prompted. You can set it to `IMMEDIATELY`, `DAILY` or `WEEKLY`.

You can also define the dialog appearance and behaviour by setting **SirenAlertType** to react according to your version increment per [Semantic Versioning](http://semver.org/). The default is `SirenAlertType.OPTION`. This generates a 2 button "Next Time" or "Update" alert. Other values are `FORCE`, `SKIP` and `NONE`. `NONE` will not display an alert, but will call your listener with appropriate text to display. See **Example** below.

You can combine these options to have different behaviour for different version changes. For example, you might will force a user to upgrade for a major version change (e.g. 1.x.x to 2.x.x), give them a "Next time" option for a minor version change (e.g. 1.2.x to 1.3.x) and add a 3rd "Skip this version" option for a 3rd or 4th level change (e.g. 1.2.5 to 1.2.6).

As well as the levels: Major, Minor, Patch and Revision, you can also set messages based on the `versionCode` of your app by using a `minVersionCode` field instead of `minVersionName`.

The following code shows how you can display "stricter" dialogs based on the version severity, with no dialog displayed for a `versionCode` change:

```java
        Siren siren = Siren.getInstance(getApplicationContext());
        siren.setMajorUpdateAlertType(SirenAlertType.FORCE);
        siren.setMinorUpdateAlertType(SirenAlertType.OPTION);
        siren.setPatchUpdateAlertType(SirenAlertType.SKIP);
        siren.setRevisionUpdateAlertType(SirenAlertType.NONE);
        siren.checkVersion(this, SirenVersionCheckType.IMMEDIATELY, SIREN_JSON_DOCUMENT_URL);
```

If you'd like to just use `versionCode` for changes, you could check every time and force an update using code like this:

```java
        Siren siren = Siren.getInstance(getApplicationContext());
        siren.setVersionCodeUpdateAlertType(SirenAlertType.FORCE);
        siren.checkVersion(this, SirenVersionCheckType.IMMEDIATELY, SIREN_JSON_DOCUMENT_URL);
```


## Example

Some developers may want to display a less obtrusive custom interface, like a banner or small icon.
You may also wish to control which level of update to force a user to update vs deferring to later.

You can find a fully functional sample project at https://github.com/eggheadgames/SirenSample.

Here is a code sample that shows how to handle all the alerts yourself:

```java
    private void checkCurrentAppVersion() {
        Siren siren = Siren.getInstance(getApplicationContext());
        siren.setSirenListener(sirenListener);
        siren.setMajorUpdateAlertType(SirenAlertType.NONE);
        siren.setMinorUpdateAlertType(SirenAlertType.NONE);
        siren.setPatchUpdateAlertType(SirenAlertType.NONE);
        siren.setRevisionUpdateAlertType(SirenAlertType.NONE);
        siren.setVersionCodeUpdateAlertType(SirenAlertType.NONE);
        siren.checkVersion(this, SirenVersionCheckType.IMMEDIATELY, SIREN_JSON_DOCUMENT_URL);
    }

    ISirenListener sirenListener = new ISirenListener() {
        @Override
        public void onShowUpdateDialog() {
            Log.d(TAG, "onShowUpdateDialog");
        }

        @Override
        public void onLaunchGooglePlay() {
            Log.d(TAG, "onLaunchGooglePlay");
        }

        @Override
        public void onSkipVersion() {
            Log.d(TAG, "onSkipVersion");
        }

        @Override
        public void onCancel() {
            Log.d(TAG, "onCancel");
        }

        @Override
        public void onDetectNewVersionWithoutAlert(String message) {
            Log.d(TAG, "onDetectNewVersionWithoutAlert: " + message);
        }

        @Override
        public void onError(Exception e) {
            Log.d(TAG, "onError");
            e.printStackTrace();
        }
    };
```

## Localization

Siren is localized for Arabic, Armenian, Basque, Chinese (Simplified), Chinese (Traditional), Danish, Dutch, English, Estonian, French, German, Greek, Hebrew, Hungarian, Italian, Japanese, Korean, Latvian, Lithuanian, Malay, Polish, Portuguese (Brazil), Portuguese (Portugal), Russian, Slovenian, Swedish, Spanish, Thai, and Turkish.

You may want the update dialog to always appear in a certain language, ignoring Android's language setting. This is supported with the `setLanguageLocalization` method. For example, you can force a French update dialog by setting the locale as follows:

```java
    Siren.setLanguageLocalization(SirenSupportedLocales.FR)
```

## Support for HTTP Headers:

If you have more than one supported APK in production based on the min supported API Level, you may want to send a different response from the server for the different version of the app for the same URL. you can add few HTTP header to installed version and get the appropriate data for that version from the server. If you have a paid app and a free app as different product flavours with same code base. You can pass the application name as a header and backend server will send return correct response for that app.

Example for App 'com.example.app' with app version 3.x.y with API level 11 and above
```java
        Map<String, String> httpParams = new HashMap<String, String>();
        httpParams.put("appVersion","3.15.4");
        httpParams.put("appName","com.example.app");
        siren.checkVersion(this, SirenVersionCheckType.IMMEDIATELY, SIREN_JSON_DOCUMENT_URL,httpParams);
```
Response from server

```json
{ "com.example.app": { "minVersionName": "3.8.2" } }
```

Example for App 'com.example.app.paid' with app version 3.x.y with API level 11 and above
```java
        Map<String, String> httpParams = new HashMap<String, String>();
        httpParams.put("appVersion","3.11.4");
        httpParams.put("appName","com.example.app.paid");
        siren.checkVersion(this, SirenVersionCheckType.IMMEDIATELY, SIREN_JSON_DOCUMENT_URL,httpParams);
```
Response from server

```json
{ "com.example.app.paid": { "minVersionName": "3.10.1" } }
```


Example for App version 4.x.y with API level 16 and above
```java
        Map<String, String> httpParams = new HashMap<String, String>();
        httpParams.put("appVersion","4.20.4");
        siren.checkVersion(this, SirenVersionCheckType.IMMEDIATELY, SIREN_JSON_DOCUMENT_URL,httpParams);
```
Response from server

```json
{ "com.example.app": { "minVersionName": "4.12.2" } }
```
## custom message from server.

You can not send custom message server side. e.g if the user language is currently not supported by library, you can send the message from server.

Example you want to display a message in Hindi or any other language, the app can send the language it is expecting as a header parameter and server can send the response in the requested landuage.
```java
        Map<String, String> httpParams = new HashMap<String, String>();
        httpParams.put("appVersion","4.20.4");
        httpParams.put("lang","hindi");
        siren.checkVersion(this, SirenVersionCheckType.IMMEDIATELY, SIREN_JSON_DOCUMENT_URL,httpParams);
```
Response from server

```json
{ "com.example.app": { "minVersionName": "4.12.2","message" : "कृपया नवीनतम वर्शन में ऐप अपडेट करें" } }
```

## Testing Siren

Change the url in your app to point to a test location (e.g. http://myjson.com/ is a convenient test site). Create an appropriate file and run your app with the temporary url.

For example, if my app's current version is `2.1.0.0` and the `applicationId` is `com.eggheadgames.sirensample`, I could create a json file with contents:
```json
{"com.eggheadgames.sirensample":{"minVersionName":"2.2.1.1"}}
```
Then, add code like the following in the `onCreate()` of my app's home page:
```java
    private Siren siren = Siren.getInstance(getApplicationContext());
    siren.checkVersion(this, SirenVersionCheckType.IMMEDIATELY, "https://api.myjson.com/bins/198mf");
```
Running this should show an update dialog when I start the app.  Of course using a value other than `IMMEDIATELY` may not bring an immediate prompt, unless your `SharedPreferences` are being deleted between attempts.

