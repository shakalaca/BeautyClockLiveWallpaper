package com.corner23.android.beautyclocklivewallpaper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.TimeZone;
import java.util.concurrent.RejectedExecutionException;

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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.service.wallpaper.WallpaperService;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

public class BeautyClockLiveWallpaper extends WallpaperService {

	public static final String SHARED_PREFS_NAME = "bclw_settings";
	public static final String BROADCAST_WALLPAPER_UPDATE = BeautyClockLiveWallpaper.class.getName() + ":UPDATE";

	private static final String TAG = "BeautyClockLiveWallpaper";

	private static final int IO_BUFFER_SIZE = 4096;
	
	private int mPictureSource = 0;
	private static final String END_STR = "_0";	
	private static final String ARTHUR_PICTURE_URL = "http://www.arthur.com.tw/photo/images/400/%02d%02d%s.JPG";
	private static final String CLOCKM_PICTURE_URL = "http://www.clockm.com/tw/img/clk/hour/%02d%02d.jpg";
	private static final String BIJIN_PICTURE_URL = "http://www.bijint.com/jp/img/clk/%02d%02d.jpg";
	private static final String BIJIN_KOREA_PICTURE_URL_L = "http://www.bijint.com/assets/pict/kr/590x450/%02d%02d.jpg";
	private static final String BIJIN_KOREA_PICTURE_URL = "http://www.bijint.com/assets/pict/kr/240x320/%02d%02d.jpg";
	private static final String BIJIN_GAL_PICTURE_URL_L = "http://gal.bijint.com/assets/pict/gal/590x450/%02d%02d.jpg";
	private static final String BIJIN_GAL_PICTURE_URL = "http://gal.bijint.com/assets/pict/gal/240x320/%02d%02d.jpg";
	private static final String BIJIN_CC_PICTURE_URL = "http://www.bijint.com/assets/pict/cc/590x450/%02d%02d.jpg";
	private static final String BINAN_PICTURE_URL = "http://www.bijint.com/binan/img/clk/%02d%02d.jpg";
	private static final String BIJIN_HK_PICTURE_URL_L = "http://www.bijint.com/assets/pict/hk/590x450/%02d%02d.jpg";
	private static final String BIJIN_HK_PICTURE_URL = "http://www.bijint.com/assets/pict/hk/240x320/%02d%02d.jpg";
	private static final String LOVELY_TIME_PICTURE_URL = "http://gameflier.lovelytime.com.tw/photo/%02d%02d.JPG";
	private static final String AVTOKEI_PICTURE_URL = "http://www.avtokei.jp/images/clocks/%02d/%02d%02d.jpg";
	private static final String BELL_TO_PLAY = "/sdcard/BeautyClock/bell/bell%02d.mp3";

	private static final String SDCARD_BASE_PATH = "/sdcard/BeautyClock/pic";
	private static final String ARTHUR_PICTURE_PATH = SDCARD_BASE_PATH + "/arthur/%02d%02d%s.JPG";
	private static final String CLOCKM_PICTURE_PATH = SDCARD_BASE_PATH + "/clockm/%02d%02d.jpg";
	private static final String BIJIN_PICTURE_PATH = SDCARD_BASE_PATH + "/bijin/%02d%02d.jpg";
	private static final String BIJIN_KOREA_PICTURE_PATH = SDCARD_BASE_PATH + "/bijin-kr/%02d%02d.jpg";
	private static final String BIJIN_GAL_PICTURE_PATH = SDCARD_BASE_PATH + "/bijin-gal/%02d%02d.jpg";
	private static final String BIJIN_CC_PICTURE_PATH = SDCARD_BASE_PATH + "/bijin-cc/%02d%02d.jpg";
	private static final String BINAN_PICTURE_PATH = SDCARD_BASE_PATH + "/binan/%02d%02d.jpg";
	private static final String BIJIN_HK_PICTURE_PATH = SDCARD_BASE_PATH + "/bijin-hk/%02d%02d.jpg";
	private static final String LOVELY_TIME_PICTURE_PATH = SDCARD_BASE_PATH + "/lovely/%02d%02d.JPG";
	private static final String AVTOKEI_PICTURE_PATH = SDCARD_BASE_PATH + "/av/%02d%02d.jpg";
	private static final String CUSTOM_PICTURE_PATH = SDCARD_BASE_PATH + "/custom/%02d%02d.jpg";

	private static final int BCLW_FETCH_STATE_OTHER_FAILED = -1;
	private static final int BCLW_FETCH_STATE_SUCCESS = 0;
	private static final int BCLW_FETCH_STATE_FILE_NOT_FOUND = 1;
	private static final int BCLW_FETCH_STATE_TIMEOUT = 2;
	private static final int BCLW_FETCH_STATE_IO_ERROR = 3;
	
	private static final int STATUS_BAR_HEIGHT = 38;
	
	private Time mTime = new Time();
	private int mHour = 0;
	private int mMinute = 0;
	private int mNextHour = 0;
	private int mNextMinute = 0;

	// private Bitmap mErrorBitmap = null;
	private Bitmap mCurrentBeautyBitmap = null;
	private Bitmap mNextBeautyBitmap = null;
	private FetchNextBeautyPictureTask mFetchNextBeautyPictureTask = null;
	private FetchCurrentBeautyPictureTask mFetchCurrentBeautyPictureTask = null;
	private PlayBellTask mPlayBellTask = null;
	
	// preferences
	private boolean mBellHourly = false;
	private boolean mFetchWhenScreenOff = true;
	private boolean mFetchLargerPicture = true;
	private boolean mFitScreen = false;
	private boolean mSaveCopy = false;

	private int mScreenHeight = 0;
	private int mScreenWidth = 0;

	private Proxy httpProxy;
	private String httpProxyHost;
	private Integer httpProxyPort;
	private ConnectivityManager cm = null;

	private boolean mIsScreenOn = true;
	
	private String getPATH(int hour, int minutes) {
		String URLstr = null;
		switch (mPictureSource) {
		default:
		case 0:	URLstr = String.format(ARTHUR_PICTURE_PATH, hour, minutes, (minutes == 0) ? END_STR : ""); break;
		case 1:	URLstr = String.format(CLOCKM_PICTURE_PATH, hour, minutes); break;
		case 2: URLstr = String.format(BIJIN_PICTURE_PATH, hour, minutes); break;
		case 3: URLstr = String.format(BIJIN_KOREA_PICTURE_PATH, hour, minutes); break;
		case 4: URLstr = String.format(BIJIN_HK_PICTURE_PATH, hour, minutes); break;
		case 5: URLstr = String.format(BIJIN_GAL_PICTURE_PATH, hour, minutes); break;
		case 6: URLstr = String.format(BIJIN_CC_PICTURE_PATH, hour, minutes); break;
		case 7: URLstr = String.format(BINAN_PICTURE_PATH, hour, minutes); break;
		case 8: URLstr = String.format(LOVELY_TIME_PICTURE_PATH, hour, minutes); break;
		case 9: URLstr = String.format(CUSTOM_PICTURE_PATH, hour, minutes); break;
		case 10: URLstr = String.format(AVTOKEI_PICTURE_PATH, hour, hour, minutes); break;
		}
		
		return URLstr;
	}

	private String getURL(int hour, int minutes) {
		String URLstr = null;
		switch (mPictureSource) {
		default:
		case 0:	URLstr = String.format(ARTHUR_PICTURE_URL, hour, minutes, (minutes == 0) ? END_STR : ""); break;
		case 1:	URLstr = String.format(CLOCKM_PICTURE_URL, hour, minutes); break;
		case 2: URLstr = String.format(BIJIN_PICTURE_URL, hour, minutes); break;
		case 3: URLstr = String.format(mFetchLargerPicture ? BIJIN_KOREA_PICTURE_URL_L : BIJIN_KOREA_PICTURE_URL, hour, minutes); break;
		case 4: URLstr = String.format(mFetchLargerPicture ? BIJIN_HK_PICTURE_URL_L : BIJIN_HK_PICTURE_URL, hour, minutes); break;
		case 5: URLstr = String.format(mFetchLargerPicture ? BIJIN_GAL_PICTURE_URL_L : BIJIN_GAL_PICTURE_URL, hour, minutes); break;
		case 6: URLstr = String.format(BIJIN_CC_PICTURE_URL, hour, minutes); break;
		case 7: URLstr = String.format(BINAN_PICTURE_URL, hour, minutes); break;
		case 8: URLstr = String.format(LOVELY_TIME_PICTURE_URL, hour, minutes); break;
		case 10: URLstr = String.format(AVTOKEI_PICTURE_URL, hour, hour, minutes); break;
		}
		
		return URLstr;
	}
	
	private class FetchNextBeautyPictureTask extends AsyncTask<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... arg0) {
			Log.w(TAG, "doInBackground:FetchNextBeautyPictureTask");
			int ret = BCLW_FETCH_STATE_OTHER_FAILED;
			mNextBeautyBitmap = null;
			try {
				String fpath = getPATH(mNextHour, mNextMinute);
				File mFile = new File(fpath);
				if (mFile.exists()) {
					mNextBeautyBitmap = fetchBeautyPictureBitmapFromFile(fpath);
				} else {
					URL mURL = new URL(getURL(mNextHour, mNextMinute));
					mNextBeautyBitmap = fetchBeautyPictureBitmapFromURL(mURL, mFile);
				}
				ret = BCLW_FETCH_STATE_SUCCESS;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				ret = BCLW_FETCH_STATE_FILE_NOT_FOUND;
			} catch (IOException e) {
				e.printStackTrace();
				ret = BCLW_FETCH_STATE_IO_ERROR;
				if (cm != null) {
					NetworkInfo ni = cm.getActiveNetworkInfo();
					if (ni != null && ni.isConnected()) {
						ret = BCLW_FETCH_STATE_TIMEOUT;
					}
				}
			}
			return ret;
		}
		
		protected void onPostExecute(Integer ret) {
			Log.w(TAG, "onPostExecute:FetchNextBeautyPictureTask");
			mFetchNextBeautyPictureTask = null;
			if (ret == BCLW_FETCH_STATE_TIMEOUT) {
				Log.w("BeautyClockUpdateService", "timeout, startToFetchNextBeautyPicture");
				startToFetchNextBeautyPicture();
			}
		}
	}

	private void startToFetchNextBeautyPicture() {
		try {
			mFetchNextBeautyPictureTask = new FetchNextBeautyPictureTask();
			mFetchNextBeautyPictureTask.execute();
		} catch(RejectedExecutionException e) {
			e.printStackTrace();
		}
	}
	
	private class FetchCurrentBeautyPictureTask extends AsyncTask<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... arg0) {
			Log.w(TAG, "doInBackground:FetchCurrentBeautyPictureTask");
			int ret = BCLW_FETCH_STATE_OTHER_FAILED;
			try {
				String fpath = getPATH(mHour, mMinute);
				File mFile = new File(fpath);
				if (mFile.exists()) {
					mCurrentBeautyBitmap = fetchBeautyPictureBitmapFromFile(fpath);
				} else {
					URL mURL = new URL(getURL(mHour, mMinute));
					mCurrentBeautyBitmap = fetchBeautyPictureBitmapFromURL(mURL, mFile);
				}
				ret = BCLW_FETCH_STATE_SUCCESS;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				ret = BCLW_FETCH_STATE_FILE_NOT_FOUND;
			} catch (IOException e) {
				e.printStackTrace();
				ret = BCLW_FETCH_STATE_IO_ERROR;
				if (cm != null) {
					NetworkInfo ni = cm.getActiveNetworkInfo();
					if (ni != null && ni.isConnected()) {
						ret = BCLW_FETCH_STATE_TIMEOUT;
					}
				}
			}
			return ret;
		}
		
		protected void onPostExecute(Integer ret) {
			Log.w(TAG, "onPostExecute:FetchCurrentBeautyPictureTask");
			mFetchCurrentBeautyPictureTask = null;
			if (mCurrentBeautyBitmap == null) {
				if (ret == BCLW_FETCH_STATE_TIMEOUT) {
					Log.w("BeautyClockUpdateService", "timeout, startToFetchCurrentBeautyPicture");
					startToFetchCurrentBeautyPicture();
					return;
				}
			} else {
				Intent i = new Intent(BROADCAST_WALLPAPER_UPDATE);
				sendBroadcast(i);
			}
			
			startToFetchNextBeautyPicture();
		}
	}

	private void startToFetchCurrentBeautyPicture(){
		try {
			mFetchCurrentBeautyPictureTask = new FetchCurrentBeautyPictureTask();
			mFetchCurrentBeautyPictureTask.execute();
		} catch(RejectedExecutionException e) {
			e.printStackTrace();
		}
	}
	
	private class PlayBellTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			if (!mBellHourly) {
				return null;
			}
			
			if (mMinute == 0) {
				// check phone settings first, if it's in silent or vibration mode, don't play bell!
				AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				if (am != null && (am.getRingerMode() == AudioManager.RINGER_MODE_SILENT ||
									am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
					Log.i(TAG, "Phone is in vibration mode or silent mode");
					return null;
				}
				
				try {
					MediaPlayer mp = new MediaPlayer();
					mp.setDataSource(String.format(BELL_TO_PLAY, mHour));
					mp.prepare();
					mp.start();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			mPlayBellTask = null;
		}
	}

	private void startToPlayBell() {
		try {
			mPlayBellTask = new PlayBellTask();
			mPlayBellTask.execute();
		} catch(RejectedExecutionException e) {
			e.printStackTrace();
		}
	}
	
	private void configureHttpProxy() {
		this.httpProxyHost = android.net.Proxy.getDefaultHost();
		this.httpProxyPort = android.net.Proxy.getDefaultPort();
		if (this.httpProxyHost != null && this.httpProxyPort != null) {
			SocketAddress proxyAddress = new InetSocketAddress(this.httpProxyHost, this.httpProxyPort);
			this.httpProxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
		} else {
			this.httpProxy = Proxy.NO_PROXY;
		}
	}

	private Bitmap ResizeBitmap(Bitmap bitmap) {
		if (bitmap == null) {
			return null;
		}

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
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

//		Log.w(TAG, "bitmap:"+ bitmap.getWidth() + "x" + bitmap.getHeight() + ":"+ bitmap.getDensity());
//		Log.w(TAG, "bitmap:"+ bitmap.getWidth()*bitmap.getHeight());
		Bitmap bitmapNew = Bitmap.createScaledBitmap(bitmap, width, height, true);
//			Log.w(TAG,"scaled !!!!!!!!!!");
//			Log.w(TAG,"bitmap:"+ bitmap.getWidth() + "x" + bitmap.getHeight() + ":"+ bitmap.getDensity());
//			Log.w(TAG,"bitmap:"+ bitmap.getWidth()*bitmap.getHeight());
		return bitmapNew;
	}
	
	private Bitmap fetchBeautyPictureBitmapFromFile(String fpath) {
		try {
//			Bitmap bitmap = BitmapFactory.decodeFile(fpath);
//			return ResizeBitmap(bitmap);
			return BitmapFactory.decodeFile(fpath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private Bitmap fetchBeautyPictureBitmapFromURL(URL url, File saveFile) throws IOException {
		Bitmap bitmap = null;
		InputStream in = null;
		OutputStream out = null;
		URLConnection urlc = null;

		try {
			if (cm != null) {
				// network is turned off by user
				if (!cm.getBackgroundDataSetting()) {
					return null;
				}
				
				// network is not connected
				NetworkInfo ni = cm.getActiveNetworkInfo();
				if (ni != null && ni.getState() != NetworkInfo.State.CONNECTED) {
					return null;
				}
				
				// if we're using WIFI, then there's no proxy from APN
				if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
					this.httpProxy = Proxy.NO_PROXY;
				} else {
					this.configureHttpProxy();
				}
			}
		} catch (Exception e) {
		}
		
		try {
			urlc = url.openConnection(this.httpProxy);
			urlc.setRequestProperty("User-Agent", "Mozilla/5.0");
			if (mPictureSource == 10) {
				urlc.setRequestProperty("Referer", "http://www.avtokei.jp/index.html");
			} else if (mPictureSource == 7) {
				urlc.setRequestProperty("Referer", "http://www.bijint.com/binan/");
			} else if (mPictureSource == 6) {
				urlc.setRequestProperty("Referer", "http://www.bijint.com/cc/");
			} else if (mPictureSource == 5) {
				urlc.setRequestProperty("Referer", "http://gal.bijint.com/");
			} else if (mPictureSource == 4) {
				urlc.setRequestProperty("Referer", "http://www.bijint.com/hk/");
			} else if (mPictureSource == 3) {
				urlc.setRequestProperty("Referer", "http://www.bijint.com/kr/");
			} else if (mPictureSource == 2) {
				urlc.setRequestProperty("Referer", "http://www.bijint.com/jp/");
			}
			
			urlc.setReadTimeout(10000);
			urlc.setConnectTimeout(10000);
			
			in = new BufferedInputStream(urlc.getInputStream(), IO_BUFFER_SIZE);

			final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
			out = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);
			copy(in, out);
			out.flush();

			final byte[] data = dataStream.toByteArray();
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

			if (mSaveCopy && saveFile != null && !saveFile.exists()) {
				saveCopy(bitmap, saveFile);
			}
//			Bitmap newbitmap = ResizeBitmap(bitmap);
//			if (newbitmap != null) {
//				bitmap = newbitmap;
//			}
		} finally {
			closeStream(in);
			closeStream(out);
		}

		return bitmap;
	}

	private static int copy(InputStream in, OutputStream out) throws IOException, SocketTimeoutException {
		byte[] b = new byte[IO_BUFFER_SIZE];
		int read, size = 0;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
			size += read;
		}
		return size;
	}

	private static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean saveCopy(Bitmap bitmap, File new_file) {
		try {
			// Try to create neccessary directory
			new_file.mkdirs();

			// output to tmp file
			String fname_tmp = new_file.getAbsolutePath() + ".tmp";
			FileOutputStream out = new FileOutputStream(fname_tmp);
			bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
			out.flush();
			out.close();

			// if success, rename to correct file name
			File tmp_file = new File(fname_tmp);
			new_file.delete();
			tmp_file.renameTo(new_file);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	private void update(Context context) {
		try {
			boolean reget = false;
			if (mFetchCurrentBeautyPictureTask != null) {
				mFetchCurrentBeautyPictureTask.cancel(true);
				reget = true;
			}
			
			if (mFetchNextBeautyPictureTask != null) {
				mFetchNextBeautyPictureTask.cancel(true);
				reget = true;
			}
			
			if (reget) {
				firstUpdate(context);
				return;
			}
			
			if (mNextBeautyBitmap != null) {
				mCurrentBeautyBitmap = mNextBeautyBitmap;
				Intent i = new Intent(BROADCAST_WALLPAPER_UPDATE);
				sendBroadcast(i);
			}
			
			mTime.setToNow();
			updateTimeForNextUpdate();
			
			startToFetchNextBeautyPicture();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.w(TAG, "onReceive");
        	if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.v(TAG, "Intent.ACTION_SCREEN_ON"); 
                mIsScreenOn = true;
				firstUpdate(context);
				return;
	    	} else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
	            Log.v(TAG, "Intent.ACTION_SCREEN_OFF"); 
	            mIsScreenOn = false;
	    	}
        	
			if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				String tz = intent.getStringExtra("time-zone");
				mTime = new Time(TimeZone.getTimeZone(tz).getID());
				if (mFetchCurrentBeautyPictureTask != null) {
					mFetchCurrentBeautyPictureTask.cancel(true);
				}
				if (mFetchNextBeautyPictureTask != null) {
					mFetchNextBeautyPictureTask.cancel(true);
				}
				firstUpdate(context);
				return;
			}
			
			mTime.setToNow();
			if (!(mHour == mTime.hour && mMinute == mTime.minute)) {
				mHour = mTime.hour;
				mMinute = mTime.minute;
				
				if (!mFetchWhenScreenOff && !mIsScreenOn) {
					updateTimeForNextUpdate();
					startToPlayBell();
					return;
				}
				
				update(context);
				startToPlayBell();
			}
		}
	};

	private void updateTimeForNextUpdate() {
		mNextHour = mTime.hour;
		mNextMinute = mTime.minute;
		if (mNextMinute == 59) {
			mNextHour++;
			if (mNextHour == 24) {
				mNextHour = 0;
			}
			mNextMinute = -1;
		}
		mNextMinute++;
	}

	private void firstUpdate(Context context) {		
		mTime.setToNow();
		mHour = mTime.hour;
		mMinute = mTime.minute;
			
		updateTimeForNextUpdate();
			
		startToFetchCurrentBeautyPicture();
	}
		
	@Override
	public void onCreate() {
		// read configuration
		SharedPreferences mSharedPreferences = this.getSharedPreferences(SHARED_PREFS_NAME, 0);
		if (mSharedPreferences != null) {
			mPictureSource = Integer.parseInt(mSharedPreferences.getString("picture_source", "0"));
			mBellHourly = mSharedPreferences.getBoolean("ring_hourly", false);
			mFetchWhenScreenOff = mSharedPreferences.getBoolean("fetch_screen_off", true);
			mFetchLargerPicture = mSharedPreferences.getBoolean("fetch_larger_picture", true);
			mFitScreen = mSharedPreferences.getBoolean("fit_screen", false);
			mSaveCopy = mSharedPreferences.getBoolean("save_copy", false);
		}

		// register notification
		IntentFilter filter = new IntentFilter();  
		filter.addAction(Intent.ACTION_TIME_TICK);  
		filter.addAction(Intent.ACTION_TIME_CHANGED);  
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		this.registerReceiver(mBroadcastReceiver, filter);
		
		// get connection manager for checking network status
		cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		// setup proxy
		this.configureHttpProxy();
		
		mScreenHeight = getResources().getDisplayMetrics().heightPixels;
		mScreenWidth = getResources().getDisplayMetrics().widthPixels;
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.w(TAG, "onStart");
		onStartCommand(intent, 0, startId);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.w(TAG, "onStartCommand");
				
		firstUpdate(this);

		return START_STICKY;
	}

	public void onDestroy() {
		if (mPlayBellTask != null) {
			mPlayBellTask.cancel(true);
		}
		if (mFetchCurrentBeautyPictureTask != null) {
			mFetchCurrentBeautyPictureTask.cancel(true);
		}
		if (mFetchNextBeautyPictureTask != null) {
			mFetchNextBeautyPictureTask.cancel(true);
		}
		this.unregisterReceiver(mBroadcastReceiver);
		super.onDestroy();
	}
	    
	@Override
	public Engine onCreateEngine() {
		return new BeautyClockEngine();
	}
	
	class BeautyClockEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
		
		private int nXOffset = 0;        
		private SharedPreferences mPrefs;
		private boolean bIsLarge = false;

		private BroadcastReceiver mWallpaperUpdateBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				draw();
			}
		};
		
	    BeautyClockEngine() {
	    	mPrefs = BeautyClockLiveWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
	    	mPrefs.registerOnSharedPreferenceChangeListener(this);
	    	onSharedPreferenceChanged(mPrefs, null);

	    	firstUpdate(BeautyClockLiveWallpaper.this);
		}

		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			mFetchWhenScreenOff = prefs.getBoolean("fetch_screen_off", true);
			mFitScreen = prefs.getBoolean("fit_screen", false);
			mSaveCopy = prefs.getBoolean("save_copy", false);
			boolean fetchlargerpicture = prefs.getBoolean("fetch_larger_picture", true);
			mBellHourly = prefs.getBoolean("ring_hourly", false);
			int picturesource = Integer.parseInt(prefs.getString("picture_source", "0"));
			if (picturesource != mPictureSource || fetchlargerpicture != mFetchLargerPicture) {
				mPictureSource = picturesource;
				mFetchLargerPicture = fetchlargerpicture;
				firstUpdate(BeautyClockLiveWallpaper.this);
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
	    	
	    	IntentFilter filter = new IntentFilter();
	    	filter.addAction(BROADCAST_WALLPAPER_UPDATE);
	    	BeautyClockLiveWallpaper.this.registerReceiver(mWallpaperUpdateBroadcastReceiver, filter);
		}

		@Override
		public void onDestroy() {
			// Log.d(TAG, "onDestroy (engine)");
			super.onDestroy();

            BeautyClockLiveWallpaper.this.unregisterReceiver(mWallpaperUpdateBroadcastReceiver);
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
			draw();
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			// Log.d(TAG, "onVisibilityChanged:" + visible);
			if (visible) {
				draw();
			}
		}

		void drawBeautyClock(Canvas c, Bitmap bitmap_src) {
			Bitmap bitmap_resized = ResizeBitmap(bitmap_src);
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
		
		void drawErrorScreen(Canvas c) {
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
		
		void draw() {
			final SurfaceHolder holder = getSurfaceHolder();
			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if (c != null) {
					if (mCurrentBeautyBitmap != null) {
						drawBeautyClock(c, mCurrentBeautyBitmap);
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