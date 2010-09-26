package com.corner23.android.beautyclocklivewallpaper.widget;

import com.corner23.android.beautyclocklivewallpaper.R;
import com.corner23.android.beautyclocklivewallpaper.SharePicture;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class ShareWidgetProvider extends AppWidgetProvider {
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		
		final int N = appWidgetIds.length;
		
		// Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
        	int appWidgetId = appWidgetIds[i];

			Intent intent = new Intent(context, SharePicture.class);
			PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
			
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.share_widget_layout);
			views.setOnClickPendingIntent(R.id.ShareWidget, pi);
			
			// Tell the AppWidgetManager to perform an update on the current App Widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
	}

	@Override
	public void onReceive(Context context, Intent intent) {
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
