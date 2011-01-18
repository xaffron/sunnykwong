package com.sunnykwong.aurorabulb;

import android.app.Activity;
import android.os.Bundle;
import java.io.File;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import android.widget.EditText;
import android.graphics.Color;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
import android.widget.CheckBox;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
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
	Button mGoButton;
	AlertDialog mAD;
	CheckBox mCheckBox;
	Boolean mTempFlag;
	TextView mTextCam;
	
	EditText camShutter, camTimer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setTitle("AuroraBulb - Light Drawing Preview");
		setContentView(R.layout.preview);

		
		mPreviewImageView = (ImageView)findViewById(R.id.imagePreview);
		AB.updatePreviewBuffer();
		mPreviewImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ABPreviewActivity.this.openOptionsMenu();
			}
		});
		mPreviewImageView.invalidate();

		mTextCam = (TextView)findViewById(R.id.textCam);
		mTextCam.setText("Camera: Timer @ " + (AB.PREFS.getString("timePhotoTimer", "10"))
				+ " sec, Shutter @ " + (AB.PREFS.getString("timeShutterDuration", "10"))
				+ " sec.");
		mTextCam.invalidate();
		
		mGoButton = (Button)findViewById(R.id.buttonGo);
		mGoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
	    		AB.updateSrcBuffer();
	        	ABPreviewActivity.this.startActivityForResult(new Intent(ABPreviewActivity.this,ABAnimActivity.class),AB.RENDERAURORA);
			}
		});
		
		if (AB.PREFS.getBoolean("showIntro", true)) {
			mTempFlag = false;
			
			LayoutInflater li = LayoutInflater.from(this);
			LinearLayout ll = (LinearLayout)(li.inflate(R.layout.splashdialog, null));
			TextView v = (TextView)ll.findViewById(R.id.splashtext);
			v.setText("This application requires another camera with long-exposure to shoot the screen of this phone.\nAfter you hit go, slide the phone in a straight line while maintaining the screen in the view of the camera's viewfinder to embed the characters on the picture.");
			mCheckBox = (CheckBox)ll.findViewById(R.id.splashcheck);
			mCheckBox.setChecked(mTempFlag);
			mCheckBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// TODO Auto-generated method stub
					mTempFlag = isChecked;
				}
			});

			mAD = new AlertDialog.Builder(this)
			.setTitle("Aurora Bulb!")
		    .setCancelable(true)
		    .setView(ll)
		    .setOnKeyListener(new OnKeyListener() {
		    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
		    		if (arg2.getKeyCode()==android.view.KeyEvent.KEYCODE_BACK) mAD.cancel();
		    		return true;
		    	};
		    })
		    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AB.PREFS.edit().putBoolean("showIntro", !mTempFlag).commit();
					mAD.dismiss();
				}
			})
		    .show();

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
		try {
			AB.PT1.setTypeface(Typeface.createFromAsset(getApplicationContext().getAssets(), AB.PREFS.getString("pickFont", "Unibody 8-SmallCaps.otf")));
		} catch (RuntimeException e ) {
			//Typeface not found - revert to clockopia
			AB.PREFS.edit().putString("pickFont", "Unibody 8-SmallCaps.otf").commit();
			AB.PT1.setTypeface(Typeface.createFromAsset(getApplicationContext().getAssets(), AB.PREFS.getString("pickFont", "Unibody 8-SmallCaps.otf")));
		}
		if (AB.BMPTODRAW==null) AB.BMPTODRAW = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.transparent),320,170,false);
		AB.updatePreviewBuffer();

		mPreviewImageView.setImageBitmap(AB.PREVIEWBUFFER);
		mPreviewImageView.invalidate();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	if (item.getItemId()==R.id.pickBitmap) {
    		if (AB.PREFS.getString("whatToShow", "text").equals("text")) {
				new AlertDialog.Builder(this)
						.setTitle("Switch to Bitmap")
						.setMessage("Loading a bitmap into AuroraBulb will cause the app to ignore your text settings.\nTo switch back to text mode, tap on Font, Color or Text.")
						.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
						    		AB.PREFS.edit().putString("whatToShow", "bitmap").commit();
						    		Intent intent = new Intent(Intent.ACTION_GET_CONTENT); 
						    		intent.setType("image/*"); 
						    		startActivityForResult(intent, AB.SELECTIMAGE);
						    	}
						})
						.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									// Do nothing.
								}
						})
						.show();
    		} else {
	    		Intent intent = new Intent(Intent.ACTION_GET_CONTENT); 
	    		intent.setType("image/*"); 
	    		startActivityForResult(intent, AB.SELECTIMAGE); 
    		}
    	}
    	if (item.getItemId()==R.id.pickColor) {
    		ColorPickerDialog cpd = new ColorPickerDialog(this, new ColorPickerDialog.OnColorChangedListener() {
				
				@Override
				public void colorUpdate(int color) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void colorChanged(int color) {
					// TODO Auto-generated method stub
		    		AB.PREFS.edit().putString("whatToShow", "text").commit();
					AB.PREFS.edit().putInt("textColor", color).commit();
					AB.updatePreviewBuffer();
					mPreviewImageView.setImageBitmap(AB.PREVIEWBUFFER);
					mPreviewImageView.invalidate();
				}
			}, AB.PREFS.getInt("textColor", Color.GREEN), false);
    		cpd.show();
    	}
		if (item.getItemId()==R.id.pickFont) {
			final CharSequence[] items = {"Comic Font", "Pixel Font", "Script Font", "Symbol Font", "Default Font"};
			final String[] values = {"YESTERDAYSMEAL.ttf", "Unibody 8-SmallCaps.otf", "Forelle.ttf", "EFON.ttf", "Clockopia.ttf"};
			new AlertDialog.Builder(this)
					.setTitle("Pick a Font")
					.setItems(items, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
					    		AB.PREFS.edit().putString("whatToShow", "text").commit();
								AB.PREFS.edit().putString("pickFont",values[item]).commit();							
								AB.PT1.setTypeface(Typeface.createFromAsset(getApplicationContext().getAssets(), values[item]));								
								AB.updatePreviewBuffer();
								mPreviewImageView.setImageBitmap(AB.PREVIEWBUFFER);
								mPreviewImageView.invalidate();
							}
					})
					.show();
		}
		if (item.getItemId()==R.id.pickText) {
			final EditText input = new EditText(this);
			input.setText(AB.PREFS.getString("pickText",""));
			new AlertDialog.Builder(this)
					.setTitle("Enter Text to Show:")
					.setView(input)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
					    		AB.PREFS.edit().putString("whatToShow", "text").commit();
								AB.PREFS.edit().putString("pickText",input.getText().toString()).commit();
								AB.updatePreviewBuffer();
								mPreviewImageView.setImageBitmap(AB.PREVIEWBUFFER);
								mPreviewImageView.invalidate();
							}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Do nothing.
							}
					})
					.show();
		}
		if (item.getItemId()==R.id.camTimerMenu) {
			LayoutInflater li = LayoutInflater.from(this);
			LinearLayout ll = (LinearLayout)(li.inflate(R.layout.camstuff, null));
			camShutter = (EditText)ll.findViewById(R.id.camshutter);
			camShutter.setText(AB.PREFS.getString("timeShutterDuration","10"));
			camTimer = (EditText)ll.findViewById(R.id.camtimer);
			camTimer.setText(AB.PREFS.getString("timePhotoTimer","10"));

			new AlertDialog.Builder(this)
					.setTitle("Camera Settings:")
					.setView(ll)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								try {
									Integer.parseInt(camShutter.getText().toString());
									AB.PREFS.edit().putString("timeShutterDuration",camShutter.getText().toString()).commit();
								} catch (NumberFormatException e) {
									// Invalid value; do nothing									
								}
								try {
									Integer.parseInt(camTimer.getText().toString());
									AB.PREFS.edit().putString("timePhotoTimer",camTimer.getText().toString()).commit();
								} catch (NumberFormatException e) {
									// Invalid value; do nothing									
								}
								AB.updateSrcBuffer();
								mTextCam.setText("Camera: Timer @ " + (AB.PREFS.getString("timePhotoTimer", "10"))
										+ " sec, Shutter @ " + (AB.PREFS.getString("timeShutterDuration", "10"))
										+ " sec.");
								mTextCam.invalidate();
							}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Do nothing.
							}
					})
					.show();
		}
		if (item.getItemId()==R.id.credits) {
			AB.PREFSCREENTOSHOW = "SectionCredits";
			startActivity(new Intent(this, ABPrefActivity.class));
		}
		
		return true;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == AB.RENDERAURORA) {
			// Do nothing
		}
		if (requestCode == AB.SELECTIMAGE) {
			if (resultCode == Activity.RESULT_CANCELED) {
				// Do nothing 
			} else {
				Cursor c = getContentResolver().query(data.getData(), null, null, null, null);
				System.out.println(c.getColumnCount()); 
//				for (String s:c.getColumnNames()) {
//					System.out.println("ColName: "+ s); 
//				}
//				System.out.println(c.getCount()); 
				c.moveToFirst();
				String sImgPath = c.getString(1); //1 is the file path
				c.close();

				AB.BMPTODRAW = BitmapFactory.decodeFile(sImgPath);
				AB.updatePreviewBuffer();
				mPreviewImageView.setImageBitmap(AB.SRCBUFFER);
				mPreviewImageView.invalidate();
			}
		}
	}
	
}