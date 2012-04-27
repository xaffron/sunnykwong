package com.sunnykwong.omc;

import java.io.BufferedInputStream;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class OMCThemeUnzipActivity extends Activity {

	public Handler mHandler;
	static Dialog pdWait;
	static String pdTitleMessage="",pdMessage="";
	static String pdPreview;
	public Uri uri;
	public File omcRoot,outputFile;
	public AlertDialog mAD;
	public boolean NOGO;
	public ProgressBar pg;
	static public boolean COMPLETE;
	public Thread currentThread;
	
	public URL downloadURL;

	final Runnable mResult = new Runnable() {
		public void run() {
			((TextView)pdWait.findViewById(getResources().getIdentifier("UnzipStatus", "id", OMC.PKGNAME))).setText(pdMessage);
			((TextView)pdWait.findViewById(getResources().getIdentifier("UnzipStatus", "id", OMC.PKGNAME))).invalidate();
			if (COMPLETE) {
				Toast.makeText(getApplicationContext(), "Import Complete!", Toast.LENGTH_SHORT).show();
				if (uri.toString().equals(OMC.STARTERPACKURL)) {
					OMC.STARTERPACKDLED = true;
					OMC.PREFS.edit().putBoolean("starterpack", true).commit();
				}
			} else if (!COMPLETE && uri.toString().equals(OMC.EXTENDEDPACK)) {
				startActivity(OMC.GETBACKUPPACKINTENT);
			} else {
				Toast.makeText(getApplicationContext(), "Import Aborted!", Toast.LENGTH_LONG).show();
			}
			if (pdWait.isShowing()) pdWait.dismiss();
			finish();
		}
	};

	final Runnable mUpdateTitle = new Runnable() {
		public void run() {
			pdWait.setTitle(pdTitleMessage);
		}
	};

	final Runnable mUpdateStatus = new Runnable() {
		public void run() {
			((TextView)(pdWait.findViewById(getResources().getIdentifier("UnzipStatus", "id", OMC.PKGNAME)))).setText(pdMessage);
			((TextView)(pdWait.findViewById(getResources().getIdentifier("UnzipStatus", "id", OMC.PKGNAME)))).invalidate();
		}
	};

	final Runnable mUpdateBitmap = new Runnable() {
		public void run() {
			try {
				Bitmap bmp = BitmapFactory.decodeFile(pdPreview);
				((ImageView)pdWait.findViewById(getResources().getIdentifier("UnzipPreview", "id", OMC.PKGNAME))).setImageBitmap(bmp);
				((ImageView)pdWait.findViewById(getResources().getIdentifier("UnzipPreview", "id", OMC.PKGNAME))).invalidate();
			} catch (Exception e) {
				Log.e(OMC.OMCSHORT + "Unzip","Mark Invalidated!");
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		//Hide the title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		COMPLETE = false;
		pdMessage = "";

		uri = getIntent().getData();
		// Check file scheme first... we don't want to support Preview!
		if (uri.getScheme().equals("content")) {
			Toast.makeText(getApplicationContext(), "Please use Download instead of Preview to import an OMC clock!", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		if (!uri.getScheme().equals("asset")) {
			if (!uri.getLastPathSegment().matches(".*.omc")) {
				Toast.makeText(getApplicationContext(), "Clock import works with .omc files.\nWere you opening the wrong file?", Toast.LENGTH_LONG).show();
				finish();
				return;
			}
		}
		
		if (!OMC.checkSDPresent()) {
			finish();
			return;
		}

		omcRoot = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes");
		mHandler = new Handler();
		pdWait = new Dialog(this);
		pdWait.setContentView(getResources().getIdentifier("themeunzippreview", "layout", OMC.PKGNAME));
		pg = (ProgressBar) pdWait.findViewById(getResources().getIdentifier("UnzipProgress", "id", OMC.PKGNAME));
		pg.setVisibility(ProgressBar.VISIBLE);

		pdWait.setTitle("Connecting...");
		pdWait.setCancelable(true);
		pdWait.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				OMCThemeUnzipActivity.this.finish();
				return;
			}
		});
		pdWait.show();

		currentThread = new Thread() {
			public void run() {
				uri = getIntent().getData();
				if (uri == null) {
					Toast.makeText(getApplicationContext(), "Nothing to extract!", Toast.LENGTH_LONG).show();
					finish();
					return;
				} else { 
					try {
						pdMessage = "Opening connection";
						mHandler.post(mUpdateStatus);
						String sScheme = uri.getScheme()+":";
						if (sScheme.equals("")) sScheme = "http:";

						if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Unzip","Scheme is " + sScheme);

						ZipInputStream zis;
						if (sScheme.equals("asset:")) {
							zis = new ZipInputStream(OMC.AM.open(uri.getSchemeSpecificPart()));
						} else if (sScheme.equals("file:")) {
							zis = new ZipInputStream(new FileInputStream(uri.getSchemeSpecificPart()));
						} else {
							downloadURL = new URL(sScheme + uri.getSchemeSpecificPart());
							URLConnection conn = downloadURL.openConnection();
							zis = new ZipInputStream(conn.getInputStream());
							pdMessage = "Streaming " + conn.getContentLength() + " bytes.";
							mHandler.post(mUpdateStatus);
						}
						BufferedInputStream bis = new BufferedInputStream(zis,8192);
						ZipEntry ze;

						while ((ze = zis.getNextEntry())!= null) {
							if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Unzip","Looping - now " + ze.getName());
							outputFile = new File(omcRoot.getAbsolutePath()+"/"+ze.getName());
							if (ze.isDirectory()) {
								pdTitleMessage = ze.getName();
								mHandler.post(mUpdateTitle);
								if (outputFile.exists()) {
									pdMessage = ze.getName() + " exists; overwriting";
									mHandler.post(mUpdateStatus);
								} else if (outputFile.mkdir()==false) {
									//ERROR CREATING DIRECTORY - crap out
									pdMessage = ze.getName() + " exists; overwriting";
									mHandler.post(mUpdateStatus);
									break;
								} else {
									pdMessage = "Theme folder '" + ze.getName() + "'created.";
									mHandler.post(mUpdateStatus);
								}
							} else {
								if (ze.getName().contains("weatherdotcom.type") ||
										ze.getName().contains("accuweather.type")) {
									if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Unzip","Detected WeatherSkin " + ze.getName());
									OMC.removeFile(new File(omcRoot.getAbsolutePath()+"/zz_WeatherSkin/weatherdotcom.type"));
									OMC.removeFile(new File(omcRoot.getAbsolutePath()+"/zz_WeatherSkin/accuweather.type"));
								}
								FileOutputStream fos = new FileOutputStream(outputFile);
								pdMessage = "Storing file " + ze.getName();

								mHandler.post(mUpdateStatus);
								try {
									//Absolute luxury 1980 style!  Using an 8k buffer.
									byte[] buffer = new byte[8192];
									int iBytesRead=0;
									while ((iBytesRead=bis.read(buffer))!= -1){
										fos.write(buffer, 0, iBytesRead);
									}
									fos.flush();
									fos.close();
								} catch (java.io.IOException e) {
									pdTitleMessage = "Download Interrupted!";
									mHandler.post(mUpdateTitle);
									try {Thread.sleep(500);}
									catch (Exception ee) {ee.printStackTrace();}
									pdMessage = "Is your phone in a poor reception area?\nPlease try again later.";
									mHandler.post(mUpdateStatus);
									try {Thread.sleep(3000);}
									catch (Exception ee) {ee.printStackTrace();}
									mHandler.post(mResult);
								} catch (Exception e) {
									e.printStackTrace();
								}
								if (outputFile.getName().equals("000preview.jpg")) {
									pdPreview = outputFile.getAbsolutePath();
									mHandler.post(mUpdateBitmap);
								}
							}
							zis.closeEntry();

						}
						zis.close();
						pdTitleMessage = "Import complete!";
						mHandler.post(mUpdateTitle);
						try {Thread.sleep(3000);}
						catch (Exception ee) {}
						COMPLETE = true;
						mHandler.post(mResult);

					} catch (java.net.SocketException e) {
						pdTitleMessage = "Connection timed out!";
						mHandler.post(mUpdateTitle);
						try {Thread.sleep(500);}
						catch (Exception ee) {ee.printStackTrace();}
						pdMessage = "Is your phone in a poor reception area?\nPlease try again later.";
						mHandler.post(mUpdateStatus);
						try {Thread.sleep(3000);}
						catch (Exception ee) {ee.printStackTrace();}
						mHandler.post(mResult);
					} catch (java.net.UnknownHostException e) {
						pdTitleMessage = "Server not found!";
						mHandler.post(mUpdateTitle);
						try {Thread.sleep(500);}
						catch (Exception ee) {ee.printStackTrace();}
						pdMessage = "Is your phone in a poor reception area?\nPlease try again later.";
						mHandler.post(mUpdateStatus);
						try {Thread.sleep(3000);}
						catch (Exception ee) {ee.printStackTrace();}
						mHandler.post(mResult);
					} catch (java.io.IOException e) {
						e.printStackTrace();
					}
				}	
			}
		};
		currentThread.start();
	}			

	@Override
	protected void onPause() {
		super.onPause();
		if (currentThread !=null && currentThread.isAlive()) currentThread.interrupt();
		if (pdWait.isShowing()) pdWait.dismiss();
		if (!isFinishing())finish();
	}

}