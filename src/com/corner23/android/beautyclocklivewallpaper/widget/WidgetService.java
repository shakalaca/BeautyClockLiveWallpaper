package com.corner23.android.beautyclocklivewallpaper.widget;

import java.io.File;
import java.util.TimeZone;

import com.corner23.android.beautyclocklivewallpaper.R;
import com.corner23.android.beautyclocklivewallpaper.Settings;
import com.corner23.android.beautyclocklivewallpaper.services.UpdateService;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class WidgetService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "WidgetService";	
	private static final String DISPLAYTIME_FORMAT = "%02d:%02d";
	
	private Time mTime = new Time();

	private int mScreenHeight = 0;
	private int mScreenWidth = 0;
	
	private SharedPreferences mPrefs;
	private File PictureFile = null;
	
	private boolean mRegScreenBR = false;
	private boolean mRegTimeBR = false;
	private boolean mRegUpdateBR = false;
		
	private final BroadcastReceiver mWallpaperUpdateBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "mWallpaperUpdateBroadcastReceiver:onReceive");
			updateWidget(context);
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
				
    			updateWidget(context);
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
				
				startService(new Intent(context, UpdateService.class));
			} else if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED)) {
				startService(new Intent(context, UpdateService.class));
			} else {
				// normal tick, get time
				mTime.setToNow();
				
				updateWidget(context);
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
	
	private Bitmap updateBeautyBitmap() {
		mTime.setToNow();		
		int hour = mTime.hour;
		int minute = mTime.minute;

		String storePath = mPrefs.getString(Settings.PREF_INTERNAL_PICTURE_PATH, "");

		// check SD card first
		String fname = String.format("%s/%02d%02d.jpg", storePath, hour, minute);
		File _f_sdcard = new File(fname);
		PictureFile = _f_sdcard;
		if (!_f_sdcard.exists()) {
			fname = String.format("%s/%02d%02d.jpg", getCacheDir().getAbsolutePath(), hour, minute);
			File _f_cache = new File(fname);
			if (!_f_cache.exists()) {
				Intent intent = new Intent(this, UpdateService.class);
				intent.putExtra("fetch_pictures", true);
				intent.putExtra("hour", hour);
				intent.putExtra("minute", minute);
				startService(intent);	
				
				return null;
			}
			PictureFile = _f_cache;
		}
		
		Log.d(TAG, fname);
		return BitmapFactory.decodeFile(fname);
	}

	@SuppressWarnings("unused")
	private Bitmap ResizeBitmap(Bitmap bitmap) {
		if (bitmap == null) {
			Log.d(TAG, "null !");
			return null;
		}
		
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		if (width == 0 || height == 0) {
			return null;
		}
		
		if (width < mScreenWidth && height < mScreenHeight) {
			return null;
		}
				
		int maxWidth = (int) (mScreenWidth * 0.9);
		int maxHeight = (int) (mScreenHeight * 0.7);
/*		
		if (height > maxHeight) {
			double ratio = (double) maxHeight / height;
			height = maxHeight;
			width = (int) (width * ratio);
			bScaled = true;
		}
		
		if (width > maxWidth) {
			double ratio = (double) maxWidth / width;
			width = maxWidth;
			height = (int) (height * ratio);
			bScaled = true;
		}
*/
		if (height > width) {
			double ratio = (double) maxHeight / height;
			height = maxHeight;
			width = (int) (width * ratio);
		} else {
			double ratio = (double) maxWidth / width;
			width = maxWidth;
			height = (int) (height * ratio);
		}
		
		return Bitmap.createScaledBitmap(bitmap, width, height, true);
	}
		
	private void updateWidget(Context context) {
		Log.i(TAG, "updateWidget");

		AppWidgetManager awm = AppWidgetManager.getInstance(context);		
		ComponentName remoteWidget = new ComponentName(context, WidgetProvider.class);		
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
		
		mTime.setToNow();
		
		Intent intent = new Intent(context, Settings.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		
		remoteViews.setTextViewText(R.id.TimeTextView, String.format(DISPLAYTIME_FORMAT, mTime.hour, mTime.minute));
		remoteViews.setOnClickPendingIntent(R.id.BeautyClockImageView, pendingIntent);
		
		Bitmap bitmap = updateBeautyBitmap();
		if (bitmap == null) {
			remoteViews.setImageViewResource(R.id.BeautyClockImageView, R.drawable.beautyclock_retry);
			remoteViews.setViewVisibility(R.id.ShareIt, View.GONE);
		} else {
//			Bitmap bitmap_scaled = ResizeBitmap(bitmap);
//			if (bitmap_scaled != null) {
//				remoteViews.setImageViewBitmap(R.id.BeautyClockImageView, bitmap_scaled);
//			} else {
				remoteViews.setImageViewBitmap(R.id.BeautyClockImageView, bitmap);
//			}
				
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("image/jpeg");
			shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(PictureFile));
			shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.share_picture_subject_text));
			shareIntent.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.share_picture_msg_text));
			shareIntent.putExtra(Intent.EXTRA_TITLE, context.getResources().getString(R.string.share_picture_title_text));
			PendingIntent pi_share = PendingIntent.getActivity(context, 0, shareIntent, 0);

			remoteViews.setOnClickPendingIntent(R.id.ShareIt, pi_share);					
		}
		
		awm.updateAppWidget(remoteWidget, remoteViews);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		this.getCacheDir().mkdirs();
		
		// register notification
		registerWallpaperUpdateBroadcastReceiver();
		registerTimeBroadcastReceiver();
		registerScreenBroadcastReceiver();
		
		mPrefs = getSharedPreferences(Settings.SHARED_PREFS_NAME, 0);
    	mPrefs.registerOnSharedPreferenceChangeListener(this);
    	onSharedPreferenceChanged(mPrefs, null);		
    	
		startService(new Intent(this, UpdateService.class));
	}
	
	@Override
	public void onDestroy() {
		unregisterWallpaperUpdateBroadcastReceiver();
		unregisterTimeBroadcastReceiver();
		unregisterScreenBroadcastReceiver();
		
		stopService(new Intent(this, UpdateService.class));
		
		super.onDestroy();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		onStartCommand(intent, 0, startId);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand");
		
        mScreenHeight = getResources().getDisplayMetrics().heightPixels;
		mScreenWidth = getResources().getDisplayMetrics().widthPixels;
		
		updateWidget(this);
		
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
			return;
		}
		
		if (key.equals(Settings.PREF_SAVE_COPY) || 
				key.equals(Settings.PREF_FETCH_LARGER_PICTURE) || 
				key.equals(Settings.PREF_PICTURE_SOURCE) || 
				key.equals(Settings.PREF_PICTURE_PER_FETCH)) {
			startService(new Intent(this, UpdateService.class));
		}
	}
}
