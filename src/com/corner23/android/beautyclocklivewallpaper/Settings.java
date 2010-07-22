package com.corner23.android.beautyclocklivewallpaper;

import java.io.File;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getPreferenceManager().setSharedPreferencesName(BeautyClockLiveWallpaper.SHARED_PREFS_NAME);
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
