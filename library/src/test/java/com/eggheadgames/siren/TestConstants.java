package com.eggheadgames.siren;

interface TestConstants {

    String appVersionName = "1.1.1.1";
    String appVersionNameTest = "0.0.1.0";
    int appVersionCode = 1;
    String appPackageName = "com.example.app";

    String jsonVersionNameMajorUpdate = "{\"com.example.app\":{\"minVersionName\":\"2.1.1.1\"}}";
    String jsonVersionNameMinorUpdate = "{\"com.example.app\":{\"minVersionName\":\"1.2.1.1\"}}";
    String jsonVersionNamePatchUpdate = "{\"com.example.app\":{\"minVersionName\":\"1.1.2.1\"}}";
    String jsonVersionNameOutdated = "{\"com.example.app\":{\"minVersionName\":\"0.0.0.0\"}}";
    String jsonVersionNameOutdatedTest = "{\"com.example.app\":{\"minVersionName\":\"0.0.0.1\"}}";

    String jsonVersionNameRevisionUpdate = "{\"com.example.app\":{\"minVersionName\":\"1.1.1.2\"}}";

    String jsonVersionCodeUpdate = "{\"com.example.app\":{\"minVersionCode\":5}}";
    String jsonVersionCodeOutdated = "{\"com.example.app\":{\"minVersionCode\":0}}";

    String jsonMalformed = "{\"com.example\":{\"minVersionName\":\"1.2.1.1\"}}";
    String jsonMalformed2 = "{}";
    String jsonMalformed3 = "{\"com.example.app\":{\"someField\":\"1.2.1.1\"}}";

    String jsonVersionCheckDisabled = "{\"com.example.app\":{\"minVersionName\":\"2.1.1.1\", \"enable\": false}}";
    String jsonForceUpdateEnabled = "{\"com.example.app\":{\"minVersionName\":\"2.1.1.1\", \"force\": true}}";
    String jsonCustomMessage = "{\"com.example.app\":{\"minVersionName\":\"2.1.1.1\", \"message\": \"This is custom message\"}}";
}
