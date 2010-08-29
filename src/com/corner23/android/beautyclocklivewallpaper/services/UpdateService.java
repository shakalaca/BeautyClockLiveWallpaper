package com.corner23.android.beautyclocklivewallpaper.services;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import java.util.concurrent.RejectedExecutionException;

import com.corner23.android.beautyclocklivewallpaper.Settings;
import com.corner23.android.beautyclocklivewallpaper.asynctasks.CacheCleanUpTask;
import com.corner23.android.beautyclocklivewallpaper.asynctasks.FetchBeautyPictureTask;
import com.corner23.android.beautyclocklivewallpaper.asynctasks.PlayBellTask;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

public class UpdateService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener  {

	public static final String BROADCAST_WALLPAPER_UPDATE = UpdateService.class.getName() + ":UPDATE";

	private static final String TAG = "UpdateService";
	
	private static final int MAX_TIMEOUT_COUNT = 5;

	private Time mTime = new Time();
	private int mHourNextTime = 0;
	private int mMinuteNextTime = 0;
	private long mNextTimeInMillis = 0;
	private int mHour = 0;
	private int mMinute = 0;

	private FetchBeautyPictureTask mFetchBeautyPictureTask = null;
	private PlayBellTask mPlayBellTask = null;
	private CacheCleanUpTask mCacheCleanUpTask = null;
	
	// preferences
	private SharedPreferences mPrefs;
	private int mPictureSource = 0;
	private boolean mBellHourly = false;
	private boolean mFetchWhenScreenOff = true;
	private boolean mFetchLargerPicture = true;
	private boolean mSaveCopy = false;
	private int mPicturesPerFetch = 30;

	// network
	private ConnectivityManager cm = null;

	private boolean mRegScreenBR = false;
	private boolean mRegTimeBR = false;
	private boolean mStarted = false;
	private int mCurrentCount = 0;
	private int mTimeOutCount = 0;

	private void cancelFetchBeautyPictureTask() {
		if (mFetchBeautyPictureTask != null &&
			mFetchBeautyPictureTask.getStatus() == AsyncTask.Status.RUNNING) {
			mFetchBeautyPictureTask.cancel(true);
		}
	}

	private void startToFetchBeautyPictureTask() {
		Log.i(TAG, "startToFetchBeautyPictureTask");
		try {
			cancelFetchBeautyPictureTask();
			mFetchBeautyPictureTask = new FetchBeautyPictureTask(this, cm, mPictureSource, mFetchLargerPicture, mSaveCopy);
			if (mCurrentCount == 0) {
				mFetchBeautyPictureTask.saveSourcePath();
			}
			mFetchBeautyPictureTask.execute(mHour, mMinute);
		} catch (RejectedExecutionException e) {
			e.printStackTrace();
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

	private void cancelCleanUpCacheTask() {
		if (mCacheCleanUpTask != null &&
			mCacheCleanUpTask.getStatus() == AsyncTask.Status.RUNNING) {
			mCacheCleanUpTask.cancel(true);
		}
	}

	private void startToCleanUpCache(int hour, int minute, boolean bForce) {
		try {
			mCacheCleanUpTask = new CacheCleanUpTask(getApplicationContext(), bForce);
			mCacheCleanUpTask.execute(hour, minute, mPicturesPerFetch);
		} catch(RejectedExecutionException e) {
			e.printStackTrace();
		}
	}	
	
	private final BroadcastReceiver mScreenBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "mScreenBroadcastReceiver:onReceive");
        	if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                // Log.i(TAG, "Intent.ACTION_SCREEN_ON"); 
				registerTimeBroadcastReceiver();
				if (!mFetchWhenScreenOff) {
					UpdatePictures(false);
				}
	    	} else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
	            // Log.i(TAG, "Intent.ACTION_SCREEN_OFF"); 
				if (!mFetchWhenScreenOff) {
					unregisterTimeBroadcastReceiver();
				}
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
				UpdatePictures(true);
				return;
			}
			
			// normal tick, get time
			mTime.setToNow();
			
			if (mTime.minute == 0 && mBellHourly) {
				cancelPlayBellTask();
				startToPlayBell(mTime.hour);
			}
			
			if (mTime.hour == mHourNextTime && mTime.minute == mMinuteNextTime) {
				UpdatePictures(false);
			} else {
				broadcastUpdate();				
			}
		}
	};
	
	private void broadcastUpdate() {
		Log.d(TAG, "Broadcast Wallpaper update !!");
		Intent i = new Intent(BROADCAST_WALLPAPER_UPDATE);
		sendBroadcast(i);
	}
	
	private void UpdatePictures(boolean bRefreshSource) {			
		// don't update on custom files..
		if (mPictureSource == 9) {
			broadcastUpdate();				
			return;
		}
		
		// check if too early to update
		mTime.setToNow();
		long TimeInMillis = mTime.toMillis(false);
		
		if (!bRefreshSource) {
			long diff = mNextTimeInMillis - TimeInMillis;
			if (diff > mPicturesPerFetch * 0.5 * 60 * 1000) { // too early
				Log.i(TAG, "Too early to refresh");
				broadcastUpdate();				
				return;
			}
		}
		
		mCurrentCount = 0;
		cancelFetchBeautyPictureTask();
		
		int hour = mTime.hour;
		int minute = mTime.minute;
		
		// get next update time
		mTime.set(TimeInMillis + (long) (mPicturesPerFetch * 0.6 * 60 * 1000));
		mHourNextTime = mTime.hour;
		mMinuteNextTime = mTime.minute;
		mNextTimeInMillis = mTime.toMillis(false);
		Log.d(TAG, "Next update time: " + mHourNextTime + ":" + mMinuteNextTime);

		cancelCleanUpCacheTask();
		startToCleanUpCache(hour, minute, bRefreshSource);
		
		broadcastUpdate();				
	}

	private void readDefaultPrefs(SharedPreferences prefs) {
		if (prefs == null) {
			return;
		}
		
		mFetchWhenScreenOff = prefs.getBoolean(Settings.PREF_FETCH_WHEN_SCREEN_OFF, true);
		mBellHourly = prefs.getBoolean(Settings.PREF_RING_HOURLY, false);
		mSaveCopy = prefs.getBoolean(Settings.PREF_SAVE_COPY, false);
		mFetchLargerPicture = prefs.getBoolean(Settings.PREF_FETCH_LARGER_PICTURE, true);
		mPictureSource = Integer.parseInt(prefs.getString(Settings.PREF_PICTURE_SOURCE, "0"));
		mPicturesPerFetch = Integer.parseInt(prefs.getString(Settings.PREF_PICTURE_PER_FETCH, "30"));
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

	@Override
	public void onStart(Intent intent, int startId) {
		onStartCommand(intent, 0, startId);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand");
				
		if (!mStarted) {
			mStarted = true;
			UpdatePictures(true);
		}
		
		if (intent != null) {
			// from CacheCleanUpTask
			if (intent.hasExtra("fetch_pictures")) {
				// get exact start time
				mHour = intent.getIntExtra("hour", 0);
				mMinute = intent.getIntExtra("minute", 0);
				
				startToFetchBeautyPictureTask();
			// from FetchBeautyPictureTask
			} else if (intent.hasExtra("fetch_pictures_ret")) {
				boolean timeout = intent.getBooleanExtra("timeout", false);
				boolean success = intent.getBooleanExtra("fetch_success", false);
				
				Log.d(TAG, "Count:" + mCurrentCount);
				if (timeout && mTimeOutCount++ < MAX_TIMEOUT_COUNT) {
					Log.i(TAG, "timeout, startToFetchBeautyPicture again");					
				} else {
					if (!success) {
						Log.e(TAG, "failed !!");
					} else {
						if (mCurrentCount == 0) {
							broadcastUpdate();
						}					
					}
					mTimeOutCount = 0;
					
					if (mMinute == 59) {
						mHour++;
						if (mHour == 24) {
							mHour = 0;
						}
						mMinute = -1;
					}
					mMinute++;					
					mCurrentCount++;
				}

				if (mCurrentCount < mPicturesPerFetch) {
					startToFetchBeautyPictureTask();
				} else {
					mCurrentCount = 0;
				}
			}
		}

		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// register notification
		registerTimeBroadcastReceiver();
		registerScreenBroadcastReceiver();
		
		// get connection manager for checking network status
		cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		
    	mPrefs = getSharedPreferences(Settings.SHARED_PREFS_NAME, 0);
		mPrefs.registerOnSharedPreferenceChangeListener(this);
    	onSharedPreferenceChanged(mPrefs, null);
    	
    	// create .nomedia to prevent scanning of this folder
		try {
			File mFile = new File("/sdcard/BeautyClock/pic/.nomedia");
			if (mFile != null) {
				mFile.mkdirs();
				mFile.createNewFile();
			}
		} catch (IOException e) {
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		cancelPlayBellTask();
		cancelCleanUpCacheTask();
		cancelFetchBeautyPictureTask();
		
		unregisterTimeBroadcastReceiver();
		unregisterScreenBroadcastReceiver();
		
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (prefs == null) {
			return;
		}
		
		if (key == null) {
			readDefaultPrefs(prefs);
			return;
		}
		
		if (key.equals(Settings.PREF_FETCH_WHEN_SCREEN_OFF)) {
			mFetchWhenScreenOff = prefs.getBoolean(Settings.PREF_FETCH_WHEN_SCREEN_OFF, true);
		} else if (key.equals(Settings.PREF_RING_HOURLY)) {
			mBellHourly = prefs.getBoolean(Settings.PREF_RING_HOURLY, false);
		} else if (key.equals(Settings.PREF_SAVE_COPY)) {
			mSaveCopy = prefs.getBoolean(Settings.PREF_SAVE_COPY, false);
		} else if (key.equals(Settings.PREF_FETCH_LARGER_PICTURE)) {
			mFetchLargerPicture = prefs.getBoolean(Settings.PREF_FETCH_LARGER_PICTURE, true);
			UpdatePictures(true);
		} else if (key.equals(Settings.PREF_PICTURE_SOURCE)) {
			mPictureSource = Integer.parseInt(prefs.getString(Settings.PREF_PICTURE_SOURCE, "0"));
			UpdatePictures(true);
		} else if (key.equals(Settings.PREF_PICTURE_PER_FETCH)) {
			mPicturesPerFetch = Integer.parseInt(prefs.getString(Settings.PREF_PICTURE_PER_FETCH, "30"));
			UpdatePictures(true);
		}
	}
}