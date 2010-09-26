package com.corner23.android.beautyclocklivewallpaper;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class SharePicture extends Activity {
	
	private File PictureFile = null;

	private Bitmap LoadCurrentPicture() {
		Time mTime = new Time();
		mTime.setToNow();
		
		SharedPreferences mPrefs = getSharedPreferences(Settings.SHARED_PREFS_NAME, 0);
		String mStorePath = mPrefs.getString(Settings.PREF_INTERNAL_PICTURE_PATH, "");
		
		String sFilePath = String.format("%s/%02d%02d.jpg", mStorePath, mTime.hour, mTime.minute);
		PictureFile = new File(sFilePath);
		if (PictureFile.exists()) {
			return BitmapFactory.decodeFile(sFilePath);
		}
		
		sFilePath = String.format("%s/%02d%02d.jpg", getCacheDir().getAbsolutePath(), mTime.hour, mTime.minute);
		PictureFile = new File(sFilePath);
		if (PictureFile.exists()) {
			return BitmapFactory.decodeFile(sFilePath);
		}
		
		PictureFile = null;
		
		return null;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.share_layout);
		
		Button btn = (Button) findViewById(R.id.ShareButton);
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (PictureFile != null) {
					Intent shareIntent = new Intent(Intent.ACTION_SEND);
					shareIntent.setType("image/jpeg");
					shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(PictureFile));
					shareIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_picture_subject_text));
					shareIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_picture_msg_text));
					shareIntent.putExtra(Intent.EXTRA_TITLE, getResources().getString(R.string.share_picture_title_text));
					startActivity(shareIntent);
				} else {
					Toast.makeText(SharePicture.this, R.string.share_picture_failed_text, Toast.LENGTH_SHORT).show();
				}
				
				SharePicture.this.finish();
			}
		});
		
		ImageView iv = (ImageView) findViewById(R.id.ShareImageView);
		Bitmap pic = LoadCurrentPicture();		
		if (pic != null) {
			iv.setImageBitmap(pic);
		} else {
			Toast.makeText(this, R.string.share_picture_failed_text, Toast.LENGTH_SHORT).show();
			SharePicture.this.finish();
		}
	}
}
