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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SharePicture extends Activity implements View.OnClickListener {
	
	private File PictureFile = null;
	
	private ImageView shareIV = null;
	private ImageView closeIV = null;

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
		
		shareIV = (ImageView) findViewById(R.id.ShareImageView);
		Bitmap pic = LoadCurrentPicture();		
		if (pic != null) {
			shareIV.setImageBitmap(pic);
			shareIV.setOnClickListener(this);
						
			TextView tv = (TextView) findViewById(R.id.ShareButton);
			tv.setOnClickListener(this);
			
			closeIV = (ImageView) findViewById(R.id.CloseImageView);
			closeIV.setOnClickListener(this);
		} else {
			Toast.makeText(this, R.string.share_picture_failed_text, Toast.LENGTH_SHORT).show();
			SharePicture.this.finish();
		}
	}

	@Override
	public void onClick(View v) {
		if (v != closeIV) {
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
		}
		
		SharePicture.this.finish();
	}
}
