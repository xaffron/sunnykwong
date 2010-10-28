package com.sunnykwong.omc;

import java.io.BufferedReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.lang.StringBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.AdapterView.OnItemSelectedListener;
import android.os.Bundle;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.format.Time;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Environment;
import android.graphics.drawable.BitmapDrawable;

public class OMCThemeImportActivity extends Activity {

	public Handler mHandler;
	public static TextView TEXT;
	public static LocationManager LM;
	public static Location CURRLOCN;
	public static LocationListener LL;
	public static String TRAFFICRESULTS;
	public static HashMap<String,String[]> ELEMENTS;
	public static HashMap<String,File> THEMES;
	public static String tempText = "";
	public static File SDROOT, THEMEROOT;
	public static ArrayAdapter<String> THEMEARRAY;
	public static Spinner THEMESPINNER;
	public static char[] THEMECREDITS;
	public static String CURRSELECTEDTHEME, RAWCONTROLFILE;
	
    static AlertDialog mAD;	

	final Runnable mResult = new Runnable() {
		public void run() {
		// Back from XML importing...
        	Toast.makeText(OMCThemeImportActivity.this, OMC.IMPORTEDTHEME.arrays.get("theme_options").get(0) + " theme applied.", Toast.LENGTH_LONG).show();
        	OMC.PREFS.edit().putString("widgetTheme", "EXTERNAL: " + OMCThemeImportActivity.CURRSELECTEDTHEME)
    		.commit();
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.importlayout);

        mHandler = new Handler();

        OMCThemeImportActivity.CURRSELECTEDTHEME = null;
        
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	Toast.makeText(this, "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
        	finish();
        	return;
        }

        OMCThemeImportActivity.SDROOT = Environment.getExternalStorageDirectory();
        OMCThemeImportActivity.THEMEROOT = new File(OMCThemeImportActivity.SDROOT.getAbsolutePath()+"/OMC");
        if (!OMCThemeImportActivity.THEMEROOT.exists()) {
        	Toast.makeText(this, "OMC folder not found in your SD Card.\nCannot import!", Toast.LENGTH_LONG).show();
        	finish();
        	return;
        }
        
        OMCThemeImportActivity.mAD = new AlertDialog.Builder(this)
		.setTitle("OMC - Theme Import")
		.setMessage("One More Clock! lets you load your own custom-designed theme.  Remember that your theme files should be unzipped and stored on your SD Card for OMC to find them.")
	    .setCancelable(true)
	    .setIcon(R.drawable.fredicon_mdpi)
	    .setPositiveButton("Okay", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
	       		OMCThemeImportActivity.mAD.dismiss();
			}
		})
	    .setOnKeyListener(new OnKeyListener() {
	    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
	       		OMCThemeImportActivity.mAD.dismiss();
	    		return true;
	    	};
	    }).create();

        OMCThemeImportActivity.mAD.show();

        OMCThemeImportActivity.THEMEARRAY =  new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line);
        OMCThemeImportActivity.THEMES =  new HashMap<String, File>();
        for (File f:OMCThemeImportActivity.THEMEROOT.listFiles()) {
        	if (!f.isDirectory()) continue;
        	File ff = new File(f.getAbsolutePath()+"/00control.txt");
        	if (ff.exists()) {
        		OMCThemeImportActivity.THEMEARRAY.add(f.getName());
        		OMCThemeImportActivity.THEMES.put(f.getName(), f);
        	}
        }
        OMCThemeImportActivity.THEMESPINNER = (Spinner)this.findViewById(R.id.spinner);
        OMCThemeImportActivity.THEMESPINNER.setAdapter(OMCThemeImportActivity.THEMEARRAY);
        OMCThemeImportActivity.THEMESPINNER.setOnItemSelectedListener(new OnItemSelectedListener() {
        	@Override
        	public void onItemSelected(AdapterView<?> arg0, View arg1,
        			int position, long id) {
        		// TODO Auto-generated method stub
        		setThemePreview(OMCThemeImportActivity.THEMEARRAY.getItem(position));
        		
        	}
        	@Override
        	public void onNothingSelected(AdapterView<?> arg0) {
        		// do nothing
       		
        	}
		});
        OMCThemeImportActivity.THEMESPINNER.invalidate();

        ((Button)this.findViewById(R.id.buttonCancel)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				OMCThemeImportActivity.this.finish();
			}
		});
        
        ((Button)this.findViewById(R.id.buttonOK)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (OMCThemeImportActivity.CURRSELECTEDTHEME!=null) importTheme();
			}
		});

//	public void processXMLResults() {
//
//		Comparator<HashMap<String,String>> c = new Comparator<HashMap<String,String>>() {
//    		Location tmpLocn1 = new Location("");
//    		Location tmpLocn2 = new Location("");
//    		@Override
//
//    		public int compare(HashMap<String,String> hm1, HashMap<String,String> hm2) {
//    			tmpLocn1.setLatitude(Double.parseDouble(hm1.get("Latitude")));
//    			tmpLocn1.setLongitude(Double.parseDouble(hm1.get("Longitude")));
//    			tmpLocn2.setLatitude(Double.parseDouble(hm2.get("Latitude")));
//    			tmpLocn2.setLongitude(Double.parseDouble(hm1.get("Longitude")));
//
////    			//Simple logic - if there's an accident, always report first
////    			if (hm1.get("Title").contains("cident") || hm1.get("Title").contains("isabled vehicle")) {
////    				return -1;
////    			} else if (hm2.get("Title").contains("cident") || hm2.get("Title").contains("isabled vehicle")){
////    				return 1;
////    			}
////    			//Otherwise, report by severity and distance to phone
////    			else {
////    				return Math.round(Integer.parseInt(hm1.get("Severity"))*-100000 + TMActivity.CURRLOCN.distanceTo(tmpLocn1)
////    					- (Integer.parseInt(hm2.get("Severity"))*-100000 + TMActivity.CURRLOCN.distanceTo(tmpLocn2)));
////    			}
//
//    			// Simplest logic - only sort by geographical distance
//    			return Math.round(Integer.parseInt(hm1.get("Severity"))*-100000 + TMActivity.CURRLOCN.distanceTo(tmpLocn1)
//    					- (Integer.parseInt(hm2.get("Severity"))*-100000 + TMActivity.CURRLOCN.distanceTo(tmpLocn2)));
//    		}
//    	};
//    	
//    	Collections.sort(TMActivity.ELEMENTS,c);
// 
//    	// Initialize TTS Engine, pass 
//    	Intent checkIntent = new Intent();
//    	checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
//    	startActivityForResult(checkIntent, 0);
//	}
//
//	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//	    if (requestCode == 0) {
//	        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
//
//	            // Create the TTS instance; once TTS inits, control returns to the onInit method below
//	            if (TMActivity.TTSENGINE==null) {
//	            	TMActivity.TTSENGINE = new TextToSpeech(this, this);
//	            }
//	            
//	        } else {
//	            // missing data, install it
//
//	        	Intent installIntent = new Intent();
//	            installIntent.setAction(
//	                TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
//	            startActivity(installIntent);
//	        }
//	        
//	    }
//	}
//	
//    @Override
//    public void onInit(int status) {       
//    	// OK, TTS Engine initialized successfully.
//		// Set to a male voice (TODO:  customizable)
//    	TMActivity.TTSENGINE.setLanguage(Locale.US);
//		TMActivity.TTSENGINE.setSpeechRate((float)1.1);
//		TMActivity.TTSENGINE.setPitch((float)0.95);
//
//		
//		// Start yammering!
//    	if (status == TextToSpeech.SUCCESS) {
//        	Thread t = new Thread () {
//        		public void run() {
//                	try {
//                        HashMap<String, String> hashText = new HashMap<String, String>();
//                        Iterator<HashMap<String,String>> i = TMActivity.ELEMENTS.iterator();
//                    	while (i.hasNext()) {
//                    		HashMap<String,String> element = i.next();
//
//                    		//If data is stale, skip to the next element
//                    		Time t = new Time();
//                    		t.setToNow();
//                    		if (t.toMillis(false)/60000 - Long.parseLong(element.get("UpdateDate"))/60 > TMActivity.STALEMINUTES) continue; 
//                    		
//                    		String s = element.get("Spoken");
//                    		hashText.clear();
//                            hashText.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,s);
//                            TMActivity.tempText= element.get("Title") + "\n\n" + element.get("Description");
//                            TMActivity.RUNSTAGE = RS.TEXT;
//                            mHandler.post(mTrafficResult);
//                            if (DEBUG) {
//                            	Log.i("TMouth",s);
//                            } else {
//                            	TMActivity.TTSENGINE.speak(s, TextToSpeech.QUEUE_ADD, hashText);
//                            	while (TMActivity.TTSENGINE.isSpeaking()) Thread.sleep(2000);
//                            }
//                    	}
//
//                	} catch (Exception e) {
//                		
//                    	e.printStackTrace();
//                	}
//
//                	// This call will end up passing control to onPause
//                    TMActivity.RUNSTAGE = RS.DONE;
//        			mHandler.post(mTrafficResult);
//        		}
//          	   
//        	};
//    		t.start();
//
//	    } else if (status == TextToSpeech.ERROR) {
//	    	Log.e("TMouth","Error initializing TTS Engine.");
//	    }
//	}
//
//    @Override
//    public void onDestroy() {
//    	if (TMActivity.TTSENGINE!=null) TMActivity.TTSENGINE.shutdown();
//    	super.onDestroy();
    }
	public void setThemePreview(String sThemeName) {
		OMCThemeImportActivity.CURRSELECTEDTHEME = sThemeName;
		if (sThemeName == null || sThemeName.equals("")) return;
		File root = OMCThemeImportActivity.THEMES.get(sThemeName);
		System.out.println(root.getAbsolutePath() + "/preview.png");
		Bitmap bmpPreview = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(root.getAbsolutePath() + "/preview.png"),320,200,false);
		((ImageView)this.findViewById(R.id.ImagePreview)).setImageBitmap(bmpPreview);
		OMCThemeImportActivity.THEMECREDITS = new char[3000];
		try {
			FileReader fr = new FileReader(root.getAbsolutePath() + "/00credits.txt");
			fr.read(OMCThemeImportActivity.THEMECREDITS);
			((TextView)this.findViewById(R.id.TextPreview)).setText(String.valueOf(OMCThemeImportActivity.THEMECREDITS).trim());
			this.findViewById(R.id.toplevel).invalidate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
 
	public void importTheme() {
		if (OMCThemeImportActivity.CURRSELECTEDTHEME == null) {
        	Toast.makeText(this, "Please select a theme first.", Toast.LENGTH_SHORT).show();
			return;
		}
		
        System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver"); 

		
		
    	Thread t = new Thread () {
    		public void run() {
            	try {
            		File root = OMCThemeImportActivity.THEMES.get(OMCThemeImportActivity.CURRSELECTEDTHEME);
            		XMLReader xr = XMLReaderFactory.createXMLReader();
            		OMC.IMPORTEDTHEME = new OMCImportedTheme();
            		xr.setContentHandler(OMC.IMPORTEDTHEME);
            		FileReader fr = new FileReader(root.getAbsolutePath() + "/00control.txt");
            		xr.setErrorHandler(OMC.IMPORTEDTHEME);
            		xr.parse(new InputSource(fr));
            	} catch (Exception e) {
            		
                	e.printStackTrace();
            	}

            	// This call will end up passing control to processXMLResults
    			mHandler.post(mResult);
    		}
      	   
    	};
		t.start();

    } 

	
}