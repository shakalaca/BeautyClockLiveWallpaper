package com.corner23.android.beautyclocklivewallpaper.widget;

import java.io.File;

import com.corner23.android.beautyclocklivewallpaper.R;
import com.corner23.android.beautyclocklivewallpaper.Settings;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {

	private static final String DISPLAYTIME_FORMAT = "%02d:%02d";
	private static final String TAG = "WidgetProvider";
	private Time mTime = new Time();
	private File PictureFile = null;
	
	@Override
	public void onEnabled(Context context) {
		Log.d(TAG, "onEnable");
		super.onEnabled(context);		
	}

	@Override
	public void onDisabled(Context context) {
		Log.d(TAG, "onDisabled");
		super.onDisabled(context);
	}

	private Bitmap updateBeautyBitmap(Context context) {
		mTime.setToNow();		
		int hour = mTime.hour;
		int minute = mTime.minute;
		
		SharedPreferences prefs = context.getSharedPreferences(Settings.SHARED_PREFS_NAME, 0);
		String mStorePath = prefs.getString(Settings.PREF_INTERNAL_PICTURE_PATH, "");
		
		// check SD card first
		String fname = String.format("%s/%02d%02d.jpg", mStorePath, hour, minute);
		File _f_sdcard = new File(fname);
		PictureFile = _f_sdcard;
		if (!_f_sdcard.exists()) {
			fname = String.format("%s/%02d%02d.jpg", context.getCacheDir().getAbsolutePath(), hour, minute);
			
			File _f_cache = new File(fname);
			if (!_f_cache.exists()) {
				return null;
			}
			PictureFile = _f_cache;
		}
		
		return BitmapFactory.decodeFile(fname);
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d(TAG, "onUpdate");
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		context.startService(new Intent(context, WidgetService.class));

//		final int N = appWidgetIds.length;
		mTime.setToNow();
		
		// Perform this loop procedure for each App Widget that belongs to this provider
//        for (int i=0; i<N; i++) {        	
//        	int appWidgetId = appWidgetIds[i];

//    		Intent serviceintent = new Intent(context, WidgetService.class);
//    		serviceintent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
//    		context.startService(serviceintent);
        	
			Intent intent = new Intent(context, Settings.class);
//			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
			
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

			views.setTextViewText(R.id.TimeTextView, String.format(DISPLAYTIME_FORMAT, mTime.hour, mTime.minute));		
			views.setOnClickPendingIntent(R.id.BeautyClockImageView, pi);

			Bitmap bitmap = updateBeautyBitmap(context);
			if (bitmap == null) {
				views.setImageViewResource(R.id.BeautyClockImageView, R.drawable.beautyclock_retry);
				views.setViewVisibility(R.id.ShareIt, View.GONE);
			} else {
				views.setImageViewBitmap(R.id.BeautyClockImageView, bitmap);
				
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.setType("image/jpeg");
				shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(PictureFile));
				shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.share_picture_subject_text));
				shareIntent.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.share_picture_msg_text));
				shareIntent.putExtra(Intent.EXTRA_TITLE, context.getResources().getString(R.string.share_picture_title_text));
				PendingIntent pi_share = PendingIntent.getActivity(context, 0, shareIntent, 0);

				views.setOnClickPendingIntent(R.id.ShareIt, pi_share);
				views.setViewVisibility(R.id.ShareIt, View.VISIBLE);
			}
			
			// Tell the AppWidgetManager to perform an update on the current App Widget
            appWidgetManager.updateAppWidget(appWidgetIds, views);
//        }
    }

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive");
		final String action = intent.getAction(); 
	    if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) { 
	        final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID); 
	        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) { 
	            this.onDeleted(context, new int[] { appWidgetId }); 
//	    		context.stopService(new Intent(context, WidgetService.class));
	        }
	    } else { 
	        super.onReceive(context, intent); 
	    } 
	}
}
