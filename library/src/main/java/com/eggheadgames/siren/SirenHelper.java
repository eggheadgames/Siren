package com.eggheadgames.siren;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

class SirenHelper {
    private static final SirenHelper instance = new SirenHelper();

    @NonNull
    public static SirenHelper getInstance() {
        return instance;
    }

    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    protected SirenHelper() {
        // visible for testing
    }


    String getPackageName(Context context) {
        return context.getPackageName();
    }

    int getDaysSinceLastCheck(Context context) {
        long lastCheckTimestamp = getLastVerificationDate(context);

        if (lastCheckTimestamp > 0) {
            return (int) (TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTimeInMillis()) - TimeUnit.MILLISECONDS.toDays(lastCheckTimestamp));
        } else {
            return 0;
        }
    }

    int getVersionCode(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(getPackageName(context), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isVersionSkippedByUser(Context context, String minAppVersion) {
        String skippedVersion = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREFERENCES_SKIPPED_VERSION, "");
        return skippedVersion.equals(minAppVersion);
    }

    void setLastVerificationDate(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong(Constants.PREFERENCES_LAST_CHECK_DATE, Calendar.getInstance().getTimeInMillis())
                .commit();
    }

    long getLastVerificationDate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(Constants.PREFERENCES_LAST_CHECK_DATE, 0);
    }

    @NonNull
    String getAlertMessage(Context context, String minAppVersion, SirenSupportedLocales locale) {
        try {
            if (context.getApplicationInfo().labelRes == 0) {
                return String.format(getLocalizedString(context, R.string.update_alert_message, locale), getLocalizedString(context, R.string.fallback_app_name, locale), minAppVersion);
            } else {
                return String.format(getLocalizedString(context, R.string.update_alert_message, locale), getLocalizedString(context, context.getApplicationInfo().labelRes, locale), minAppVersion);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return context.getString(R.string.update_alert_message, context.getString(R.string.fallback_app_name), minAppVersion);
        }
    }

    @NonNull
    String getLocalizedString(Context context, int stringResource, SirenSupportedLocales locale) {
        if (context == null) {
            return "";
        }
        if (locale == null) {
            return context.getString(stringResource);
        } else {
            Resources standardResources = context.getResources();
            AssetManager assets = standardResources.getAssets();
            DisplayMetrics metrics = standardResources.getDisplayMetrics();
            Configuration defaultConfiguration = standardResources.getConfiguration();
            Configuration newConfiguration = new Configuration(defaultConfiguration);
            newConfiguration.locale = new Locale(locale.getLocale());
            String string = new Resources(assets, metrics, newConfiguration).getString(stringResource);

            //need to turn back the default locale
            new Resources(assets, metrics, defaultConfiguration);
            return string;
        }
    }


    void openGooglePlay(Activity activity) {
        if (activity == null) {
            return;
        }
        final String appPackageName = getPackageName(activity);
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    void setVersionSkippedByUser(Context context, String skippedVersion) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(Constants.PREFERENCES_SKIPPED_VERSION, skippedVersion)
                .commit();
    }

    String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(getPackageName(context), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    boolean isGreater(String first, String second) {
        return TextUtils.isDigitsOnly(first) && TextUtils.isDigitsOnly(second) && Integer.parseInt(first) > Integer.parseInt(second);
    }

    boolean isEquals(String first, String second) {
        return TextUtils.isDigitsOnly(first) && TextUtils.isDigitsOnly(second) && Integer.parseInt(first) == Integer.parseInt(second);
    }

    boolean isEmpty(String appDescriptionUrl) {
        return TextUtils.isEmpty(appDescriptionUrl);
    }

    public void logError(String tag, String message) {
        Log.d(tag, message);
    }
}
