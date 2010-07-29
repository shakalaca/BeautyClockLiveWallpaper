package com.corner23.android.beautyclocklivewallpaper.asynctasks;

import java.io.File;

import com.corner23.android.beautyclocklivewallpaper.services.UpdateService;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class CacheCleanUpTask extends AsyncTask<Integer, Void, Void> {

	private static final String TAG = "CacheCleanUpTask";
	private static final String CACHE_FILE_PATH = "%02d%02d.jpg";
	private int hour, minute, max_pic;
	private Context mContext = null;
	private boolean bForceCleanUp = false;
	
	public CacheCleanUpTask (Context context, boolean bForce) {
		mContext = context;
		bForceCleanUp = bForce;
	}
	
	private void moveCachedFiles(int h, int m, File _from_root, File _to_root) {
		for (int i = 0; i < max_pic; i++) {
			String _cache_fname = String.format(CACHE_FILE_PATH, h, m);
			File _from = new File(_from_root, _cache_fname);
			File _to = new File(_to_root, _cache_fname);
			if (_from != null && _to != null) {
				if (_from.exists()) {
					Log.d(TAG, "Found.." + _cache_fname);
					_from.renameTo(_to);
				}
			}
			
			if (m == 59) {
				h++;
				if (h == 24) {
					h = 0;
				}
				m = -1;
			}
			m++;
		}
	}
	
	private void deleteExpiredCacheFiles() {
		File[] files = mContext.getCacheDir().listFiles();
		for (int i = 0; i < files.length; i++) {
			Log.d(TAG, "deleting: " + files[i].getAbsolutePath());
			files[i].delete();
		}
	}
	
	@Override
	protected Void doInBackground(Integer... params) {
		hour = params[0];
		minute = params[1];
		max_pic = params[2];

		File cacheDir = mContext.getCacheDir();
		File tmpDir = mContext.getDir("bc_tmp", Context.MODE_PRIVATE);

		if (!bForceCleanUp) {
			moveCachedFiles(hour, minute, cacheDir, tmpDir);
		}
		deleteExpiredCacheFiles();
		if (!bForceCleanUp) {
			moveCachedFiles(hour, minute, tmpDir, cacheDir);
		}
		
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		
		Intent intent = new Intent(mContext, UpdateService.class);
		intent.putExtra("fetch_pictures", true);
		intent.putExtra("hour", hour);
		intent.putExtra("minute", minute);
		mContext.startService(intent);		
	}
}