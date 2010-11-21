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

public class OMCThemeUnzipActivity extends Activity {

	public Handler mHandler;
	static Dialog pdWait;
	static String pdMessage="";
	static String pdPreview;
	public Uri uri;
	public File sdRoot,outputFile;
	
	public URL downloadURL;
	
	final Runnable mResult = new Runnable() {
		public void run() {
			((TextView)pdWait.findViewById(R.id.UnzipStatus)).setText(pdMessage);
			((TextView)pdWait.findViewById(R.id.UnzipStatus)).invalidate();
			Toast.makeText(getApplicationContext(), "Import Complete!", Toast.LENGTH_SHORT).show();
			pdWait.dismiss();
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

        checkSetup();
        
        mHandler = new Handler();
        pdWait = new Dialog(this);
        pdWait.setContentView(R.layout.themeunzippreview);
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
        				String sScheme = "http";
        				if (uri.getScheme().equals("omcs")) sScheme = "https";
        				else if (uri.getScheme().equals("omc")) sScheme = "http";
        				downloadURL = new URL(sScheme + uri.getSchemeSpecificPart());
        				URLConnection conn = downloadURL.openConnection();
        				ZipInputStream zis = new ZipInputStream(conn.getInputStream());
        				BufferedInputStream bis = new BufferedInputStream(zis);
        				ZipEntry ze;

        				pdMessage = "Streaming data";
        				mHandler.post(mUpdateStatus);
        				while ((ze = zis.getNextEntry())!= null) {
        					outputFile = new File(sdRoot.getAbsolutePath()+"/"+ze.getName());
        					if (ze.isDirectory()) {
                				pdMessage = "Importing: " + ze.getName();
                				mHandler.post(mUpdateTitle);
        						if (outputFile.exists()) {
        							//System.out.println(ze.getName() + " Theme already exists!");
        							pdMessage = "Theme already exists! Cancelling...";
        							mHandler.post(mResult);
        						} else {
        							if (outputFile.mkdir()==false) {
        								//ERROR CREATING DIRECTORY - crap out
            							pdMessage = "Error creating directory! Does theme already exist?";
        								mHandler.post(mResult);
        								break;
        							} else {
                        				pdMessage = "Theme folder '" + ze.getName() + "'created.";
                        				mHandler.post(mUpdateStatus);
        							}
        						}
        					} else {
        						FileOutputStream fos = new FileOutputStream(outputFile);
                				pdMessage = "Storing file " + ze.getName();
                				mHandler.post(mUpdateStatus);
        						try {
        							//Absolute luxury 1980 style!  Using a 512byte buffer.
        						    byte[] buffer = new byte[512];
        						    while (bis.read(buffer)!= -1){
        						    	fos.write(buffer);
        						    }
        						    fos.flush();
        						    fos.close();
        						} catch (Exception e) {
        							e.printStackTrace();
        						}
        						if (outputFile.getName().equals("preview.jpg")) {
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
