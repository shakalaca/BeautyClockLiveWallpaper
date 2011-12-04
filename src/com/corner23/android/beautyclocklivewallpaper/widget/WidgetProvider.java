package com.corner23.android.beautyclocklivewallpaper.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WidgetProvider extends AppWidgetProvider {

	private static final String TAG = "WidgetProvider";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d(TAG, "onUpdate");
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		context.startService(new Intent(context, WidgetService.class));
    }

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive");
		final String action = intent.getAction(); 
	    if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) { 
	        final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID); 
	        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) { 
	            this.onDeleted(context, new int[] { appWidgetId }); 
	        }
	    } else { 
	        super.onReceive(context, intent); 
	    } 
	}
}
