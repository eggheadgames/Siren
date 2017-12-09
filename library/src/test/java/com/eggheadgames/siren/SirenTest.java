package com.eggheadgames.siren;

import android.app.Activity;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class SirenTest {
    private static final String APP_DESCRIPTION_URL = "http://example.com";

    @Mock
    Activity activity;

    @Mock
    SirenHelper sirenHelper;

    private SirenAlertWrapper alertWrapper;
    private Siren siren;
    private Map<String, String> httpParams;
    private long lastCheckDate = 0;

    @Before
    public void prepareTest() {

        siren = Mockito.spy(new Siren());
        httpParams = new HashMap<String, String>();
        httpParams.put("key","Value");
        alertWrapper = Mockito.spy(new SirenAlertWrapper(null, null, null, null, null, null));
        //Mock SirenHelper class
        Mockito.when(sirenHelper.getAlertMessage(Mockito.any(Context.class), Mockito.anyString(), Mockito.any(SirenSupportedLocales.class))).thenReturn("");
        Mockito.when(sirenHelper.getDaysSinceLastCheck(activity)).thenReturn(0);
        Mockito.when(sirenHelper.getLocalizedString(Mockito.any(Context.class), Mockito.anyInt(), Mockito.any(SirenSupportedLocales.class))).thenReturn("");
        Mockito.when(sirenHelper.getPackageName(activity)).thenReturn(TestConstants.appPackageName);
        Mockito.when(sirenHelper.getVersionCode(activity)).thenReturn(TestConstants.appVersionCode);
        Mockito.when(sirenHelper.getVersionName(activity)).thenReturn(TestConstants.appVersionName);
        Mockito.when(sirenHelper.isVersionSkippedByUser(Mockito.any(Context.class), Mockito.anyString())).thenReturn(false);
        Mockito.when(sirenHelper.isGreater(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return Integer.parseInt((String) invocation.getArguments()[0]) > Integer.parseInt((String) invocation.getArguments()[1]);
            }
        });

        Mockito.when(sirenHelper.isEquals(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return Integer.parseInt((String) invocation.getArguments()[0]) == Integer.parseInt((String) invocation.getArguments()[1]);
            }
        });
        Mockito.when(sirenHelper.isEmpty(Mockito.anyString())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return !(invocation.getArguments()[0] != null && !invocation.getArguments()[0].equals(""));
            }
        });
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                lastCheckDate = System.currentTimeMillis();
                return null;
            }
        }).when(sirenHelper).setLastVerificationDate(Mockito.any(Context.class));
        Mockito.when(sirenHelper.getLastVerificationDate(activity)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                return lastCheckDate;
            }
        });

        //Mock SirenAlertWrapper class
        Mockito.doNothing().when(alertWrapper).show();

        //Mock Siren instance
        siren.mApplicationContext = activity;

        Mockito.when(siren.getSirenHelper()).thenReturn(sirenHelper);
        Mockito.doReturn(alertWrapper).when(siren).getAlertWrapper(Mockito.any(SirenAlertType.class), Mockito.anyString());

        mockResult(TestConstants.jsonVersionNameMajorUpdate);
    }

    private void mockResult(final String forResult) {
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                siren.handleVerificationResults(forResult);
                return null;
            }
        }).when(siren).performVersionCheck(Mockito.anyString(),Mockito.anyMapOf(String.class,String.class));
    }

    @Test
    public void onCheckVersion_noExceptionsShouldBeThrown() {
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
    }

    @Test
    public void onEmptyJsonUrl_verificationShouldNotBePerformed() {
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, "",httpParams);
        Mockito.verify(siren, Mockito.never()).performVersionCheck(Mockito.anyString(),Mockito.anyMapOf(String.class,String.class));
    }

    @Test
    public void onEmptyJsonUrl_warningShouldBePrintedToTheLog() {
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, "",httpParams);
        Mockito.verify(sirenHelper, Mockito.times(1)).logError(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void onImmediateVerification_versionCheckShouldBePerformedEveryTime() {
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(1)).show();

        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(2)).show();

        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(3)).show();
    }

    @Test
    public void onDailyVerification_versionCheckShouldBePerformedOnceADay() {
        Mockito.when(sirenHelper.getDaysSinceLastCheck(activity)).thenReturn(0);
        siren.checkVersion(activity, SirenVersionCheckType.DAILY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(1)).show();

        //verification on the same day
        siren.checkVersion(activity, SirenVersionCheckType.DAILY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(1)).show();

        //next day verification
        Mockito.when(sirenHelper.getDaysSinceLastCheck(activity)).thenReturn(1);
        siren.checkVersion(activity, SirenVersionCheckType.DAILY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(2)).show();
    }

    @Test
    public void onWeeklyVerificationVersionCheckShouldBePerformedOnceAWeek() {
        Mockito.when(sirenHelper.getDaysSinceLastCheck(activity)).thenReturn(0);
        siren.checkVersion(activity, SirenVersionCheckType.WEEKLY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(1)).show();

        //same day verification
        siren.checkVersion(activity, SirenVersionCheckType.WEEKLY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(1)).show();

        //day 5 verification
        Mockito.when(sirenHelper.getDaysSinceLastCheck(activity)).thenReturn(5);
        siren.checkVersion(activity, SirenVersionCheckType.WEEKLY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(1)).show();

        //next week verification
        Mockito.when(sirenHelper.getDaysSinceLastCheck(activity)).thenReturn(7);
        siren.checkVersion(activity, SirenVersionCheckType.WEEKLY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(2)).show();
    }

    @Test
    public void onMalformedJson_verificationShouldBeIgnored() {
        mockResult(TestConstants.jsonMalformed);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.never()).show();

        mockResult(TestConstants.jsonMalformed2);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.never()).show();

        mockResult(TestConstants.jsonMalformed3);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.never()).show();
    }

    @Test
    public void onVersionCodeUpdate_dialogShouldBeDisplayed() {
        mockResult(TestConstants.jsonVersionCodeUpdate);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(1)).show();
    }

    @Test
    public void onOutdatedVersionCode_dialogShouldNotBeShown() {
        mockResult(TestConstants.jsonVersionCodeOutdated);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.never()).show();
    }

    @Test
    public void onVersionNameUpdate_dialogShouldBeDisplayed() {
        mockResult(TestConstants.jsonVersionNameMajorUpdate);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);

        mockResult(TestConstants.jsonVersionNameMinorUpdate);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);

        mockResult(TestConstants.jsonVersionNamePatchUpdate);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);

        mockResult(TestConstants.jsonVersionNameRevisionUpdate);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);

        Mockito.verify(alertWrapper, Mockito.times(4)).show();
    }

    @Test
    public void onOutdatedVersionName_dialogShouldNotBeDisplayed() {
        mockResult(TestConstants.jsonVersionNameOutdated);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);

        Mockito.verify(alertWrapper, Mockito.never()).show();
    }

    @Test
    public void onNoneAlertType_dialogShouldNotBeDisplayed() {
        siren.setMajorUpdateAlertType(SirenAlertType.NONE);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);

        Mockito.verify(alertWrapper, Mockito.never()).show();
    }

    @Test
    public void onVersionNameSkipped_dialogShouldNotBeShownFurther() {
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(1)).show();

        Mockito.when(sirenHelper.isVersionSkippedByUser(Mockito.any(Context.class), Mockito.anyString())).thenReturn(true);

        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(1)).show();
    }

    @Test
    public void onVersionCodeSkipped_dialogShouldNotBeShownFurther() {
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(1)).show();

        mockResult(TestConstants.jsonVersionCodeUpdate);
        Mockito.when(sirenHelper.isVersionSkippedByUser(Mockito.any(Context.class), Mockito.anyString())).thenReturn(true);

        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.times(1)).show();
    }

    @Test
    public void onVersionNameCountNotEquals_verificationShouldBeSkipped() {
        Mockito.when(sirenHelper.getVersionName(activity)).thenReturn("1.1.1");
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(alertWrapper, Mockito.never()).show();
    }

    @Test
    public void onVerificationFailure_listenerShouldBeTriggered() {
        mockResult(TestConstants.jsonMalformed);

        ISirenListener listener = Mockito.mock(ISirenListener.class);
        siren.setSirenListener(listener);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(listener, Mockito.times(1)).onError(Mockito.any(Exception.class));
    }

    @Test
    public void onNoneAlertType_listenerShouldBeTriggered() {
        ISirenListener listener = Mockito.mock(ISirenListener.class);
        siren.setSirenListener(listener);
        siren.setMajorUpdateAlertType(SirenAlertType.NONE);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(listener, Mockito.times(1)).onDetectNewVersionWithoutAlert(Mockito.anyString());
    }

    @Test
    public void onVersionCodeUpdate_checkVersionCodeAlertType() {
        mockResult(TestConstants.jsonVersionCodeUpdate);

        siren.setVersionCodeUpdateAlertType(SirenAlertType.FORCE);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);
        Mockito.verify(siren).getAlertWrapper(eq(SirenAlertType.FORCE), Mockito.anyString());
    }

    @Test
    public void onVersionUpdateWithFreshInstall_shouldNotFireAllertIfVersionUpToDate() {
        mockResult(TestConstants.jsonVersionNameOutdatedTest);
        Mockito.when(sirenHelper.getVersionName(activity)).thenReturn(TestConstants.appVersionNameTest);

        ISirenListener listener = Mockito.mock(ISirenListener.class);
        siren.setSirenListener(listener);
        siren.setRevisionUpdateAlertType(SirenAlertType.NONE);

        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);

        Mockito.verify(listener, Mockito.never()).onDetectNewVersionWithoutAlert(Mockito.anyString());
    }

    @Test
    public void onVersionCheckDisabled_shouldNotDoVersionCheck() {
        mockResult(TestConstants.jsonVersionCheckDisabled);
        Mockito.when(sirenHelper.getVersionName(activity)).thenReturn(TestConstants.appVersionNameTest);

        ISirenListener listener = Mockito.mock(ISirenListener.class);
        siren.setSirenListener(listener);
        siren.setMajorUpdateAlertType(SirenAlertType.NONE);
        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);

        Mockito.verify(listener, Mockito.never()).onDetectNewVersionWithoutAlert(Mockito.anyString());
    }

    @Test
    public void onForceUpdateEnabled_shouldShowForceAlertType() {
        mockResult(TestConstants.jsonForceUpdateEnabled);
        Mockito.when(sirenHelper.getVersionName(activity)).thenReturn(TestConstants.appVersionNameTest);

        siren.checkVersion(activity, SirenVersionCheckType.IMMEDIATELY, APP_DESCRIPTION_URL,httpParams);

        Mockito.verify(siren).getAlertWrapper(eq(SirenAlertType.FORCE), Mockito.anyString());
    }
}