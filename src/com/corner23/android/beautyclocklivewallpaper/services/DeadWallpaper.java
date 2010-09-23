package com.corner23.android.beautyclocklivewallpaper.services;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import java.util.concurrent.RejectedExecutionException;

import com.corner23.android.beautyclocklivewallpaper.Settings;
import com.corner23.android.beautyclocklivewallpaper.asynctasks.PlayBellTask;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

public class DeadWallpaper extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "DeadWallpaper";

	private Time mTime = new Time();

	// preferences
	private boolean mFitScreen = false;
	private String mStorePath = null;
	private boolean mBellHourly = false;

	private int mScreenHeight = 0;
	private int mScreenWidth = 0;
	private SharedPreferences mPrefs;
	private WallpaperManager wm = null;
	private int OrigWallpaperWidth = 0;
	private int OrigWallpaperHeight = 0;
	
	private Bitmap mBeautyBitmap = null;
	private int mBitmapHeight = 0;
	private int mBitmapWidth = 0;

	private boolean mRegScreenBR = false;
	private boolean mRegTimeBR = false;
	private boolean mRegUpdateBR = false;

	private PlayBellTask mPlayBellTask = null;

	private final BroadcastReceiver mWallpaperUpdateBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "mWallpaperUpdateBroadcastReceiver:onReceive");
			updateBeautyBitmap();
			setWallpaper();
		}
	};
	
	private final BroadcastReceiver mScreenBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "mScreenBroadcastReceiver:onReceive");
        	if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                // Log.i(TAG, "Intent.ACTION_SCREEN_ON"); 
				registerTimeBroadcastReceiver();
        		registerWallpaperUpdateBroadcastReceiver();
				
				updateBeautyBitmap();
				setWallpaper();
	    	} else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
	            // Log.i(TAG, "Intent.ACTION_SCREEN_OFF"); 
	    		unregisterWallpaperUpdateBroadcastReceiver();
				unregisterTimeBroadcastReceiver();
	    	}
		}
	};

	private final BroadcastReceiver mTimeBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "mTimeBroadcastReceiver:onReceive");
			
			if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				String tz = intent.getStringExtra("time-zone");
				mTime = new Time(TimeZone.getTimeZone(tz).getID());
				
				startUpdateService();
			} else if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED)) {
				startUpdateService();
			} else {
				// normal tick, get time
				mTime.setToNow();
				
				if (mTime.minute == 0 && mBellHourly) {
					cancelPlayBellTask();
					startToPlayBell(mTime.hour);
				}
				
				updateBeautyBitmap();
				setWallpaper();
			}
		}
	};
	
	private void registerWallpaperUpdateBroadcastReceiver() {
		if (!mRegUpdateBR) {
	    	IntentFilter filter = new IntentFilter();
	    	filter.addAction(UpdateService.BROADCAST_WALLPAPER_UPDATE);
	    	registerReceiver(mWallpaperUpdateBroadcastReceiver, filter);
	    	mRegUpdateBR = true;
		}
	}
	
	private void unregisterWallpaperUpdateBroadcastReceiver() {
		if (mRegUpdateBR) {
			this.unregisterReceiver(mWallpaperUpdateBroadcastReceiver);
			mRegUpdateBR = false;
		}
	}
	
	private void registerScreenBroadcastReceiver() {
		if (!mRegScreenBR) {
			IntentFilter filter = new IntentFilter();  
			filter.addAction(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			this.registerReceiver(mScreenBroadcastReceiver, filter);
			mRegScreenBR = true;
		}
	}
	
	private void unregisterScreenBroadcastReceiver() {
		if (mRegScreenBR) {
			this.unregisterReceiver(mScreenBroadcastReceiver);
			mRegScreenBR = false;
		}
	}

	private void registerTimeBroadcastReceiver() {
		if (!mRegTimeBR) {
			IntentFilter filter = new IntentFilter();  
			filter.addAction(Intent.ACTION_TIME_TICK);  
			filter.addAction(Intent.ACTION_TIME_CHANGED);  
			filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
			this.registerReceiver(mTimeBroadcastReceiver, filter);
			mRegTimeBR = true;
		}
	}
	
	private void unregisterTimeBroadcastReceiver() {
		if (mRegTimeBR) {
			this.unregisterReceiver(mTimeBroadcastReceiver);
			mRegTimeBR = false;
		}
	}
	
	private void cancelPlayBellTask() {
		if (mPlayBellTask != null &&
			mPlayBellTask.getStatus() == AsyncTask.Status.RUNNING) {
			mPlayBellTask.cancel(true);
		}
	}
	
	private void startToPlayBell(int hour) {
		try {
			mPlayBellTask = new PlayBellTask(this);
			mPlayBellTask.execute(hour);
		} catch(RejectedExecutionException e) {
			e.printStackTrace();
		}
	}
	
	private void startUpdateService() {
		Intent intent = new Intent(DeadWallpaper.this, UpdateService.class);
		startService(intent);		
	}
	
	private void startUpdateServiceWithTime(int hour, int minute) {
		Intent intent = new Intent(DeadWallpaper.this, UpdateService.class);
		intent.putExtra("fetch_pictures", true);
		intent.putExtra("hour", hour);
		intent.putExtra("minute", minute);
		startService(intent);		
	}
	
	private void updateBeautyBitmap() {
		mTime.setToNow();		
		int hour = mTime.hour;
		int minute = mTime.minute;
		
		// check SD card first
		String fname = String.format("%s/%02d%02d.jpg", mStorePath, hour, minute);
		File _f_sdcard = new File(fname);
		if (!_f_sdcard.exists()) {
			fname = String.format("%s/%02d%02d.jpg", getCacheDir().getAbsolutePath(), hour, minute);

			File _f_cache = new File(fname);
			if (!_f_cache.exists()) {
				startUpdateServiceWithTime(hour, minute);
				return;
			}
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
		registerWallpaperUpdateBroadcastReceiver();
		registerTimeBroadcastReceiver();
		registerScreenBroadcastReceiver();
		
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
		cancelPlayBellTask();

		unregisterWallpaperUpdateBroadcastReceiver();
		unregisterTimeBroadcastReceiver();
		unregisterScreenBroadcastReceiver();
		
		try {
			wm.suggestDesiredDimensions(OrigWallpaperWidth, OrigWallpaperHeight);
			wm.clear();
			Log.d(TAG, OrigWallpaperWidth + "x" + OrigWallpaperHeight);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		super.onDestroy();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		onStartCommand(intent, 0, startId);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand");
				
		updateBeautyBitmap();
		setWallpaper();
		
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
		} else if (key.equals(Settings.PREF_RING_HOURLY)) {
			mBellHourly = prefs.getBoolean(Settings.PREF_RING_HOURLY, false);
		} else if (key.equals(Settings.PREF_SAVE_COPY) || 
				key.equals(Settings.PREF_FETCH_LARGER_PICTURE) || 
				key.equals(Settings.PREF_PICTURE_SOURCE) || 
				key.equals(Settings.PREF_PICTURE_PER_FETCH)) {
			startUpdateService();
		}
	}
}
