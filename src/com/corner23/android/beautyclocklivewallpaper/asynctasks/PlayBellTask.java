package com.corner23.android.beautyclocklivewallpaper.asynctasks;

import java.io.IOException;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

public class PlayBellTask extends AsyncTask<Integer, Void, Void> {

	private static final String TAG = "PlayBellTask";
	
	private static final String BELL_TO_PLAY = "/sdcard/BeautyClock/bell/bell%02d.mp3";

	private Context mContext = null;

	public PlayBellTask(Context context) {
		mContext = context;
	}
	
	@Override
	protected Void doInBackground(Integer... params) {
		int hour = params[0];
		
		// check phone settings first, if it's in silent or vibration mode, don't play bell!
		AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		if (am != null && (am.getRingerMode() == AudioManager.RINGER_MODE_SILENT ||
							am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
			Log.i(TAG, "Phone is in vibration mode or silent mode");
			return null;
		}
		
		try {
			MediaPlayer mp = new MediaPlayer();
			mp.setDataSource(String.format(BELL_TO_PLAY, hour));
			mp.prepare();
			mp.start();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}		
}
