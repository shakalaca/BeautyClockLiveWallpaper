package com.corner23.android.beautyclocklivewallpaper;

import com.corner23.android.beautyclocklivewallpaper.services.DeadWallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
	
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			SharedPreferences prefs = context.getSharedPreferences(Settings.SHARED_PREFS_NAME, 0);
			if (prefs != null) {
				boolean enableDeadWallpaperService = prefs.getBoolean(Settings.PREF_ENABLE_DEADWALLPAPER, false);
				if (enableDeadWallpaperService) {
		    		context.startService(new Intent(context, DeadWallpaper.class));
				}
			}
		}
	}
}
