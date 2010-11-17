package com.sunnykwong.omc;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class OMCThemeImportActivity extends Activity {

	public Handler mHandler;
	public static TextView TEXT;
	public static LocationManager LM;
	public static Location CURRLOCN;
	public static LocationListener LL;
	public static String TRAFFICRESULTS;
	//public static HashMap<String,String[]> ELEMENTS;
	//public static HashMap<String,File> THEMES;
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
//			if (OMCXMLThemeParser.valid) {
//	        	Toast.makeText(OMCThemeImportActivity.this, OMC.IMPORTEDTHEMEMAP.get(OMCXMLThemeParser.latestThemeName).arrays.get("theme_options").get(0) + " theme imported.", Toast.LENGTH_SHORT).show();
//	        	OMC.PREFS.edit()
//			        	.putString("widgetTheme", OMCXMLThemeParser.latestThemeName)
//			        	.putBoolean("external", true)
//			    		.commit();
//	        	OMC.saveImportedThemeToCache(OMCThemeImportActivity.this,OMCXMLThemeParser.latestThemeName);
//	        	Toast.makeText(OMCThemeImportActivity.this, OMC.IMPORTEDTHEMEMAP.get(OMCXMLThemeParser.latestThemeName).arrays.get("theme_options").get(0) + " theme cached and applied.", Toast.LENGTH_SHORT).show();
//			} else {
//	        	Toast.makeText(OMCThemeImportActivity.this, OMCThemeImportActivity.CURRSELECTEDTHEME + " theme did not pass validity checks!\nPlease check with the author of your theme.\nImport cancelled.", Toast.LENGTH_SHORT).show();
//			}
//
//			setResult(Activity.RESULT_OK);
//        	finish();
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        OMCThemeImportActivity.CURRSELECTEDTHEME = null;
        
        OMCThemeImportActivity.mAD = new AlertDialog.Builder(this)
		.setTitle("OMC - Pick a Theme")
		.setMessage("Go ahead - Pick your own theme.  Why stick with one all the time?  If you want more, just check out the \"More Clocks\" button at the bottom for more OMC goodness!")
	    .setCancelable(true)
	    .setIcon(R.drawable.fredicon_mdpi)
	    .setPositiveButton("Okay", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
	       		OMCThemeImportActivity.mAD.dismiss();
	       		chooseTheme();
			}
		})
	    .setOnKeyListener(new OnKeyListener() {
	    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
	       		OMCThemeImportActivity.mAD.dismiss();
	       		chooseTheme();
	    		return true;
	    	};
	    }).create();

        OMCThemeImportActivity.mAD.show();

    }

    public void chooseTheme() {
        Intent itThemePicker = new Intent(this,OMCThemePickerActivity.class);
        itThemePicker.putExtra("externalonly", false);

        startActivityForResult(itThemePicker, 0);
    }
    
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	if (resultCode == Activity.RESULT_CANCELED) {
    		finish();
    		return;
    	}
    	
    	OMCThemeImportActivity.CURRSELECTEDTHEME = data.getExtras().getString("theme");

    	if (!data.getBooleanExtra("external", false)) {
        	OMC.PREFS.edit()
        	.putString("widgetTheme", OMCThemeImportActivity.CURRSELECTEDTHEME)
        	.putBoolean("external", false)
    		.commit();
        	Toast.makeText(OMCThemeImportActivity.this, "Lockscreen Look theme applied.", Toast.LENGTH_SHORT).show();
    		finish();
    		return;
    	}

    	if (OMCThemeImportActivity.CURRSELECTEDTHEME!=null) {
			if (!OMC.IMPORTEDTHEMEMAP.containsKey(OMCThemeImportActivity.CURRSELECTEDTHEME)) {
				OMCXMLThemeParser parser = new OMCXMLThemeParser(Environment.getExternalStorageDirectory().getAbsolutePath()
						+"/OMC/" + OMCThemeImportActivity.CURRSELECTEDTHEME);
				parser.importTheme();

				while (!parser.doneParsing){
					try {
						Thread.sleep(500);
					} catch (Exception e) {
						// Do nothing
					}
				}
				
				if (parser.valid) {
		        	Toast.makeText(this, OMCThemeImportActivity.CURRSELECTEDTHEME + " theme imported.", Toast.LENGTH_SHORT).show();
		        	OMC.PREFS.edit()
				        	.putString("widgetTheme", OMCThemeImportActivity.CURRSELECTEDTHEME)
				        	.putBoolean("external", true)
				    		.commit();
		        	OMC.saveImportedThemeToCache(OMCThemeImportActivity.this,OMCThemeImportActivity.CURRSELECTEDTHEME);
		        	Toast.makeText(OMCThemeImportActivity.this, OMC.IMPORTEDTHEMEMAP.get(OMCThemeImportActivity.CURRSELECTEDTHEME).arrays.get("theme_options").get(0) + " theme cached and applied.", Toast.LENGTH_SHORT).show();
				} else {
		        	Toast.makeText(OMCThemeImportActivity.this, OMCThemeImportActivity.CURRSELECTEDTHEME + " theme did not pass validity checks!\nPlease check with the author of your theme.\nImport cancelled.", Toast.LENGTH_SHORT).show();
				}

				setResult(Activity.RESULT_OK);
	        	finish();
			} else {
				
				Toast.makeText(OMCThemeImportActivity.this, 
						"Theme already imported and cached.\n" + 
						OMC.IMPORTEDTHEMEMAP.get(
								OMCThemeImportActivity.CURRSELECTEDTHEME).arrays
								.get("theme_options").get(0) + " theme applied."
								, Toast.LENGTH_SHORT).show();
				
	        	OMC.PREFS.edit()
	        	.putString("widgetTheme", OMCThemeImportActivity.CURRSELECTEDTHEME)
	        	.putBoolean("external", true)
	    		.commit();

	        	setResult(Activity.RESULT_OK);
				finish();
			}

		} else {
			//For some reason, no theme selected
			finish();
			return;
		}
	}

//	public void importTheme() {
//		if (OMCThemeImportActivity.CURRSELECTEDTHEME == null) {
//        	Toast.makeText(this, "Please select a theme first.", Toast.LENGTH_SHORT).show();
//			return;
//		}
//		
//        System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver"); 
//
//		
//		
//    	Thread t = new Thread () {
//    		public void run() {
//            	try {
//            		// Set SD OMC Root
//            		File root = OMCThemeImportActivity.THEMES.get(OMCThemeImportActivity.CURRSELECTEDTHEME);
//            		// Setup XML Parsing...
//            		XMLReader xr = XMLReaderFactory.createXMLReader();
//            		OMCXMLThemeParser parser = new OMCXMLThemeParser(root.getAbsolutePath());
//            		xr.setContentHandler(parser);
//            		// Feed data from control file to XML Parser.
//            		// XML Parser will populate OMC.IMPORTEDTHEME.
//            		FileReader fr = new FileReader(root.getAbsolutePath() + "/00control.xml");
//            		xr.setErrorHandler(parser);
//            		xr.parse(new InputSource(fr));
//            		// When we're done, remove all references to parser.
//                	parser = null;
//                	fr.close();
//
//            	} catch (Exception e) {
//            		
//                	e.printStackTrace();
//            	}
//
//            	// This call will end up passing control to processXMLResults
//    			mHandler.post(mResult);
//    		}
//      	   
//    	};
//		t.start();
//
//    } 

	
}