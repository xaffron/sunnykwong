package com.sunnykwong.omc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.widget.ProgressBar;
import android.view.ViewGroup.LayoutParams;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

public class OMCThemeUnzipActivity extends Activity {

	public Handler mHandler;
	static Dialog pdWait;
	static String pdMessage="";
	static String pdPreview;
	public Uri uri;
	public File sdRoot,outputFile;
	public AlertDialog mAD;
	public boolean NOGO;
	
	public URL downloadURL;
	
	final Runnable mResult = new Runnable() {
		public void run() {
			((TextView)pdWait.findViewById(R.id.UnzipStatus)).setText(pdMessage);
			((TextView)pdWait.findViewById(R.id.UnzipStatus)).invalidate();
			Toast.makeText(getApplicationContext(), "Import Complete!", Toast.LENGTH_SHORT).show();
			pdWait.dismiss();
			if (uri.toString().equals(OMC.STARTERPACKURL)) {
				OMC.STARTERPACKDLED = true;
				OMC.PREFS.edit().putBoolean("starterpack", true).commit();
			}
			finish();
		}
	};

	final Runnable mUpdateTitle = new Runnable() {
		public void run() {
			pdWait.setTitle(pdMessage);
		}
	};

	final Runnable mUpdateStatus = new Runnable() {
		public void run() {
			((TextView)(pdWait.findViewById(R.id.UnzipStatus))).setText(pdMessage);
			((TextView)(pdWait.findViewById(R.id.UnzipStatus))).invalidate();
		}
	};

	final Runnable mUpdateBitmap = new Runnable() {
		public void run() {
			((ImageView)pdWait.findViewById(R.id.UnzipPreview)).setImageBitmap(BitmapFactory.decodeFile(pdPreview));
			((ImageView)pdWait.findViewById(R.id.UnzipPreview)).invalidate();
		}
	};

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        //Hide the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        NOGO = OMC.FREEEDITION;
        
        uri = getIntent().getData();
        if (NOGO && uri.toString().equals(OMC.STARTERPACKURL)) {
        	NOGO = false;
        } 
        if (NOGO) {
        	mAD = new AlertDialog.Builder(this)
        						.setTitle("Why doesn't this work?")
        						.setCancelable(true)
        						.setMessage("Actually... it does.  Really well.  However, direct theme download requires the paid edition of OMC.  To install themes on the free edition, just download the theme offline to your computer, then unzip and copy to your SD card manually.\n\nAt the end of the day, do you like OMC?  If so, please consider donating!")
        						.setPositiveButton("Take me to the paid version!", new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										// TODO Auto-generated method stub
										OMCThemeUnzipActivity.this.mAD.dismiss();
										OMCThemeUnzipActivity.this.startActivity(OMC.OMCMARKETINTENT);
							        	OMCThemeUnzipActivity.this.finish();
										
									}
								}).create();
        	mAD.show();

        } else {

	        checkSetup();
	        
	        mHandler = new Handler();
	        pdWait = new Dialog(this);
	        pdWait.setContentView(R.layout.themeunzippreview);
	        ProgressBar pg = (ProgressBar) pdWait.findViewById(R.id.UnzipProgress);
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
	
	        Thread t = new Thread() {
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
	        				String sScheme = "http:";
	        				if (uri.getScheme().equals("omcs")) sScheme = "https:";
	        				else if (uri.getScheme().equals("omc")) sScheme = "http:";
	        				if (OMC.DEBUG) Log.i("OMCUnzip","Scheme is " + sScheme);
	        				downloadURL = new URL(sScheme + uri.getSchemeSpecificPart());
	        				if (OMC.DEBUG) Log.i("OMCUnzip","The rest is " + uri.getSchemeSpecificPart());
	        				URLConnection conn = downloadURL.openConnection();
	        				ZipInputStream zis = new ZipInputStream(conn.getInputStream());
	        				BufferedInputStream bis = new BufferedInputStream(zis,8192);
	        				ZipEntry ze;
	
	        				pdMessage = "Streaming " + conn.getContentLength() + " bytes.";
	        				mHandler.post(mUpdateStatus);
	        				while ((ze = zis.getNextEntry())!= null) {
	            				if (OMC.DEBUG) Log.i("OMCUnzip","Looping - now " + ze.getName());
	        					outputFile = new File(sdRoot.getAbsolutePath()+"/"+ze.getName());
	        					if (ze.isDirectory()) {
	                				pdMessage = "Importing: " + ze.getName();
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
							pdMessage = "Import complete!";
	        				mHandler.post(mResult);
	        				
	        			} catch (Exception e) {
	        				e.printStackTrace();
	        			}
	        		}	
	        	}
	        };
	        t.start();
        }			
    }

    public void checkSetup() {
    	
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	Toast.makeText(getApplicationContext(), "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
			setResult(Activity.RESULT_OK);
			finish();
        	return;
        }

        sdRoot = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OMC");
        if (!sdRoot.exists()) {
        	Toast.makeText(this, "OMC folder not found in your SD Card.\nCreating folder...", Toast.LENGTH_LONG).show();
        	sdRoot.mkdir();
        }
    }
}
