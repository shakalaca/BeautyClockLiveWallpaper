package com.corner23.android.beautyclocklivewallpaper.services;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;

import com.corner23.android.beautyclocklivewallpaper.Settings;
import com.corner23.android.beautyclocklivewallpaper.asynctasks.CacheCleanUpTask;
import com.corner23.android.beautyclocklivewallpaper.asynctasks.FetchBeautyPictureTask;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.Log;

public class UpdateService extends Service {

	public static final String BROADCAST_WALLPAPER_UPDATE = UpdateService.class.getName() + ":UPDATE";

	private static final String TAG = "UpdateService";
	
	private static final int MAX_TIMEOUT_COUNT = 5;

	private int mHour = 0;
	private int mMinute = 0;

	private FetchBeautyPictureTask mFetchBeautyPictureTask = null;
	private CacheCleanUpTask mCacheCleanUpTask = null;
	
	// preferences
	private SharedPreferences mPrefs;
	private int mPictureSource = 0;
	private boolean mFetchWhenScreenOff = true;
	private boolean mFetchLargerPicture = true;
	private boolean mSaveCopy = false;
	private int mPicturesPerFetch = 30;

	// network
	private ConnectivityManager cm = null;

	// alarm
	private AlarmManager am = null;

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

	private void cancelCleanUpCacheTask() {
		if (mCacheCleanUpTask != null &&
			mCacheCleanUpTask.getStatus() == AsyncTask.Status.RUNNING) {
			mCacheCleanUpTask.cancel(true);
		}
	}

	private void startToCleanUpCache(int hour, int minute) {
		try {
			mCacheCleanUpTask = new CacheCleanUpTask(getApplicationContext(), true);
			mCacheCleanUpTask.execute(hour, minute, mPicturesPerFetch);
		} catch(RejectedExecutionException e) {
			e.printStackTrace();
		}
	}	
	
	private void broadcastUpdate() {
		Log.d(TAG, "Broadcast Wallpaper update !!");
		Intent i = new Intent(BROADCAST_WALLPAPER_UPDATE);
		sendBroadcast(i);
	}
	
	private void UpdatePictures() {			
		// don't update on custom files..
		if (mPictureSource != 9) {
			Time mTime = new Time();
			mTime.setToNow();		
			mCurrentCount = 0;
			
			cancelFetchBeautyPictureTask();		
			cancelCleanUpCacheTask();		
			startToCleanUpCache(mTime.hour, mTime.minute);
		}

		broadcastUpdate();				
	}

	private void readDefaultPrefs(SharedPreferences prefs) {
		if (prefs == null) {
			return;
		}
		
		mFetchWhenScreenOff = prefs.getBoolean(Settings.PREF_FETCH_WHEN_SCREEN_OFF, true);
		mSaveCopy = prefs.getBoolean(Settings.PREF_SAVE_COPY, false);
		mFetchLargerPicture = prefs.getBoolean(Settings.PREF_FETCH_LARGER_PICTURE, true);
		mPictureSource = Integer.parseInt(prefs.getString(Settings.PREF_PICTURE_SOURCE, "0"));
		mPicturesPerFetch = Integer.parseInt(prefs.getString(Settings.PREF_PICTURE_PER_FETCH, "30"));
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
			
			readDefaultPrefs(mPrefs);	    	
			
			PendingIntent pi = PendingIntent.getService(this, 0, new Intent(UpdateService.this, UpdateService.class), 0);
	        am.cancel(pi);
		}
		
		if (intent != null) {
			// from CacheCleanUpTask
			if (intent.hasExtra("fetch_pictures")) {
				// get exact start time
				mHour = intent.getIntExtra("hour", 0);
				mMinute = intent.getIntExtra("minute", 0);
				
				readDefaultPrefs(mPrefs);
		    	
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
					
					if (mFetchWhenScreenOff) {
						Log.d(TAG, "Setting alarm ..");
						PendingIntent pi = PendingIntent.getService(this, 0, new Intent(UpdateService.this, UpdateService.class), 0);
						long RoundTime = (long)(mPicturesPerFetch * 0.6 * 60 * 1000);
						
						am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
								SystemClock.elapsedRealtime() + RoundTime, 
								RoundTime, pi);
					}
					
					this.stopSelf();
				}
			// do a full refresh
			} else {
				UpdatePictures();
			}
		}

		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// get connection manager for checking network status
		cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		
    	mPrefs = getSharedPreferences(Settings.SHARED_PREFS_NAME, 0);

    	// create .nomedia to prevent scanning of this folder
		try {
			File mFile = new File("/sdcard/BeautyClock/pic/.nomedia");
			if (mFile != null) {
				mFile.getParentFile().mkdirs();
				mFile.createNewFile();
			}
		} catch (IOException e) {
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		cancelCleanUpCacheTask();
		cancelFetchBeautyPictureTask();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
