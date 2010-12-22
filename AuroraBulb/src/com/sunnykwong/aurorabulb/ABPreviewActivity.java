package com.sunnykwong.aurorabulb;

import android.app.Activity;
import android.os.Bundle;
import java.io.File;
import java.util.HashMap;
import android.graphics.Paint;

import android.graphics.Color;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;

import com.android.settings.activities.ColorPickerDialog;

import android.graphics.drawable.BitmapDrawable;

import android.widget.ImageView;
import android.widget.Button;
import android.view.Display;
import android.app.AlertDialog;

public class ABPreviewActivity extends Activity {

	ImageView mPreviewImageView;
	Button mAppearanceButton, mCameraButton, mCreditsButton;
	AlertDialog mAD;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setTitle("AuroraBulb - Light Drawing Preview");
		setContentView(R.layout.preview);
		
		mPreviewImageView = (ImageView)findViewById(R.id.ImageView01);
		mPreviewImageView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
	    		AB.updateSrcBuffer();
	        	ABPreviewActivity.this.startActivityForResult(new Intent(ABPreviewActivity.this,ABAnimActivity.class),0);
				
			}
		});

		if (AB.PREFS.getBoolean("showIntro", true)) {
			CharSequence[] donotshow = {"Do not show again"};
			boolean[] donotshowBool = {false};
//			mAD = new AlertDialog.Builder(this)
//			.setTitle("Aurora Bulb!")
//			.setMessage("This application requires another camera with long-exposure to shoot the screen of this phone after you hit go.  Slide the phone on a straight line while maintaining the screen in the view of the camera's viewfinder, to embed the characters on the picture.")
//		    .setCancelable(true)
////		    .setMultiChoiceItems(donotshow, null, new AlertDialog.OnMultiChoiceClickListener() {
////				
////				@Override
////				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
////					// TODO Auto-generated method stub
////					AB.PREFS.edit().putBoolean("showIntro", false).commit();
////				}
////			})
//		    .setOnKeyListener(new OnKeyListener() {
//		    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
//		    		mAD.cancel();
//		    		return true;
//		    	};
//		    }).create();
//			mAD.show();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.previewmenu, menu);
		return true;
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		AB.PT1.setTypeface(Typeface.createFromAsset(getApplicationContext().getAssets(), AB.PREFS.getString("pickFont", "Unibody 8-SmallCaps.otf")));
		AB.updateSrcBuffer();

		mPreviewImageView.setBackgroundDrawable(new BitmapDrawable(AB.SRCBUFFER2));
		mPreviewImageView.invalidate();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
    	if (item.getItemId()==R.id.pickColor) {
    		ColorPickerDialog cpd = new ColorPickerDialog(this, new ColorPickerDialog.OnColorChangedListener() {
				
				@Override
				public void colorUpdate(int color) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void colorChanged(int color) {
					// TODO Auto-generated method stub
					AB.PREFS.edit().putInt("textColor", color).commit();
					AB.updateSrcBuffer();
					mPreviewImageView.setBackgroundDrawable(new BitmapDrawable(AB.SRCBUFFER2));
					mPreviewImageView.postInvalidate();
				}
			}, AB.PREFS.getInt("textColor", Color.GREEN));
    		cpd.show();
    	}
		if (item.getItemId()==R.id.camGuidance) {
			System.out.println("Cam Guidance");
		}
		if (item.getItemId()==R.id.pickFont) {
			AB.PREFSCREENTOSHOW = "pickFontScreen";
			startActivity(new Intent(this, ABPrefActivity.class));
		}
		if (item.getItemId()==R.id.pickText) {
			AB.PREFSCREENTOSHOW = "pickTextScreen";
			startActivity(new Intent(this, ABPrefActivity.class));
		}
		if (item.getItemId()==R.id.camGuidance) {
			AB.PREFSCREENTOSHOW = "camRefScreen";
			startActivity(new Intent(this, ABPrefActivity.class));
		}
		if (item.getItemId()==R.id.credits) {
			AB.PREFSCREENTOSHOW = "SectionCredits";
			startActivity(new Intent(this, ABPrefActivity.class));
		}
		
		return true;
	}
	
}