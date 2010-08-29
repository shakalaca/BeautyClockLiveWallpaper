package com.corner23.android.beautyclocklivewallpaper.services;

import java.io.File;
import java.io.IOException;

import com.corner23.android.beautyclocklivewallpaper.Settings;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

public class DeadWallpaper extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "DeadWallpaper";

	private Time mTime = new Time();

	// preferences
	private boolean mFitScreen = false;
	private String mStorePath = null;

	private int mScreenHeight = 0;
	private int mScreenWidth = 0;
	private SharedPreferences mPrefs;
	private WallpaperManager wm = null;
	private int OrigWallpaperWidth = 0;
	private int OrigWallpaperHeight = 0;
	
	private Bitmap mBeautyBitmap = null;
	private int mBitmapHeight = 0;
	private int mBitmapWidth = 0;
	
	private final BroadcastReceiver mWallpaperUpdateBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "mWallpaperUpdateBroadcastReceiver:onReceive");
			updateBeautyBitmap();
			setWallpaper();
		}
	};
	
	private void updateBeautyBitmap() {
		mTime.setToNow();		
		int hour = mTime.hour;
		int minute = mTime.minute;
		
		// check SD card first
		String fname = String.format("%s/%02d%02d.jpg", mStorePath, hour, minute);
		File _f_sdcard = new File(fname);
		if (!_f_sdcard.exists()) {
			fname = String.format("%s/%02d%02d.jpg", getCacheDir().getAbsolutePath(), hour, minute);
		}
		
		mBeautyBitmap = BitmapFactory.decodeFile(fname);
	}
	
	private void CalculateRightSize(Bitmap bitmap) {
		if (bitmap == null) {
			return;
		}

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		if (width == 0 || height == 0) {
			return;
		}
		
		if (mScreenWidth > mScreenHeight) {
			double ratio = (double) mScreenHeight / height;
			width = (int) (width * ratio);
			height = mScreenHeight;
		} else {
			if (height > width) {
				if (mFitScreen) {
					height = mScreenHeight;
				} else {
					double ratio = (double) mScreenWidth / width;
					height = (int) (height * ratio);
				}
				width = mScreenWidth;
			} else {
				if (mFitScreen) {
					height = mScreenHeight;
				} else {
					double ratio = (double) mScreenWidth*2 / width;
					height = (int) (height * ratio);
				}
				width = mScreenWidth*2;
			}
		}
		
		mBitmapHeight = height;
		mBitmapWidth = width;
	}
	
	private void setWallpaper() {
		try {
			if (mBeautyBitmap != null) {
				CalculateRightSize(mBeautyBitmap);
				
				wm.setBitmap(mBeautyBitmap);
				
				if (mBitmapWidth != 0 && mBitmapHeight != 0) {
					wm.suggestDesiredDimensions(mBitmapWidth, mBitmapHeight);
				} else {
					if (mBitmapWidth == 0) {
						Log.e(TAG, "Width is zero !");
					}
					
					if (mBitmapHeight == 0) {
						Log.e(TAG, "Height is zero !");
					}
				}
			}
		} catch (IOException e) {
			Log.e(TAG, "Error setting wallpaper ! :" + e.getMessage());
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		// register notification
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(UpdateService.BROADCAST_WALLPAPER_UPDATE);
    	registerReceiver(mWallpaperUpdateBroadcastReceiver, filter);

		mScreenHeight = getResources().getDisplayMetrics().heightPixels;
		mScreenWidth = getResources().getDisplayMetrics().widthPixels;
		
    	mPrefs = getSharedPreferences(Settings.SHARED_PREFS_NAME, 0);
    	mPrefs.registerOnSharedPreferenceChangeListener(this);
    	onSharedPreferenceChanged(mPrefs, null);

		wm = WallpaperManager.getInstance(this);
		OrigWallpaperWidth = wm.getDesiredMinimumWidth();
		OrigWallpaperHeight = wm.getDesiredMinimumHeight();
		Log.d(TAG, OrigWallpaperWidth + "x" + OrigWallpaperHeight);
		
		startService(new Intent(DeadWallpaper.this, UpdateService.class));
	}

	public void onDestroy() {		
        unregisterReceiver(mWallpaperUpdateBroadcastReceiver);

		super.onDestroy();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		onStartCommand(intent, 0, startId);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand");
				
		if (intent != null) {
			boolean enable = intent.getBooleanExtra("enable", false);
			if (enable) {
				updateBeautyBitmap();
				setWallpaper();
			} else {
				try {
					wm.suggestDesiredDimensions(OrigWallpaperWidth, OrigWallpaperHeight);
					wm.clear();
					Log.d(TAG, OrigWallpaperWidth + "x" + OrigWallpaperHeight);
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.stopSelf();
			}
		}
		
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (prefs == null) {
			return;
		}
		
		if (key == null) {
			mFitScreen = prefs.getBoolean(Settings.PREF_FIT_SCREEN, false);
			mStorePath = prefs.getString(Settings.PREF_INTERNAL_PICTURE_PATH, "");
			
			return;
		}
		
		if (key.equals(Settings.PREF_FIT_SCREEN)) {
			mFitScreen = prefs.getBoolean(Settings.PREF_FIT_SCREEN, false);
			setWallpaper();
		} else if (key.equals(Settings.PREF_INTERNAL_PICTURE_PATH)) {
			mStorePath = prefs.getString(Settings.PREF_INTERNAL_PICTURE_PATH, "");
		}
	}
}
