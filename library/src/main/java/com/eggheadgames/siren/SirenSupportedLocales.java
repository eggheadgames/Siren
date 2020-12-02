package com.eggheadgames.siren;

@SuppressWarnings("unused")
public enum SirenSupportedLocales {
    EL("el"),
    AR("ar"),
    DA("da"),
    DE("de"),
    ES("es"),
    ET("et"),
    EU("eu"),
    FR("fr"),
    HU("hu"),
    HY("hy"),
    IT("it"),
    IW("iw"),
    JA("ja"),
    KO("ko"),
    LT("lt"),
    LV("lv"),
    MS("ms"),
    NL("nl"),
    PL("pl"),
    PT("pt"),
    RU("ru"),
    SL("sl"),
    SV("sv"),
    TH("th"),
    TR("tr"),
    ZH("zh"),
    ZH-Hant("zh-rTW");

    private final String locale;

    SirenSupportedLocales(String locale) {
        this.locale = locale;
    }

    public String getLocale() {
        return locale;
    }
}
