package com.corner23.android.beautyclocklivewallpaper.services;

import java.io.File;

import com.corner23.android.beautyclocklivewallpaper.Settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.service.wallpaper.WallpaperService;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

public class LiveWallpaper extends WallpaperService {

	@Override
	public Engine onCreateEngine() {
		return new BeautyClockEngine();
	}
	
	class BeautyClockEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
		
		private static final String TAG = "LiveWallpaper";
		private static final int STATUS_BAR_HEIGHT = 38;
		
		private Time mTime = new Time();
		private int mHour = 0;
		private int mMinute = 0;

		// preferences
		private boolean mFitScreen = false;
		private String mStorePath = null;

		private boolean mRegScreenBR = false;
		private boolean mRegUpdateBR = false;
		private boolean mIsEngineVisible = false;
		private boolean bIsLarge = false;
		
		private int mScreenHeight = 0;
		private int mScreenWidth = 0;
		private int nXOffset = 0;        
		private SharedPreferences mPrefs;
		
		private Bitmap mBeautyBitmap = null;

		private final BroadcastReceiver mScreenBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.w(TAG, "mScreenBroadcastReceiver:onReceive");
	        	if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
	                // Log.v(TAG, "Intent.ACTION_SCREEN_ON"); 
	                if (mIsEngineVisible) {
	                	registerWallpaperUpdateBroadcastReceiver();
	                }
		    	} else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
		            // Log.v(TAG, "Intent.ACTION_SCREEN_OFF"); 
		    		unregisterWallpaperUpdateBroadcastReceiver();
		    	}
			}
		};
		
		private final BroadcastReceiver mWallpaperUpdateBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				Log.w(TAG, "mWallpaperUpdateBroadcastReceiver:onReceive");
				updateBitmap();
				draw();
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
				unregisterReceiver(mWallpaperUpdateBroadcastReceiver);
				mRegUpdateBR = false;
			}
		}
		
		private void registerScreenBroadcastReceiver() {
			if (!mRegScreenBR) {
				IntentFilter filter = new IntentFilter();  
				filter.addAction(Intent.ACTION_SCREEN_ON);
				filter.addAction(Intent.ACTION_SCREEN_OFF);
				registerReceiver(mScreenBroadcastReceiver, filter);
				mRegScreenBR = true;
			}
		}
		
		private void unregisterScreenBroadcastReceiver() {
			if (mRegScreenBR) {
				unregisterReceiver(mScreenBroadcastReceiver);
				mRegScreenBR = false;
			}
		}
		
		BeautyClockEngine() {
			mScreenHeight = getResources().getDisplayMetrics().heightPixels;
			mScreenWidth = getResources().getDisplayMetrics().widthPixels;

	    	mPrefs = getSharedPreferences(Settings.SHARED_PREFS_NAME, 0);
	    	mPrefs.registerOnSharedPreferenceChangeListener(this);
	    	onSharedPreferenceChanged(mPrefs, null);
	    	
			startService(new Intent(LiveWallpaper.this, UpdateService.class));
		}

		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			if (prefs == null) {
				return;
			}
			
			if (key == null) {
				readDefaultPrefs(prefs);
				return;
			}
			
			if (key.equals(Settings.PREF_FIT_SCREEN)) {
				mFitScreen = prefs.getBoolean(Settings.PREF_FIT_SCREEN, false);
				updateBitmap();
				draw();
			} else if (key.equals(Settings.PREF_INTERNAL_PICTURE_PATH)) {
				mStorePath = prefs.getString(Settings.PREF_INTERNAL_PICTURE_PATH, "");
			}
		}

/*		
		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			Log.d(TAG, "onSurfaceCreated");
			super.onSurfaceCreated(holder);
		}

		@Override
		public int getDesiredMinimumHeight() {
			Log.d(TAG, "getDesiredMinimumHeight");
			return super.getDesiredMinimumHeight();
		}

		@Override
		public int getDesiredMinimumWidth() {
			Log.d(TAG, "getDesiredMinimumWidth");
			return super.getDesiredMinimumWidth();
		}

		@Override
		public SurfaceHolder getSurfaceHolder() {
			Log.d(TAG, "getSurfaceHolder");
			return super.getSurfaceHolder();
		}

		@Override
		public boolean isPreview() {
			Log.d(TAG, "isPreview");
			return super.isPreview();
		}

		@Override
		public boolean isVisible() {
			Log.d(TAG, "isVisible");
			return super.isVisible();
		}

		@Override
		public Bundle onCommand(String action, int x, int y, int z,
				Bundle extras, boolean resultRequested) {
			Log.d(TAG, "onCommand");
			return super.onCommand(action, x, y, z, extras, resultRequested);
		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			Log.d(TAG, "onTouchEvent");
			super.onTouchEvent(event);
		}

		@Override
		public void setTouchEventsEnabled(boolean enabled) {
			Log.d(TAG, "setTouchEventsEnabled:" + enabled);
			super.setTouchEventsEnabled(enabled);
		}


		@Override
		public void onDesiredSizeChanged(int desiredWidth, int desiredHeight) {
			Log.d(TAG, "onDesiredSizeChanged");
			super.onDesiredSizeChanged(desiredWidth, desiredHeight);
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			Log.d(TAG, "onSurfaceDestroyed");
			super.onSurfaceDestroyed(holder);
		}
*/
		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			// Log.d(TAG, "onCreate (engine)");
			super.onCreate(surfaceHolder);

			setTouchEventsEnabled(false);
	    	
			// register notification
	    	registerWallpaperUpdateBroadcastReceiver();
			registerScreenBroadcastReceiver();		
		}

		@Override
		public void onDestroy() {
			// Log.d(TAG, "onDestroy (engine)");
			super.onDestroy();

            unregisterWallpaperUpdateBroadcastReceiver();
    		unregisterScreenBroadcastReceiver();
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset,
				float xOffsetStep, float yOffsetStep, int xPixelOffset,
				int yPixelOffset) {
			// Log.d(TAG, "onOffsetsChanged");
			// Log.d(TAG, "x:" + xPixelOffset + ", y:" + yPixelOffset);
			nXOffset = xPixelOffset;
			if (bIsLarge) {
				draw();
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			// Log.d(TAG, "onSurfaceChanged:" + width + "," + height);
			
			mScreenHeight = height;
			mScreenWidth = width;
			updateBitmap();
			draw();
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			Log.d(TAG, "onVisibilityChanged:" + visible);
			if (visible) {
				updateBitmap();
				draw();
				mIsEngineVisible = true;
				registerWallpaperUpdateBroadcastReceiver();
			} else {
				mIsEngineVisible = false;
				unregisterWallpaperUpdateBroadcastReceiver();
			}
		}
		
		private void readDefaultPrefs(SharedPreferences prefs) {
			if (prefs == null) {
				return;
			}
			
			mFitScreen = prefs.getBoolean(Settings.PREF_FIT_SCREEN, false);
			mStorePath = prefs.getString(Settings.PREF_INTERNAL_PICTURE_PATH, "");
		}
		
		private void updateTime() {		
			mTime.setToNow();
			mHour = mTime.hour;
			mMinute = mTime.minute;
		}
		
		private void updateBitmap() {
			updateTime();
			
			// check SD card first
			String fname = String.format("%s/%02d%02d.jpg", mStorePath, mHour, mMinute);
			File _f_sdcard = new File(fname);
			Log.d(TAG, "check SD:" + fname);
			if (!_f_sdcard.exists()) {
				fname = String.format("%s/%02d%02d.jpg", getCacheDir().getAbsolutePath(), mHour, mMinute);
				Log.d(TAG, "check cache:" + fname);
				
				File _f_cache = new File(fname);
				if (!_f_cache.exists()) {
					Intent intent = new Intent(LiveWallpaper.this, UpdateService.class);
					intent.putExtra("fetch_pictures", true);
					intent.putExtra("hour", mHour);
					intent.putExtra("minute", mMinute);
					startService(intent);
					
					return;
				}
 			}
			
			Bitmap bitmap = BitmapFactory.decodeFile(fname);
			if (bitmap != null) {
				mBeautyBitmap = ResizeBitmap(bitmap);
			}
		}

		private Bitmap ResizeBitmap(Bitmap bitmap) {
			if (bitmap == null) {
				return null;
			}

			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			if (width == 0 || height == 0) {
				return null;
			}
			
			if (mScreenWidth > mScreenHeight) {
				double ratio = (float) (mScreenHeight - STATUS_BAR_HEIGHT) / height;
				width = (int) (width * ratio);
				height = mScreenHeight - STATUS_BAR_HEIGHT;
			} else {
				if (height > width) {
					if (mFitScreen) {
						height = mScreenHeight - STATUS_BAR_HEIGHT;
					} else {
						double ratio = (float) mScreenWidth / width;
						height = (int) (height * ratio);
					}
					width = mScreenWidth;
					/*
					double ratio = (float) (mScreenHeight - 60) / height;
					height = mScreenHeight - 60;
					width = (int) (width * ratio);
					*/
				} else {
					if (mFitScreen) {
						height = mScreenHeight - STATUS_BAR_HEIGHT;
					} else {
						double ratio = (float) mScreenWidth*2 / width;
						height = (int) (height * ratio);
					}
					width = mScreenWidth*2;
				}
			}

//			Log.w(TAG, "bitmap:"+ bitmap.getWidth() + "x" + bitmap.getHeight() + ":"+ bitmap.getDensity());
//			Log.w(TAG, "bitmap:"+ bitmap.getWidth()*bitmap.getHeight());
			Bitmap bitmapNew = Bitmap.createScaledBitmap(bitmap, width, height, true);
//				Log.w(TAG,"scaled !!!!!!!!!!");
//				Log.w(TAG,"bitmap:"+ bitmap.getWidth() + "x" + bitmap.getHeight() + ":"+ bitmap.getDensity());
//				Log.w(TAG,"bitmap:"+ bitmap.getWidth()*bitmap.getHeight());
			return bitmapNew;
		}
		
		private void drawBeautyClock(Canvas c, Bitmap bitmap_src) {
			if (c == null || bitmap_src == null) {
				return;
			}
			
			Bitmap bitmap_resized = bitmap_src; //ResizeBitmap(bitmap_src);
			int width = bitmap_resized.getWidth();
			int height = bitmap_resized.getHeight();
			int Xpos = 0, Ypos = STATUS_BAR_HEIGHT;
			
			// picture across virtual desktop, set scrolling
			if (width > height) {
				Xpos = nXOffset;
				bIsLarge = true;
			} else {
				bIsLarge = false;
			}
			
			if (mScreenWidth > mScreenHeight) {
				int offset = (int) (mScreenWidth * (bIsLarge ? 1.2 : 1) - width) / 2;
				if (offset > 0) {
					Xpos += offset;
				}
			} else {
				if (!mFitScreen) {
					int offset = (mScreenHeight - STATUS_BAR_HEIGHT - height) / 2;
					if (offset > 0) {
						Ypos += offset;
					}
				}
			}
			
			// clean before drawing
			c.drawColor(Color.BLACK);
			c.drawBitmap(bitmap_resized, Xpos, Ypos, null);
		}
		
		private void drawErrorScreen(Canvas c) {
			updateTime();
			
			// setup paint for drawing digitl clock
			Paint paint = new Paint();
			paint.setColor(Color.YELLOW);
			paint.setAntiAlias(true);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(5);
			paint.setTextSize(120);
			
			String time = String.format("%02d:%02d", mHour, mMinute);
			c.drawColor(Color.BLACK);
			c.drawText(time, mScreenWidth/2-150, mScreenHeight/2-30, paint);
			
/*
			Paint paint = new Paint();
			paint.setColor(Color.BLACK);
			paint.setAntiAlias(true);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(5);
			paint.setTextSize(120);
			
			if (mErrorBitmap == null) {
				Drawable dw = getResources().getDrawable(R.drawable.beautyclock_retry);
				mErrorBitmap = ResizeBitmap(((BitmapDrawable)dw).getBitmap());
			}
			c.drawBitmap(mErrorBitmap, 0, 15, paint);
			String time = String.format("%02d:%02d", mHour, mMinute);
			/c.drawText(time, mScreenWidth/2-150, 140, paint);
*/
		}
		
		private void draw() {
			final SurfaceHolder holder = getSurfaceHolder();
			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if (c != null) {
					if (mBeautyBitmap != null) {
						drawBeautyClock(c, mBeautyBitmap);
					} else {
						drawErrorScreen(c);
					}
				}
			} finally {
				if (c != null) {
					holder.unlockCanvasAndPost(c);
				}
			}
		}
	}
}