package com.corner23.android.beautyclocklivewallpaper;

import java.io.File;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String SHARED_PREFS_NAME = "bclw_settings";
	
    public static final String PREF_FETCH_WHEN_SCREEN_OFF = "fetch_screen_off";
    public static final String PREF_RING_HOURLY = "ring_hourly";
    public static final String PREF_SAVE_COPY = "save_copy";
    public static final String PREF_FIT_SCREEN = "fit_screen";
    public static final String PREF_FETCH_LARGER_PICTURE = "fetch_larger_picture";
    public static final String PREF_PICTURE_SOURCE = "picture_source";
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
		File secretParadise = new File("/sdcard/BeautyClock/givemepower");
		if (secretParadise.exists()) {
			addPreferencesFromResource(R.xml.preferences_secret);
		} else {
	        addPreferencesFromResource(R.xml.preferences);
		}
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(
                this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
        super.onDestroy();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
    }
}
