package com.eggheadgames.siren;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 *JSON format should be the following
 * {
 *     "com.example.app": {
 *         "minVersionName": "1.0.0.0"
 *     }
 * }
 *
 * OR
 *
 * {
 *     "com.example.app": {
 *         "minVersionCode": 7,
 *     }
 * }
 */

@SuppressWarnings({"WeakerAccess", "unused", "PMD.GodClass"})
public class Siren {

    @VisibleForTesting
    protected static final Siren sirenInstance = new Siren();
    @VisibleForTesting
    protected Context mApplicationContext;
    private ISirenListener mSirenListener;
    private WeakReference<Activity> mActivityRef;

    /**
     * Determines alert type during version code verification
     */
    private SirenAlertType versionCodeUpdateAlertType = SirenAlertType.OPTION;

    /**
     * Determines the type of alert that should be shown for major version updates: A.b.c
     */
    private SirenAlertType majorUpdateAlertType = SirenAlertType.OPTION;

    /**
     * Determines the type of alert that should be shown for minor version updates: a.B.c
     */
    private SirenAlertType minorUpdateAlertType  = SirenAlertType.OPTION;

    /**
     Determines the type of alert that should be shown for minor patch updates: a.b.C
     */
    private SirenAlertType patchUpdateAlertType = SirenAlertType.OPTION;

    /**
     Determines the type of alert that should be shown for revision updates: a.b.c.D
     */
    private SirenAlertType revisionUpdateAlertType = SirenAlertType.OPTION;

    /**
     Overrides the default localization of a user's device when presenting the update message and button titles in the alert.
     */
    private SirenSupportedLocales forceLanguageLocalization = null;
    /**
     * @param context - you should use an Application mApplicationContext here in order to not cause memory leaks
     */
    public static Siren getInstance(Context context) {
        sirenInstance.mApplicationContext = context;
        return sirenInstance;
    }

    @VisibleForTesting
    protected Siren() {
        // visible for testing
    }

    public void checkVersion(Activity activity, SirenVersionCheckType versionCheckType, String appDescriptionUrl) {

        mActivityRef = new WeakReference<>(activity);

        if (getSirenHelper().isEmpty(appDescriptionUrl)) {
            getSirenHelper().logError(getClass().getSimpleName(), "Please make sure you set correct path to app version description document");
            return;
        }

        if (versionCheckType == SirenVersionCheckType.IMMEDIATELY) {
            performVersionCheck(appDescriptionUrl);
        } else if (versionCheckType.getValue() <= getSirenHelper().getDaysSinceLastCheck(mApplicationContext)
                ||getSirenHelper().getLastVerificationDate(mApplicationContext) == 0) {
            performVersionCheck(appDescriptionUrl);
        }
    }

    public void setMajorUpdateAlertType(@SuppressWarnings("SameParameterValue") SirenAlertType majorUpdateAlertType) {
        this.majorUpdateAlertType = majorUpdateAlertType;
    }

    public void setMinorUpdateAlertType(SirenAlertType minorUpdateAlertType) {
        this.minorUpdateAlertType = minorUpdateAlertType;
    }

    public void setPatchUpdateAlertType(SirenAlertType patchUpdateAlertType) {
        this.patchUpdateAlertType = patchUpdateAlertType;
    }

    public void setRevisionUpdateAlertType(SirenAlertType revisionUpdateAlertType) {
        this.revisionUpdateAlertType = revisionUpdateAlertType;
    }

    public void setSirenListener(ISirenListener sirenListener) {
        this.mSirenListener = sirenListener;
    }

    public void setVersionCodeUpdateAlertType(SirenAlertType versionCodeUpdateAlertType) {
        this.versionCodeUpdateAlertType = versionCodeUpdateAlertType;
    }

    public void setLanguageLocalization(SirenSupportedLocales localization) {
        forceLanguageLocalization = localization;
    }

    @VisibleForTesting
    protected void performVersionCheck(String appDescriptionUrl) {
        new LoadJsonTask().execute(appDescriptionUrl);
    }

    @VisibleForTesting
    protected void handleVerificationResults(String json) {
        try {
            JSONObject rootJson = new JSONObject(json);

            if (rootJson.isNull(getSirenHelper().getPackageName(mApplicationContext))) {
                throw new JSONException("field not found");
            } else {
                JSONObject appJson = rootJson.getJSONObject(getSirenHelper().getPackageName(mApplicationContext));

                //version name have higher priority then version code
                if (checkVersionName(appJson)) {
                    return;
                }

                checkVersionCode(appJson);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            if (mSirenListener != null) {
                mSirenListener.onError(e);
            }
        }
    }

    @VisibleForTesting
    protected SirenAlertWrapper getAlertWrapper(SirenAlertType alertType, String appVersion) {
        Activity activity = mActivityRef.get();
        return new SirenAlertWrapper(activity, mSirenListener, alertType, appVersion, forceLanguageLocalization, getSirenHelper());
    }

    protected SirenHelper getSirenHelper() {
        return SirenHelper.getInstance();
    }

    private boolean checkVersionName(JSONObject appJson) throws JSONException{
        if (appJson.isNull(Constants.JSON_MIN_VERSION_NAME)) {
            return false;
        }
        getSirenHelper().setLastVerificationDate(mApplicationContext);

        // If no config found, assume version check is enabled
        Boolean versionCheckEnabled = appJson.has(Constants.JSON_ENABLE_VERSION_CHECK) ? appJson.getBoolean(Constants.JSON_ENABLE_VERSION_CHECK) : true;
        if (!versionCheckEnabled) {
            return false;
        }

        // If no config found, assume force update = false
        Boolean forceUpdateEnabled = appJson.has(Constants.JSON_FORCE_ALERT_TYPE) ? appJson.getBoolean(Constants.JSON_FORCE_ALERT_TYPE) : false;
        String minVersionName = appJson.getString(Constants.JSON_MIN_VERSION_NAME);
        String currentVersionName = getSirenHelper().getVersionName(mApplicationContext);

        if (getSirenHelper().isEmpty(minVersionName) || getSirenHelper().isEmpty(currentVersionName) || getSirenHelper().isVersionSkippedByUser(mApplicationContext, minVersionName)) {
            return false;
        }

        SirenAlertType alertType = null;
        String[] minVersionNumbers = minVersionName.split("\\.");
        String[] currentVersionNumbers = currentVersionName.split("\\.");
        //noinspection ConstantConditions
        if (minVersionNumbers != null && currentVersionNumbers != null
                && minVersionNumbers.length == currentVersionNumbers.length) {

            Boolean versionUpdateDetected = false;
            for (Integer index = 0; index < Math.min(minVersionNumbers.length, currentVersionNumbers.length); index++) {
                Integer compareResult = checkVersionDigit(minVersionNumbers, currentVersionNumbers, index);
                if (compareResult == 1) {
                    versionUpdateDetected = true;
                    if (forceUpdateEnabled) {
                        alertType = SirenAlertType.FORCE;
                    } else {
                        switch (index) {
                            case 0: alertType = majorUpdateAlertType; break;
                            case 1: alertType = minorUpdateAlertType; break;
                            case 2: alertType = patchUpdateAlertType; break;
                            case 3: alertType = revisionUpdateAlertType; break;
                            default: alertType = SirenAlertType.OPTION; break;
                        }
                    }
                    break;
                } else if (compareResult == -1) {
                    return false;
                }
            }

            if (versionUpdateDetected) {
                showAlert(minVersionName, alertType);
                return true;
            }
        }
        return false;
    }

    private int checkVersionDigit(String[] minVersionNumbers, String[] currentVersionNumbers, int digitIndex) {
        if (minVersionNumbers.length > digitIndex) {
            if (getSirenHelper().isGreater(minVersionNumbers[digitIndex], currentVersionNumbers[digitIndex])) {
                return 1;
            } else if (getSirenHelper().isEquals(minVersionNumbers[digitIndex], currentVersionNumbers[digitIndex])) {
                return 0;
            }
        }
        return -1;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean checkVersionCode(JSONObject appJson) throws JSONException{
        if (!appJson.isNull(Constants.JSON_MIN_VERSION_CODE)) {
            int minAppVersionCode = appJson.getInt(Constants.JSON_MIN_VERSION_CODE);
            // If no config found, assume version check is enabled
            Boolean versionCheckEnabled = appJson.has(Constants.JSON_ENABLE_VERSION_CHECK) ? appJson.getBoolean(Constants.JSON_ENABLE_VERSION_CHECK) : true;
            if (!versionCheckEnabled) {
                return false;
            }

            // If no config found, assume force update = false
            Boolean forceUpdateEnabled = appJson.has(Constants.JSON_FORCE_ALERT_TYPE) ? appJson.getBoolean(Constants.JSON_FORCE_ALERT_TYPE) : false;

            //save last successful verification date
            getSirenHelper().setLastVerificationDate(mApplicationContext);

            if (getSirenHelper().getVersionCode(mApplicationContext) < minAppVersionCode
                    && !getSirenHelper().isVersionSkippedByUser(mApplicationContext, String.valueOf(minAppVersionCode))) {
                showAlert(String.valueOf(minAppVersionCode), forceUpdateEnabled ? SirenAlertType.FORCE : versionCodeUpdateAlertType);
                return true;
            }
        }
        return false;
    }

    private void showAlert(String appVersion, SirenAlertType alertType) {
        if (alertType == SirenAlertType.NONE) {
            if (mSirenListener != null) {
                mSirenListener.onDetectNewVersionWithoutAlert(getSirenHelper().getAlertMessage(mApplicationContext, appVersion, forceLanguageLocalization));
            }
        } else {
            getAlertWrapper(alertType, appVersion).show();
        }
    }

    private static class LoadJsonTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            try {
                TLSSocketFactory TLSSocketFactory = new TLSSocketFactory();
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setUseCaches(false);
                connection.setAllowUserInteraction(false);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                if ("https".equalsIgnoreCase(url.getProtocol())) {
                    ((HttpsURLConnection)connection).setSSLSocketFactory(TLSSocketFactory);
                }
                connection.connect();
                int status = connection.getResponseCode();

                switch (status) {
                    case 200:
                    case 201:
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (isCancelled()) {
                                br.close();
                                connection.disconnect();
                                return null;
                            }
                            sb.append(line).append('\n');
                        }
                        br.close();
                        return sb.toString();
                    default: /* ignore unsuccessful results */
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                if (Siren.sirenInstance.mSirenListener != null) {
                    Siren.sirenInstance.mSirenListener.onError(ex);
                }

            } catch (Exception ex) {
              ex.printStackTrace();
            } finally {
                if (connection != null) {
                    try {
                        connection.disconnect();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        if (Siren.sirenInstance.mSirenListener != null) {
                            Siren.sirenInstance.mSirenListener.onError(ex);
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (Siren.sirenInstance.getSirenHelper().isEmpty(result)) {
                if (Siren.sirenInstance.mSirenListener != null) {
                    Siren.sirenInstance.mSirenListener.onError(new NullPointerException());
                }
            } else {
                Siren.sirenInstance.handleVerificationResults(result);
            }
        }
    }
}
